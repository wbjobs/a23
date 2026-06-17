const FORMULA_PATTERNS = [
  { regex: /公式\s*\d+\.\d+/g, type: 'cn_full' },
  { regex: /第\d+公式/g, type: 'cn_ordinal' },
  { regex: /式\s*\(\d+\.\d+\)/g, type: 'cn_short' },
  { regex: /Equation\s+\d+\.\d+/g, type: 'en_full' },
  { regex: /Eq\.?\s*\d+\.\d+/g, type: 'en_short' },
  { regex: /\(\d+\.\d+\)/g, type: 'paren_only' }
]

export function detectFormulaReferences(text) {
  if (!text) return []
  
  const references = []
  const seen = new Set()

  for (const pattern of FORMULA_PATTERNS) {
    const regex = new RegExp(pattern.regex.source, 'g')
    let match
    while ((match = regex.exec(text)) !== null) {
      const raw = match[0]
      const number = normalizeFormulaNumber(raw)
      const start = match.index
      const end = match.index + raw.length
      const key = `${start}-${end}-${number}`
      
      if (!seen.has(key)) {
        seen.add(key)
        references.push({ raw, number, start, end })
      }
    }
  }

  references.sort((a, b) => a.start - b.start)

  const result = []
  let lastEnd = -1
  for (const ref of references) {
    if (ref.start >= lastEnd) {
      result.push(ref)
      lastEnd = ref.end
    }
  }

  return result
}

export function normalizeFormulaNumber(text) {
  if (!text) return ''
  
  let match = text.match(/(\d+\.\d+)/)
  if (match) return match[1]
  
  match = text.match(/第(\d+)公式/)
  if (match) return match[1]
  
  return text
}

export function renderFormulaAnchors(text, onClick) {
  if (!text) return ''
  
  const refs = detectFormulaReferences(text)
  if (!refs.length) return escapeHtml(text)
  
  let result = ''
  let lastIndex = 0
  
  for (const ref of refs) {
    result += escapeHtml(text.slice(lastIndex, ref.start))
    result += `<span class="formula-anchor" data-number="${ref.number}" title="跳转到公式 ${ref.number}">${escapeHtml(ref.raw)}</span>`
    lastIndex = ref.end
  }
  
  result += escapeHtml(text.slice(lastIndex))
  
  return result
}

function escapeHtml(text) {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}
