function parseRules(input) {
  const rules = typeof input === 'string' ? JSON.parse(input) : input;
  if (!Array.isArray(rules)) throw new TypeError('Rules must be an array of [source, replacement] pairs.');
  return rules.map((pair) => {
    if (!Array.isArray(pair) || pair.length !== 2 || typeof pair[0] !== 'string' || !pair[0] || typeof pair[1] !== 'string') {
      throw new TypeError('Each rule must contain two strings and a non-empty source.');
    }
    return pair;
  }).sort((left, right) => right[0].length - left[0].length);
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
  for (let index = 0; index < sourceText.length;) {
    const rule = textRules.find(([source]) => sourceText.startsWith(source, index));
    if (!rule) result += sourceText[index++];
    else {
      result += rule[1];
      index += rule[0].length;
    }
  }
  return result;
}

if (typeof module !== 'undefined') module.exports = { parseRules, transform };
