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
