import os
import re
import uuid
import pdfplumber
from PIL import Image
import io


class PdfPlumberExtractor:
    def __init__(self):
        self.formula_patterns = [
            re.compile(r'\$\$(.*?)\$\$', re.DOTALL),
            re.compile(r'\$(.*?)\$'),
            re.compile(r'\\\[(.*?)\\\]', re.DOTALL),
            re.compile(r'\\\((.*?)\\\)'),
            re.compile(r'\\begin\{equation\}(.*?)\\end\{equation\}', re.DOTALL),
            re.compile(r'\\begin\{align\}(.*?)\\end\{align\}', re.DOTALL),
            re.compile(r'\\begin\{equation\*\}(.*?)\\end\{equation\*\}', re.DOTALL),
        ]
        self.formula_keywords = [
            r'\\frac', r'\\sqrt', r'\\sum', r'\\int', r'\\prod',
            r'\\alpha', r'\\beta', r'\\gamma', r'\\delta', r'\\theta',
            r'\\lambda', r'\\mu', r'\\pi', r'\\sigma', r'\\omega',
            r'\\infty', r'\\partial', r'\\nabla', r'\\leq', r'\\geq',
            r'\\neq', r'\\approx', r'\\pm', r'\\times', r'\\cdot'
        ]

    def extract_images(self, pdf_path, output_dir):
        images = []
        images_dir = os.path.join(output_dir, 'images')
        os.makedirs(images_dir, exist_ok=True)
        
        try:
            with pdfplumber.open(pdf_path) as pdf:
                img_idx = 1
                for page_num, page in enumerate(pdf.pages, start=1):
                    if page.images:
                        for img_info in page.images:
                            try:
                                x0, y0, x1, y1 = img_info.get('x0', 0), img_info.get('y0', 0), img_info.get('x1', 0), img_info.get('y1', 0)
                                width = max(0, x1 - x0)
                                height = max(0, y1 - y0)
                                
                                bbox = (x0, y0, x1, y1)
                                cropped_page = page.within_bbox(bbox)
                                img_obj = cropped_page.to_image(resolution=300)
                                pil_img = img_obj.original
                                
                                img_uuid = uuid.uuid4().hex[:12]
                                filename = f'image_{page_num}_{img_idx}_{img_uuid}.png'
                                filepath = os.path.join(images_dir, filename)
                                pil_img.save(filepath)
                                
                                caption = self._find_nearby_caption(page, x0, y0, x1, y1)
                                
                                images.append({
                                    'id': img_idx,
                                    'filename': filename,
                                    'page': page_num,
                                    'caption': caption
                                })
                                img_idx += 1
                            except Exception as e:
                                continue
        except Exception as e:
            pass
        
        return images

    def _find_nearby_caption(self, page, x0, y0, x1, y1):
        try:
            words = page.extract_words()
            nearby_texts = []
            
            search_below = {
                'x0': x0 - 50,
                'y0': y1,
                'x1': x1 + 50,
                'y1': y1 + 80
            }
            
            for word in words:
                wx, wy0, wx1, wy1 = word.get('x0', 0), word.get('top', 0), word.get('x1', 0), word.get('bottom', 0)
                if (wx >= search_below['x0'] and wx1 <= search_below['x1'] and
                    wy0 >= search_below['y0'] and wy1 <= search_below['y1']):
                    nearby_texts.append(word.get('text', ''))
            
            caption = ' '.join(nearby_texts).strip()
            
            if caption:
                fig_match = re.search(r'(?:Figure|Fig|图|Figure)\s*\d+[:\.]?\s*(.*)', caption, re.IGNORECASE)
                if fig_match:
                    caption = fig_match.group(1).strip()
            
            return caption
        except Exception:
            return ''

    def extract_tables(self, pdf_path):
        tables = []
        try:
            with pdfplumber.open(pdf_path) as pdf:
                table_idx = 1
                for page_num, page in enumerate(pdf.pages, start=1):
                    page_tables = page.extract_tables()
                    if page_tables:
                        for table_data in page_tables:
                            caption = self._find_table_caption(page, table_data)
                            content = []
                            if table_data:
                                for row in table_data:
                                    processed_row = []
                                    for cell in row:
                                        if cell is not None:
                                            processed_row.append(str(cell).strip())
                                        else:
                                            processed_row.append('')
                                    content.append(processed_row)
                            
                            tables.append({
                                'id': table_idx,
                                'page': page_num,
                                'content': content,
                                'caption': caption
                            })
                            table_idx += 1
        except Exception as e:
            pass
        
        return tables

    def _find_table_caption(self, page, table_data):
        try:
            words = page.extract_words()
            table_texts = []
            for row in table_data:
                for cell in row:
                    if cell:
                        table_texts.append(str(cell))
            
            if table_texts:
                first_cell = table_texts[0]
                for i, word in enumerate(words):
                    if first_cell in word.get('text', ''):
                        caption_area = {
                            'y0': max(0, word.get('top', 0) - 80),
                            'y1': word.get('top', 0)
                        }
                        caption_words = []
                        for w in words:
                            if (w.get('top', 0) >= caption_area['y0'] and 
                                w.get('bottom', 0) <= caption_area['y1']):
                                caption_words.append(w.get('text', ''))
                        
                        caption = ' '.join(caption_words).strip()
                        if caption:
                            tab_match = re.search(r'(?:Table|Tab|表|Table)\s*\d+[:\.]?\s*(.*)', caption, re.IGNORECASE)
                            if tab_match:
                                return tab_match.group(1).strip()
                            return caption
                        break
        except Exception:
            pass
        return ''

    def extract_formulas(self, pdf_path):
        formulas = []
        try:
            with pdfplumber.open(pdf_path) as pdf:
                formula_idx = 1
                for page_num, page in enumerate(pdf.pages, start=1):
                    text = page.extract_text() or ''
                    lines = text.split('\n')
                    
                    for line_idx, line in enumerate(lines):
                        found_formulas = []
                        
                        for pattern in self.formula_patterns:
                            matches = pattern.findall(line)
                            for match in matches:
                                match = match.strip()
                                if match and len(match) >= 2:
                                    found_formulas.append(match)
                        
                        if not found_formulas:
                            line_lower = line.lower()
                            if any(re.search(kw, line) for kw in self.formula_keywords):
                                if len(line.strip()) > 5:
                                    found_formulas.append(line.strip())
                        
                        for formula in found_formulas:
                            context_start = max(0, line_idx - 2)
                            context_end = min(len(lines), line_idx + 3)
                            context = '\n'.join(lines[context_start:context_end]).strip()
                            
                            formulas.append({
                                'id': formula_idx,
                                'latex': formula,
                                'page': page_num,
                                'context': context
                            })
                            formula_idx += 1
        except Exception as e:
            pass
        
        return formulas

    def extract_text_with_layout(self, pdf_path):
        result = []
        try:
            with pdfplumber.open(pdf_path) as pdf:
                for page_num, page in enumerate(pdf.pages, start=1):
                    words = page.extract_words()
                    page_words = []
                    for word in words:
                        page_words.append({
                            'text': word.get('text', ''),
                            'page': page_num,
                            'x0': word.get('x0', 0),
                            'y0': word.get('top', 0),
                            'x1': word.get('x1', 0),
                            'y1': word.get('bottom', 0),
                            'fontname': word.get('fontname', ''),
                            'size': word.get('size', 0)
                        })
                    result.extend(page_words)
        except Exception as e:
            pass
        
        return result

    def extract_sections(self, pdf_path):
        sections = []
        try:
            with pdfplumber.open(pdf_path) as pdf:
                full_text = ''
                for page in pdf.pages:
                    full_text += (page.extract_text() or '') + '\n\n'
                
                lines = full_text.split('\n')
                current_section = None
                current_content = []
                
                heading_patterns = [
                    re.compile(r'^(\d+(?:\.\d+)*)\s+(.+)$'),
                    re.compile(r'^(?:Section|Sec|节)\s*(\d+(?:\.\d+)*)\s*[:\.]?\s*(.*)$', re.IGNORECASE),
                    re.compile(r'^([IVXLCDM]+)\s+(.+)$'),
                    re.compile(r'^(Abstract|摘要|Introduction|引言|Methods|方法|Results|结果|Discussion|讨论|Conclusion|结论|References|参考文献|Acknowledgements|致谢)\s*$', re.IGNORECASE),
                    re.compile(r'^(Abstract|摘要|Introduction|引言|Methods|方法|Results|结果|Discussion|讨论|Conclusion|结论|References|参考文献|Acknowledgements|致谢)\s*[:\.]', re.IGNORECASE),
                ]
                
                for line in lines:
                    line_stripped = line.strip()
                    if not line_stripped:
                        continue
                    
                    is_heading = False
                    heading_text = ''
                    
                    for pattern in heading_patterns:
                        match = pattern.match(line_stripped)
                        if match:
                            groups = match.groups()
                            if len(groups) == 2:
                                heading_text = ' '.join(groups).strip()
                            else:
                                heading_text = groups[0].strip()
                            is_heading = True
                            break
                    
                    if len(line_stripped) < 80 and (line_stripped.isupper() or 
                        (len(line_stripped) < 50 and not line_stripped.endswith('.') and 
                         len(line_stripped.split()) <= 8)):
                        if not is_heading and line_stripped and not line_stripped[0].islower():
                            is_heading = True
                            heading_text = line_stripped
                    
                    if is_heading:
                        if current_section is not None:
                            content = '\n'.join(current_content).strip()
                            if content:
                                current_section['content'] = content
                                sections.append(current_section)
                        
                        current_section = {
                            'heading': heading_text,
                            'content': '',
                            'level': heading_text.count('.') if re.match(r'^\d+', heading_text) else 1
                        }
                        current_content = []
                    elif current_section is not None:
                        current_content.append(line_stripped)
                
                if current_section is not None:
                    content = '\n'.join(current_content).strip()
                    if content:
                        current_section['content'] = content
                        sections.append(current_section)
        except Exception as e:
            pass
        
        return sections

    def extract_full_text(self, pdf_path):
        full_text = ''
        try:
            with pdfplumber.open(pdf_path) as pdf:
                for page in pdf.pages:
                    text = page.extract_text()
                    if text:
                        full_text += text + '\n\n'
        except Exception as e:
            pass
        
        return full_text.strip()
