# Persian Colloquial Core

A small Persian text transformer for Android and JavaScript.

## Files

- `rules.json` is a plain JSON array of `[source, replacement]` string pairs.
- Blank lines separate related blocks: character cleanup, phrases, exceptions, suffixes, words, and verb forms. Similar verb forms sit together.
- `engine.js` and `engine.kt` apply literal replacements with the same behavior.
- `extension.json` declares the data file and action for NovelReader Extension API v1.

There are no regular expressions, contextual rules, generated forms, or special rule types. Single-character pairs run first for character cleanup. Within a source pair, ordinary spaces and Persian ZWNJ are interchangeable; an internal separator may also be absent. A separator at the start or end of a source remains a boundary marker and must be present in the text. This keeps suffix rules from matching inside unrelated words. Matching also treats آ and ا as the same character. The original text outside a matched pair stays unchanged, apart from the single-character cleanup pairs. When sources overlap, the longest normalized source wins, then file order breaks ties. Replacements do not get processed again during the same conversion. Every source can match inside a longer word.

The Android app installs `rules.json` as data through its generic manifest-based extension host. It verifies the file with SHA-256 and does not download or execute the Kotlin source. The Kotlin and JavaScript engines remain available to independent consumers of this repository.

## Updating the extension

Edit pairs in `rules.json` and keep related rules together. After changing a declared file, recalculate its SHA-256 value and byte limit in `extension.json`, increment the extension version, then publish the changes. As long as the extension uses the existing `chapter.text.transform.pairs.v1` capability, changing its rules does not require an app release.
