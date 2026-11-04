const ZWNJ = '\u200c';

// الگوی منعطف: نیم‌فاصله‌های میانی اختیاری، اولیِ قواعد پسوند/پیشوند اجباری
function makeFlexiblePattern(source, anchorStart = false) {
  let pattern = '';
  source.split('').forEach((char, i) => {
    if (char === ZWNJ) {
      // نیم‌فاصله اول قاعده فقط در پسوند اجباریه تا وسط کلمه‌ها مچ نشه
      pattern += (anchorStart && i === 0) ? ZWNJ : '[ \\u200c]?';
    } else {
      pattern += char.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }
  });
  return pattern;
}

function replacePairs(text, pairs, kind = 'general', suffixes = []) {
  const mapping = new Map();
  for (const [src, dst] of pairs) {
    if (src && src !== dst) mapping.set(src, dst);
  }
  if (mapping.size === 0) return text;

  // مرتب‌سازی: طولانی‌ترین اول
  const sortedSources = Array.from(mapping.keys()).sort((a, b) => b.length - a.length);

  const anchored = (kind === 'suffix' || kind === 'prefix');
  const patterns = sortedSources.map((src, i) => `(?<rule${i}>${makeFlexiblePattern(src, anchored)})`);
  const combined = patterns.join('|');

  let regexStr = '';
  if (kind === 'prefix') {
    regexStr = `(?<![\\p{L}\\p{N}\\u200c])(?:${combined})(?=[\\p{L}])`;
  } else if (kind === 'suffix') {
    regexStr = `(?:${combined})(?![\\p{L}\\p{N}\\u200c])`;
  } else if (kind === 'word') {
    const joined = suffixes
      .filter(([s]) => s.startsWith(ZWNJ))
      .map(([s]) => makeFlexiblePattern(s.slice(1)));
    const extra = joined.length ? `|(?=(?:${joined.join('|')})(?![\\p{L}\\p{N}\\u200c]))` : '';
    regexStr = `(?<![\\p{L}\\p{N}\\u200c])(?:${combined})((?![\\p{L}\\p{N}])${extra})`;
  } else {
    regexStr = `(?<![\\p{L}\\p{N}\\u200c])(?:${combined})(?![\\p{L}\\p{N}\\u200c])`;
  }

  const regex = new RegExp(regexStr, 'gu');

  return text.replace(regex, (...args) => {
    const groups = args[args.length - 1];
    for (let i = 0; i < sortedSources.length; i++) {
      if (groups[`rule${i}`] !== undefined) {
        return mapping.get(sortedSources[i]);
      }
    }
    return args[0];
  });
}

function replaceContextualPairs(text, rules) {
  let result = text;
  for (const [source, replacement, rawContexts] of rules || []) {
    if (!source || !replacement || !Array.isArray(rawContexts)) continue;
    const contexts = rawContexts.filter(Boolean).sort((a, b) => b.length - a.length);
    for (const context of contexts) {
      const pattern = new RegExp(
        `(?<![\\p{L}\\p{N}\\u200c])(${makeFlexiblePattern(context)})([ \\u200c]+)` +
        `${makeFlexiblePattern(source)}(?![\\p{L}\\p{N}\\u200c])`,
        'gu'
      );
      result = result.replace(pattern, (_, before, separator) => before + separator + replacement);
    }
  }
  return result;
}

function transform(text, rules) {
  const suffixes = Array.isArray(rules.suffixes) ? rules.suffixes : [];
  let result = replacePairs(text, rules.normalizations || []);
  result = replacePairs(result, rules.prefixes || [], 'prefix');
  result = replacePairs(result, rules.phrases || []);
  result = replaceContextualPairs(result, rules.contextualVerbs || []);
  result = replacePairs(result, suffixes, 'suffix');
  result = replacePairs(result, rules.words || [], 'word', suffixes);
  return replacePairs(result, rules.verbs || []);
}

if (typeof module !== 'undefined') module.exports = { makeFlexiblePattern, replacePairs, transform };
