# Persian Colloquial Core

A small Persian text transformer for Android and JavaScript.

## Files

- `rules.json` is a plain JSON array of `[source, replacement]` string pairs.
- Blank lines separate related blocks: character cleanup, prefixes, phrases, exceptions, suffixes, words, and verb forms. Similar verb forms sit together.
- `engine.js` and `engine.kt` apply literal replacements with the same behavior.
- `extension.json` is the versioned install manifest used by NovelReader.

There are no regular expressions, contextual rules, generated forms, or special rule types. Single-character pairs run first for character cleanup. The remaining rules scan the text once; when sources overlap, the longest source wins, then file order breaks ties. Replacements do not get processed again during the same conversion. Every source is matched literally, including when it appears inside a longer word.

The Android app uses a matching Java implementation and downloads the Kotlin source together with the rules. It verifies both files with SHA-256; downloaded source is stored but not executed.

## Updating the extension

Edit pairs in `rules.json` and keep related rules together. After changing a declared file, recalculate its SHA-256 value and byte limit in `extension.json`, increment the extension version, then publish the changes.
