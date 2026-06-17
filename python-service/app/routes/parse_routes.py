import os
import uuid
from flask import Blueprint, request, jsonify, current_app
from app.pdf.grobid_client import GrobidClient
from app.pdf.pdfplumber_extractor import PdfPlumberExtractor

parse_bp = Blueprint('parse', __name__, url_prefix='/api')

grobid_client = GrobidClient()
pdfplumber_extractor = PdfPlumberExtractor()


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in current_app.config.get('ALLOWED_EXTENSIONS', {'pdf'})


def merge_figures(grobid_figures, pdf_images):
    merged = []
    grobid_by_page = {}
    
    for fig in grobid_figures:
        page = fig.get('coords', {}).get('page', 0)
        if page not in grobid_by_page:
            grobid_by_page[page] = []
        grobid_by_page[page].append(fig)
    
    idx = 1
    for img in pdf_images:
        entry = {
            'id': idx,
            'filename': img.get('filename', ''),
            'page': img.get('page', 0),
            'caption': img.get('caption', ''),
            'label': '',
            'coords': {}
        }
        page = img.get('page', 0)
        if page in grobid_by_page and grobid_by_page[page]:
            grobid_fig = grobid_by_page[page].pop(0)
            if not entry['caption']:
                entry['caption'] = grobid_fig.get('caption', '')
            entry['label'] = grobid_fig.get('label', '')
            entry['coords'] = grobid_fig.get('coords', {})
        merged.append(entry)
        idx += 1
    
    for page, figs in grobid_by_page.items():
        for fig in figs:
            merged.append({
                'id': idx,
                'filename': '',
                'page': page,
                'caption': fig.get('caption', ''),
                'label': fig.get('label', ''),
                'coords': fig.get('coords', {})
            })
            idx += 1
    
    return merged


def merge_tables(grobid_tables, pdfplumber_tables):
    merged = []
    idx = 1
    
    for tbl in pdfplumber_tables:
        entry = {
            'id': idx,
            'page': tbl.get('page', 0),
            'content': tbl.get('content', []),
            'caption': tbl.get('caption', '')
        }
        merged.append(entry)
        idx += 1
    
    grobid_ids = {(t.get('caption', '')) for t in pdfplumber_tables}
    for tbl in grobid_tables:
        if tbl.get('caption', '') not in grobid_ids:
            entry = {
                'id': idx,
                'page': 0,
                'content': tbl.get('content', []),
                'caption': tbl.get('caption', '')
            }
            merged.append(entry)
            idx += 1
    
    return merged


@parse_bp.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'ok',
        'service': 'pdf-parser-service',
        'version': '1.0.0'
    }), 200


@parse_bp.route('/parse', methods=['POST'])
def parse_pdf():
    if 'file' not in request.files:
        return jsonify({
            'success': False,
            'error': 'No file part in request'
        }), 400
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({
            'success': False,
            'error': 'No file selected'
        }), 400
    
    if not allowed_file(file.filename):
        return jsonify({
            'success': False,
            'error': 'Only PDF files are allowed'
        }), 400
    
    temp_dir = current_app.config.get('TEMP_DIR')
    os.makedirs(temp_dir, exist_ok=True)
    
    file_uuid = uuid.uuid4().hex
    pdf_filename = f'{file_uuid}.pdf'
    pdf_path = os.path.join(temp_dir, pdf_filename)
    output_dir = os.path.join(temp_dir, file_uuid)
    os.makedirs(output_dir, exist_ok=True)
    
    try:
        file.save(pdf_path)
        
        grobid_result = None
        try:
            grobid_result = grobid_client.process_pdf(pdf_path)
        except Exception as e:
            grobid_result = None
        
        try:
            pdf_images = pdfplumber_extractor.extract_images(pdf_path, output_dir)
        except Exception:
            pdf_images = []
        
        try:
            pdf_tables = pdfplumber_extractor.extract_tables(pdf_path)
        except Exception:
            pdf_tables = []
        
        try:
            pdf_formulas = pdfplumber_extractor.extract_formulas(pdf_path)
        except Exception:
            pdf_formulas = []
        
        try:
            pdf_sections = pdfplumber_extractor.extract_sections(pdf_path)
        except Exception:
            pdf_sections = []
        
        try:
            full_text = pdfplumber_extractor.extract_full_text(pdf_path)
        except Exception:
            full_text = ''
        
        title = ''
        abstract = ''
        authors = []
        keywords = []
        citations = []
        grobid_body = []
        grobid_figures = []
        grobid_tables = []
        
        if grobid_result:
            title = grobid_result.get('title', '')
            abstract = grobid_result.get('abstract', '')
            authors = grobid_result.get('authors', [])
            keywords = grobid_result.get('keywords', [])
            citations = grobid_result.get('citations', [])
            grobid_body = grobid_result.get('body', [])
            grobid_figures = grobid_result.get('figures', [])
            grobid_tables = grobid_result.get('tables', [])
        
        sections = grobid_body if grobid_body else pdf_sections
        
        figures = merge_figures(grobid_figures, pdf_images)
        tables = merge_tables(grobid_tables, pdf_tables)
        
        result = {
            'success': True,
            'title': title,
            'abstract': abstract,
            'authors': authors,
            'keywords': keywords,
            'citations': citations,
            'sections': sections,
            'figures': figures,
            'tables': tables,
            'formulas': pdf_formulas,
            'full_text': full_text,
            'references': citations,
            'grobid_available': grobid_result is not None
        }
        
        try:
            if os.path.exists(pdf_path):
                os.remove(pdf_path)
        except Exception:
            pass
        
        return jsonify(result), 200
        
    except Exception as e:
        try:
            if os.path.exists(pdf_path):
                os.remove(pdf_path)
        except Exception:
            pass
        
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500
