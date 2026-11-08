import org.json.JSONArray

object PersianTransformer {
    data class Rule(val source: String, val replacement: String, val matchSource: String = normalizeKey(source))

    private fun normalizeKey(value: String): String {
        val key = StringBuilder(value.length)
        for (character in value) {
            if (character == ' ' || character == '\u200c') continue
            key.append(if (character == '\u0622') '\u0627' else character)
        }
        return key.toString()
    }

    fun loadRules(rulesJson: String): List<Rule> {
        val input = JSONArray(rulesJson)
        val rules = ArrayList<Rule>(input.length())
        for (index in 0 until input.length()) {
            val pair = input.optJSONArray(index)
                ?: throw IllegalArgumentException("Each rule must be a two-string array.")
            if (pair.length() != 2) throw IllegalArgumentException("Each rule must contain two strings.")
            val source = pair.opt(0) as? String
                ?: throw IllegalArgumentException("Rule source must be a string.")
            val replacement = pair.opt(1) as? String
                ?: throw IllegalArgumentException("Rule replacement must be a string.")
            if (source.isEmpty()) throw IllegalArgumentException("Rule source cannot be empty.")
            val rule = Rule(source, replacement)
            if (rule.matchSource.isEmpty()) throw IllegalArgumentException("Rule source must contain a matchable character.")
            rules.add(rule)
        }
        return rules.sortedByDescending { it.matchSource.length }
    }

    private fun startsWith(text: CharSequence, value: String, offset: Int): Boolean {
        if (offset + value.length > text.length) return false
        for (index in value.indices) if (text[offset + index] != value[index]) return false
        return true
    }

    fun transform(text: String, rulesJson: String): String = transform(text, loadRules(rulesJson))

    fun transform(text: String, rules: List<Rule>): String {
        var sourceText = text
        for (rule in rules) {
            if (rule.source.length == 1) sourceText = sourceText.replace(rule.source, rule.replacement)
        }
        val textRules = rules.filter { it.source.length != 1 }
        val normalized = StringBuilder(sourceText.length)
        val starts = IntArray(sourceText.length)
        val ends = IntArray(sourceText.length)
        var normalizedLength = 0
        for (index in sourceText.indices) {
            val character = sourceText[index]
            if (character == ' ' || character == '\u200c') continue
            normalized.append(if (character == '\u0622') '\u0627' else character)
            starts[normalizedLength] = index
            ends[normalizedLength] = index + 1
            normalizedLength++
        }

        val result = StringBuilder(sourceText.length)
        var originalCursor = 0
        var index = 0
        while (index < normalizedLength) {
            val rule = textRules.firstOrNull { startsWith(normalized, it.matchSource, index) }
            if (rule == null) {
                index++
            } else {
                val originalStart = starts[index]
                val originalEnd = ends[index + rule.matchSource.length - 1]
                result.append(sourceText, originalCursor, originalStart)
                result.append(rule.replacement)
                originalCursor = originalEnd
                index += rule.matchSource.length
            }
        }
        result.append(sourceText, originalCursor, sourceText.length)
        return result.toString()
    }
}
