import re
import pdfplumber


def detect_formula_anchors(pdf_path, formulas=None):
    anchors = []
    try:
        formula_patterns = [
            re.compile(r'(公式)\s*(\d+\.\d+)', re.IGNORECASE),
            re.compile(r'(Equation)\s*(\d+\.\d+)', re.IGNORECASE),
            re.compile(r'(Eq\.?)\s*(\d+\.\d+)', re.IGNORECASE),
            re.compile(r'\((\d+\.\d+)\)'),
            re.compile(r'\((\d+)\)'),
        ]

        with pdfplumber.open(pdf_path) as pdf:
            anchor_idx = 1
            for page_num, page in enumerate(pdf.pages, start=1):
                try:
                    words = page.extract_words()
                    if not words:
                        continue

                    full_text = page.extract_text() or ''
                    lines = full_text.split('\n')

                    for line_idx, line in enumerate(lines):
                        line_stripped = line.strip()
                        if not line_stripped:
                            continue

                        for pattern in formula_patterns:
                            matches = pattern.finditer(line_stripped)
                            for match in matches:
                                try:
                                    number_raw = match.group(0)
                                    number = match.group(2) if pattern.groups >= 2 and len(match.groups()) >= 2 else match.group(1)

                                    if not re.match(r'^\d+(\.\d+)?$', number):
                                        continue

                                    line_words = _find_line_words(words, line_idx, line_stripped)

                                    if line_words:
                                        x0 = min(w.get('x0', 0) for w in line_words)
                                        y0 = min(w.get('top', 0) for w in line_words)
                                        x1 = max(w.get('x1', 0) for w in line_words)
                                        y1 = max(w.get('bottom', 0) for w in line_words)

                                        context_start = max(0, line_idx - 2)
                                        context_end = min(len(lines), line_idx + 3)
                                        context = '\n'.join(lines[context_start:context_end]).strip()

                                        anchors.append({
                                            'id': anchor_idx,
                                            'number': number,
                                            'number_raw': number_raw,
                                            'latex': '',
                                            'page': page_num,
                                            'x0': x0,
                                            'y0': y0,
                                            'x1': x1,
                                            'y1': y1,
                                            'context': context
                                        })
                                        anchor_idx += 1
                                except Exception:
                                    continue
                except Exception:
                    continue
    except Exception:
        pass

    if formulas and anchors:
        anchors = _match_formulas_to_anchors(anchors, formulas)

    return anchors


def _find_line_words(words, line_idx, line_text):
    line_words = []
    approx_y = line_idx * 15

    for word in words:
        word_top = word.get('top', 0)
        if abs(word_top - approx_y) < 25:
            line_words.append(word)

    if not line_words and line_text:
        for word in words:
            if word.get('text', '') in line_text:
                line_words.append(word)

    return line_words


def _match_formulas_to_anchors(anchors, formulas):
    try:
        formula_list = formulas if isinstance(formulas, list) else []

        for anchor in anchors:
            anchor_page = anchor.get('page', 0)
            anchor_y = anchor.get('y0', 0)

            best_formula = None
            best_distance = float('inf')

            for formula in formula_list:
                formula_page = formula.get('page', 0)
                formula_y = formula.get('y0', formula.get('y', 0))

                if formula_page != anchor_page:
                    continue

                distance = abs(formula_y - anchor_y)
                if distance < best_distance and distance < 200:
                    best_distance = distance
                    best_formula = formula

            if best_formula:
                anchor['latex'] = best_formula.get('latex', '')
                if 'formula_id' in best_formula:
                    anchor['formula_id'] = best_formula['formula_id']
    except Exception:
        pass

    return anchors
