import os
import re
import xml.etree.ElementTree as ET
import requests
from config import Config


GROBID_URL = Config.GROBID_URL
TEI_NS = {'tei': 'http://www.tei-c.org/ns/1.0'}


class GrobidClient:
    def __init__(self, base_url=GROBID_URL, timeout=Config.GROBID_TIMEOUT):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout

    def _call_grobid(self, endpoint, pdf_path, params=None):
        url = f"{self.base_url}{endpoint}"
        try:
            with open(pdf_path, 'rb') as f:
                files = {'input': (os.path.basename(pdf_path), f, 'application/pdf')}
                data = params or {}
                response = requests.post(
                    url,
                    files=files,
                    data=data,
                    timeout=self.timeout
                )
            response.raise_for_status()
            return response.text
        except (requests.exceptions.RequestException, requests.exceptions.Timeout) as e:
            raise ConnectionError(f"GROBID connection failed: {str(e)}")

    def _parse_authors(self, root):
        authors = []
        file_desc = root.find('.//tei:fileDesc', TEI_NS)
        if file_desc is None:
            return authors
        
        source_desc = file_desc.find('tei:sourceDesc', TEI_NS)
        if source_desc is None:
            return authors
        
        bibl = source_desc.find('.//tei:biblStruct', TEI_NS)
        if bibl is None:
            analytic = source_desc.find('.//tei:analytic', TEI_NS)
            if analytic is None:
                return authors
        else:
            analytic = bibl.find('tei:analytic', TEI_NS)
        
        if analytic is None:
            return authors
        
        for author_elem in analytic.findall('.//tei:author', TEI_NS):
            author = {}
            
            pers_name = author_elem.find('tei:persName', TEI_NS)
            if pers_name is not None:
                forename_parts = []
                forename = pers_name.find('tei:forename', TEI_NS)
                if forename is not None and forename.text:
                    forename_parts.append(forename.text)
                middle = pers_name.find('tei:middle', TEI_NS)
                if middle is not None and middle.text:
                    forename_parts.append(middle.text)
                surname = pers_name.find('tei:surname', TEI_NS)
                surname_text = surname.text if surname is not None and surname.text else ''
                
                full_name_parts = forename_parts + [surname_text]
                author['name'] = ' '.join(p for p in full_name_parts if p)
            else:
                author['name'] = ''
            
            affiliations = []
            for aff in author_elem.findall('tei:affiliation', TEI_NS):
                org_names = []
                for org_name in aff.findall('tei:orgName', TEI_NS):
                    if org_name.text:
                        org_names.append(org_name.text)
                addr_lines = []
                address = aff.find('tei:address', TEI_NS)
                if address is not None:
                    for addr_line in address.findall('tei:addrLine', TEI_NS):
                        if addr_line.text:
                            addr_lines.append(addr_line.text)
                    settlement = address.find('tei:settlement', TEI_NS)
                    if settlement is not None and settlement.text:
                        addr_lines.append(settlement.text)
                    country = address.find('tei:country', TEI_NS)
                    if country is not None and country.text:
                        addr_lines.append(country.text)
                aff_str = ', '.join(org_names + addr_lines)
                if aff_str:
                    affiliations.append(aff_str)
            author['affiliation'] = affiliations[0] if affiliations else ''
            
            emails = []
            for email in author_elem.findall('tei:email', TEI_NS):
                if email.text:
                    emails.append(email.text)
            author['email'] = emails[0] if emails else ''
            
            authors.append(author)
        
        return authors

    def _parse_title(self, root):
        title_stmt = root.find('.//tei:titleStmt', TEI_NS)
        if title_stmt is not None:
            title_elem = title_stmt.find('tei:title', TEI_NS)
            if title_elem is not None and title_elem.text:
                return title_elem.text.strip()
        return ''

    def _parse_abstract(self, root):
        profile_desc = root.find('.//tei:profileDesc', TEI_NS)
        if profile_desc is not None:
            abstract = profile_desc.find('tei:abstract', TEI_NS)
            if abstract is not None:
                texts = []
                for div in abstract.findall('.//tei:div', TEI_NS):
                    head = div.find('tei:head', TEI_NS)
                    if head is not None and head.text:
                        texts.append(head.text.strip())
                    for p in div.findall('tei:p', TEI_NS):
                        if p.text:
                            texts.append(p.text.strip())
                for p in abstract.findall('tei:p', TEI_NS):
                    if p.text:
                        texts.append(p.text.strip())
                return '\n'.join(texts).strip()
        return ''

    def _parse_keywords(self, root):
        keywords_list = []
        profile_desc = root.find('.//tei:profileDesc', TEI_NS)
        if profile_desc is not None:
            text_class = profile_desc.find('tei:textClass', TEI_NS)
            if text_class is not None:
                for keywords in text_class.findall('tei:keywords', TEI_NS):
                    for term in keywords.findall('tei:term', TEI_NS):
                        if term.text and term.text.strip():
                            keywords_list.append(term.text.strip())
        return keywords_list

    def _parse_citations(self, root):
        citations = []
        back = root.find('.//tei:back', TEI_NS)
        if back is None:
            return citations
        
        list_bibl = back.find('.//tei:listBibl', TEI_NS)
        if list_bibl is None:
            return citations
        
        for idx, bibl in enumerate(list_bibl.findall('tei:biblStruct', TEI_NS)):
            citation = {'id': idx + 1, 'title': '', 'authors': [], 'year': '', 'journal': ''}
            
            analytic = bibl.find('tei:analytic', TEI_NS)
            if analytic is not None:
                title_elem = analytic.find('tei:title', TEI_NS)
                if title_elem is not None and title_elem.text:
                    citation['title'] = title_elem.text.strip()
                
                for author in analytic.findall('tei:author', TEI_NS):
                    author_name = ''
                    pers_name = author.find('tei:persName', TEI_NS)
                    if pers_name is not None:
                        parts = []
                        forename = pers_name.find('tei:forename', TEI_NS)
                        if forename is not None and forename.text:
                            parts.append(forename.text)
                        middle = pers_name.find('tei:middle', TEI_NS)
                        if middle is not None and middle.text:
                            parts.append(middle.text)
                        surname = pers_name.find('tei:surname', TEI_NS)
                        if surname is not None and surname.text:
                            parts.append(surname.text)
                        author_name = ' '.join(p for p in parts if p)
                    if author_name:
                        citation['authors'].append(author_name)
            
            monogr = bibl.find('tei:monogr', TEI_NS)
            if monogr is not None:
                title_elem = monogr.find('tei:title', TEI_NS)
                if title_elem is not None and title_elem.text:
                    citation['journal'] = title_elem.text.strip()
                
                imprint = monogr.find('tei:imprint', TEI_NS)
                if imprint is not None:
                    date = imprint.find('tei:date', TEI_NS)
                    if date is not None:
                        when = date.get('when', '')
                        if when:
                            match = re.search(r'\d{4}', when)
                            if match:
                                citation['year'] = match.group()
                        elif date.text:
                            match = re.search(r'\d{4}', date.text)
                            if match:
                                citation['year'] = match.group()
            
            citations.append(citation)
        
        return citations

    def _parse_body(self, root):
        body_sections = []
        text = root.find('.//tei:text', TEI_NS)
        if text is None:
            return body_sections
        
        body = text.find('tei:body', TEI_NS)
        if body is None:
            return body_sections
        
        def parse_div(div, level=1):
            sections = []
            head = div.find('tei:head', TEI_NS)
            heading = head.text.strip() if head is not None and head.text else ''
            
            content_parts = []
            for child in list(div):
                if child.tag == f'{{{TEI_NS["tei"]}}}p':
                    if child.text:
                        content_parts.append(child.text.strip())
                    for sub in child:
                        if sub.tail:
                            content_parts[-1] += sub.tail.strip() if content_parts else sub.tail.strip()
                elif child.tag == f'{{{TEI_NS["tei"]}}}div':
                    sections.extend(parse_div(child, level + 1))
            
            content = '\n\n'.join(p for p in content_parts if p)
            
            if heading or content:
                sections.append({
                    'heading': heading,
                    'content': content,
                    'level': level
                })
            
            return sections
        
        for div in body.findall('tei:div', TEI_NS):
            body_sections.extend(parse_div(div))
        
        return body_sections

    def _parse_figures(self, root):
        figures = []
        text = root.find('.//tei:text', TEI_NS)
        if text is None:
            return figures
        
        body = text.find('tei:body', TEI_NS)
        if body is None:
            return figures
        
        idx = 1
        for figure in body.findall('.//tei:figure', TEI_NS):
            fig = {
                'id': idx,
                'caption': '',
                'label': '',
                'coords': {}
            }
            
            label = figure.find('tei:label', TEI_NS)
            if label is not None and label.text:
                fig['label'] = label.text.strip()
            
            desc = figure.find('tei:figDesc', TEI_NS)
            if desc is not None and desc.text:
                fig['caption'] = desc.text.strip()
            
            graphic = figure.find('tei:graphic', TEI_NS)
            if graphic is not None:
                coords_str = graphic.get('coords', '')
                if coords_str:
                    parts = coords_str.split(',')
                    if len(parts) == 4:
                        try:
                            fig['coords'] = {
                                'page': int(parts[0]),
                                'x': float(parts[1]),
                                'y': float(parts[2]),
                                'w': float(parts[3]),
                                'h': float(parts[4]) if len(parts) > 4 else 0
                            }
                        except (ValueError, IndexError):
                            pass
            
            figures.append(fig)
            idx += 1
        
        return figures

    def _parse_tables(self, root):
        tables = []
        text = root.find('.//tei:text', TEI_NS)
        if text is None:
            return tables
        
        body = text.find('tei:body', TEI_NS)
        if body is None:
            return tables
        
        idx = 1
        for table in body.findall('.//tei:table', TEI_NS):
            tbl = {
                'id': idx,
                'caption': '',
                'content': ''
            }
            
            head = table.find('tei:head', TEI_NS)
            if head is not None and head.text:
                tbl['caption'] = head.text.strip()
            
            rows = []
            for row in table.findall('.//tei:row', TEI_NS):
                cells = []
                for cell in row.findall('tei:cell', TEI_NS):
                    cell_text = cell.text.strip() if cell.text else ''
                    cells.append(cell_text)
                rows.append(cells)
            tbl['content'] = rows
            
            tables.append(tbl)
            idx += 1
        
        return tables

    def process_pdf(self, pdf_path):
        endpoint = '/api/processFulltextDocument'
        params = {
            'consolidateHeader': '1',
            'consolidateCitations': '1',
            'includeRawCitations': '1',
            'segmentSentences': '0'
        }
        
        try:
            xml_content = self._call_grobid(endpoint, pdf_path, params)
            root = ET.fromstring(xml_content)
            
            result = {
                'title': self._parse_title(root),
                'abstract': self._parse_abstract(root),
                'authors': self._parse_authors(root),
                'keywords': self._parse_keywords(root),
                'citations': self._parse_citations(root),
                'body': self._parse_body(root),
                'figures': self._parse_figures(root),
                'tables': self._parse_tables(root)
            }
            return result
        except (ConnectionError, ET.ParseError) as e:
            raise Exception(f"GROBID processing failed: {str(e)}")

    def process_header(self, pdf_path):
        endpoint = '/api/processHeaderDocument'
        params = {
            'consolidateHeader': '1'
        }
        
        try:
            xml_content = self._call_grobid(endpoint, pdf_path, params)
            root = ET.fromstring(xml_content)
            
            result = {
                'title': self._parse_title(root),
                'abstract': self._parse_abstract(root),
                'authors': self._parse_authors(root),
                'keywords': self._parse_keywords(root)
            }
            return result
        except (ConnectionError, ET.ParseError) as e:
            raise Exception(f"GROBID header processing failed: {str(e)}")

    def process_citations(self, pdf_path):
        endpoint = '/api/processReferences'
        params = {
            'consolidateCitations': '1',
            'includeRawCitations': '1'
        }
        
        try:
            xml_content = self._call_grobid(endpoint, pdf_path, params)
            root = ET.fromstring(xml_content)
            
            result = {
                'citations': self._parse_citations(root)
            }
            return result
        except (ConnectionError, ET.ParseError) as e:
            raise Exception(f"GROBID citations processing failed: {str(e)}")
