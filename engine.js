function isWordCodePoint(codePoint) {
  return /[\p{L}\p{N}_]/u.test(String.fromCodePoint(codePoint));
}

function codePointBefore(text, index) {
  const last = text.charCodeAt(index - 1);
  if (last >= 0xdc00 && last <= 0xdfff && index > 1) {
    const first = text.charCodeAt(index - 2);
    if (first >= 0xd800 && first <= 0xdbff) return ((first - 0xd800) << 10) + last - 0xdc00 + 0x10000;
  }
  return last;
}

function hasWordBoundary(text, index) {
  const beforeIsWord = index > 0 && isWordCodePoint(codePointBefore(text, index));
  const afterIsWord = index < text.length && isWordCodePoint(text.codePointAt(index));
  return beforeIsWord !== afterIsWord;
}

function foldCharacter(character) {
  return character === '\u0622' ? '\u0627' : character;
}

function matchKey(source) {
  let key = '';
  for (let index = 0; index < source.length; index++) {
    const character = source[index];
    if (character !== '\u200c') key += foldCharacter(character);
  }
  return key;
}

function matchEnd(text, start, source) {
  let textIndex = start;
  for (let ruleIndex = 0; ruleIndex < source.length; ruleIndex++) {
    const expected = source[ruleIndex];
    if (expected === '\u200c') {
      if (text[textIndex] === '\u200c' || text[textIndex] === ' ') textIndex++;
      continue;
    }
    if (textIndex >= text.length || foldCharacter(text[textIndex]) !== foldCharacter(expected)) return -1;
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
  const sourceText = String(text == null ? '' : text);
  let result = '';
  let originalCursor = 0;
  for (let index = 0; index < sourceText.length;) {
    let matchedRule = null;
    let matchEndIndex = -1;
    for (const rule of rules) {
      if (!hasWordBoundary(sourceText, index)) break;
      const end = matchEnd(sourceText, index, rule[0]);
      if (end >= 0 && hasWordBoundary(sourceText, end)) {
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
