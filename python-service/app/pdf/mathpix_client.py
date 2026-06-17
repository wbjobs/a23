import os
import base64
import requests
import pdfplumber
from config import Config


class MathpixClient:
    def __init__(self):
        self.app_id = os.getenv('MATHPIX_APP_ID', Config.MATHPIX_APP_ID if hasattr(Config, 'MATHPIX_APP_ID') else '')
        self.app_key = os.getenv('MATHPIX_APP_KEY', Config.MATHPIX_APP_KEY if hasattr(Config, 'MATHPIX_APP_KEY') else '')
        self.api_url = 'https://api.mathpix.com/v3/text'
        self.timeout = 30
        self.enabled = os.getenv('MATHPIX_ENABLED', 'false').lower() == 'true' or (hasattr(Config, 'MATHPIX_ENABLED') and Config.MATHPIX_ENABLED)

    def _image_to_base64(self, image_path):
        try:
            with open(image_path, 'rb') as f:
                return base64.b64encode(f.read()).decode('utf-8')
        except Exception:
            return ''

    def _build_headers(self):
        return {
            'app_id': self.app_id,
            'app_key': self.app_key,
            'Content-type': 'application/json'
        }

    def ocr_image(self, image_path):
        if not self.enabled or not self.app_id or not self.app_key:
            return ''

        try:
            base64_img = self._image_to_base64(image_path)
            if not base64_img:
                return ''

            payload = {
                'src': f'data:image/png;base64,{base64_img}',
                'formats': ['latex_styled', 'text'],
                'data_options': {
                    'include_asciimath': True,
                    'include_latex': True
                }
            }

            response = requests.post(
                self.api_url,
                json=payload,
                headers=self._build_headers(),
                timeout=self.timeout
            )
            response.raise_for_status()
            result = response.json()

            return result.get('latex_styled', '')
        except Exception:
            return ''

    def ocr_pdf_page(self, pdf_path, page_num, output_dir):
        if not self.enabled:
            return ''

        try:
            os.makedirs(output_dir, exist_ok=True)

            with pdfplumber.open(pdf_path) as pdf:
                if page_num < 1 or page_num > len(pdf.pages):
                    return ''

                page = pdf.pages[page_num - 1]
                img_obj = page.to_image(resolution=300)
                pil_img = img_obj.original

                img_filename = f'page_{page_num}.png'
                img_path = os.path.join(output_dir, img_filename)
                pil_img.save(img_path)

                return self.ocr_image(img_path)
        except Exception:
            return ''

    def batch_ocr_formulas(self, pdf_path, output_dir):
        if not self.enabled:
            return []

        formulas = []
        try:
            os.makedirs(output_dir, exist_ok=True)

            with pdfplumber.open(pdf_path) as pdf:
                for page_num, page in enumerate(pdf.pages, start=1):
                    try:
                        text = page.extract_text() or ''
                        lines = text.split('\n')

                        for line_idx, line in enumerate(lines):
                            line_stripped = line.strip()
                            if len(line_stripped) < 3:
                                continue

                            has_math_indicators = any(kw in line_stripped for kw in [
                                '\\frac', '\\sqrt', '\\sum', '\\int', '\\prod',
                                '\\alpha', '\\beta', '\\gamma', '\\delta',
                                '\\leq', '\\geq', '\\neq', '\\approx',
                                '$', '\\[', '\\('
                            ])

                            if has_math_indicators or (len(line_stripped) > 10 and not line_stripped.endswith('.')):
                                words = page.extract_words()
                                line_words = [w for w in words if abs(w.get('top', 0) - (line_idx * 15)) < 20]

                                if line_words:
                                    x0 = min(w.get('x0', 0) for w in line_words)
                                    y0 = min(w.get('top', 0) for w in line_words)
                                    x1 = max(w.get('x1', 0) for w in line_words)
                                    y1 = max(w.get('bottom', 0) for w in line_words)

                                    padding = 10
                                    bbox = (
                                        max(0, x0 - padding),
                                        max(0, y0 - padding),
                                        min(page.width, x1 + padding),
                                        min(page.height, y1 + padding)
                                    )

                                    cropped_page = page.within_bbox(bbox)
                                    img_obj = cropped_page.to_image(resolution=300)
                                    pil_img = img_obj.original

                                    img_filename = f'formula_p{page_num}_{line_idx}.png'
                                    img_path = os.path.join(output_dir, img_filename)
                                    pil_img.save(img_path)

                                    latex = self.ocr_image(img_path)
                                    if latex:
                                        formulas.append({
                                            'page': page_num,
                                            'latex': latex,
                                            'x0': x0,
                                            'y0': y0,
                                            'x1': x1,
                                            'y1': y1,
                                            'context': line_stripped
                                        })
                    except Exception:
                        continue
        except Exception:
            pass

        return formulas
