function normalizeKey(value) {
  let key = '';
  for (let index = 0; index < value.length; index++) {
    const character = value[index];
    if (character === ' ' || character === '\u200c') continue;
    key += character === '\u0622' ? '\u0627' : character;
  }
  return key;
}

function parseRules(input) {
  const rules = typeof input === 'string' ? JSON.parse(input) : input;
  if (!Array.isArray(rules)) throw new TypeError('Rules must be an array of [source, replacement] pairs.');
  return rules.map((pair) => {
    if (!Array.isArray(pair) || pair.length !== 2 || typeof pair[0] !== 'string' || !pair[0] || typeof pair[1] !== 'string') {
      throw new TypeError('Each rule must contain two strings and a non-empty source.');
    }
    if (!normalizeKey(pair[0])) throw new TypeError('Rule source must contain a matchable character.');
    return pair;
  });
}

function normalizedText(value) {
  let text = '';
  const starts = [];
  const ends = [];
  for (let index = 0; index < value.length; index++) {
    const character = value[index];
    if (character === ' ' || character === '\u200c') continue;
    text += character === '\u0622' ? '\u0627' : character;
    starts.push(index);
    ends.push(index + 1);
  }
  return { text, starts, ends };
}

function transform(text, inputRules) {
  const rules = parseRules(inputRules);
  let sourceText = String(text == null ? '' : text);
  const characterRules = rules.filter(([source]) => source.length === 1);
  for (const [source, replacement] of characterRules) {
    sourceText = sourceText.split(source).join(replacement);
  }
  const textRules = rules
    .filter(([source]) => source.length !== 1)
    .map(([source, replacement], order) => ({ source: normalizeKey(source), replacement, order }))
    .sort((left, right) => right.source.length - left.source.length || left.order - right.order);
  const normalized = normalizedText(sourceText);
  let result = '';
  let originalCursor = 0;
  for (let index = 0; index < normalized.text.length;) {
    const rule = textRules.find((candidate) => normalized.text.startsWith(candidate.source, index));
    if (!rule) {
      index++;
      continue;
    }
    const originalStart = normalized.starts[index];
    const originalEnd = normalized.ends[index + rule.source.length - 1];
    result += sourceText.slice(originalCursor, originalStart) + rule.replacement;
    originalCursor = originalEnd;
    index += rule.source.length;
  }
  return result + sourceText.slice(originalCursor);
}

if (typeof module !== 'undefined') module.exports = { parseRules, transform };
