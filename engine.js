function isSeparator(character) {
  return character === ' ' || character === '\u200c';
}

function foldCharacter(character) {
  return character === '\u0622' ? '\u0627' : character;
}

function matchKey(source) {
  let key = '';
  for (let index = 0; index < source.length; index++) {
    const character = source[index];
    if (!isSeparator(character)) key += foldCharacter(character);
  }
  return key;
}

function matchEnd(text, start, source) {
  let textIndex = start;
  for (let ruleIndex = 0; ruleIndex < source.length; ruleIndex++) {
    const expected = source[ruleIndex];
    if (isSeparator(expected)) {
      const separatorStart = textIndex;
      while (textIndex < text.length && isSeparator(text[textIndex])) textIndex++;
      const isSourceEdge = ruleIndex === 0 || ruleIndex === source.length - 1;
      if (isSourceEdge && textIndex === separatorStart) return -1;
      continue;
    }
    if (textIndex >= text.length || isSeparator(text[textIndex])
        || foldCharacter(text[textIndex]) !== foldCharacter(expected)) return -1;
    textIndex++;
  }
  return textIndex;
}

function parseRules(input) {
  const rules = typeof input === 'string' ? JSON.parse(input) : input;
  if (!Array.isArray(rules)) throw new TypeError('Rules must be an array of [source, replacement] pairs.');
  return rules.map((pair, order) => {
    if (!Array.isArray(pair) || pair.length !== 2 || typeof pair[0] !== 'string' || !pair[0] || typeof pair[1] !== 'string') {
      throw new TypeError('Each rule must contain two strings and a non-empty source.');
    }
    const key = matchKey(pair[0]);
    if (!key) throw new TypeError('Rule source must contain a matchable character.');
    return { pair, keyLength: key.length, order };
  }).sort((left, right) => right.keyLength - left.keyLength || left.order - right.order).map(({ pair }) => pair);
}

function transform(text, inputRules) {
  const rules = parseRules(inputRules);
  let sourceText = String(text == null ? '' : text);
  const characterRules = rules.filter(([source]) => source.length === 1);
  for (const [source, replacement] of characterRules) {
    sourceText = sourceText.split(source).join(replacement);
  }
  const textRules = rules.filter(([source]) => source.length !== 1);
  let result = '';
  let originalCursor = 0;
  for (let index = 0; index < sourceText.length;) {
    let matchedRule = null;
    let matchEndIndex = -1;
    for (const rule of textRules) {
      const end = matchEnd(sourceText, index, rule[0]);
      if (end >= 0) {
        matchedRule = rule;
        matchEndIndex = end;
        break;
      }
    }
    if (!matchedRule) {
      index++;
    } else {
      result += sourceText.slice(originalCursor, index) + matchedRule[1];
      originalCursor = matchEndIndex;
      index = matchEndIndex;
    }
  }
  return result + sourceText.slice(originalCursor);
}

if (typeof module !== 'undefined') module.exports = { parseRules, transform };
