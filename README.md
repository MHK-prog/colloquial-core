# Persian Colloquial Core

A small, rule-based Persian text transformer for Android and JavaScript clients.

## Files

- `engine.kt` is the Android/Kotlin implementation. Call `PersianTransformer.transform(text, rulesJson)`.
- `engine.js` provides the same rule order for JavaScript clients.
- `rules.json` contains UTF-8 conversion rules grouped by transformation stage.
- `extension.json` is the versioned install manifest used by NovelReader.

The Android app uses a Java-compatible implementation bundled with the app and downloads this Kotlin source together with the JSON rules for the installed extension package. It never executes downloaded source code.

## Rule order

`normalizations` → `prefixes` → `phrases` → `contextualVerbs` → `suffixes` → `words` → `verbs`.
Most items are two-element `[source, replacement]` arrays. `contextualVerbs` entries have a third item containing allowed preceding contexts, so ambiguous forms such as `دهم` and `شوم` are only changed in clear verb phrases. `verb_stems` stores compact formal/colloquial stem pairs (`f` and `c`); the optional `link` field adds a formal connector such as `ی` for `گو`. The engines expand these into prefixed person forms when rules are first loaded, then use the generated pairs in the final `verbs` stage. Explicit entries in `verbs` take precedence for irregular forms. JavaScript clients should call `parseRules(json)` once; Kotlin clients can call `loadRules(json)` once and reuse its `PreparedRules`. The JSON root's numeric `version` identifies the rules schema.

## Updating the extension

Update the engine or rules, recalculate both SHA-256 values in `extension.json`, increment its semantic version, and publish the commit to the default branch. The Android app accepts only the two declared files and verifies their hashes before installation.
