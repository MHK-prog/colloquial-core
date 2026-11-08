import org.json.JSONArray

object PersianTransformer {
    data class Rule(val source: String, val replacement: String, val matchSource: String = matchKey(source))

    private fun foldCharacter(character: Char): Char = if (character == '\u0622') '\u0627' else character

    private fun isWordCodePoint(codePoint: Int): Boolean {
        if (codePoint == 95) return true
        val type = Character.getType(codePoint)
        return type == Character.UPPERCASE_LETTER.toInt()
            || type == Character.LOWERCASE_LETTER.toInt()
            || type == Character.TITLECASE_LETTER.toInt()
            || type == Character.MODIFIER_LETTER.toInt()
            || type == Character.OTHER_LETTER.toInt()
            || type == Character.DECIMAL_DIGIT_NUMBER.toInt()
            || type == Character.LETTER_NUMBER.toInt()
            || type == Character.OTHER_NUMBER.toInt()
    }

    private fun hasWordBoundary(text: String, index: Int): Boolean {
        val beforeIsWord = index > 0 && isWordCodePoint(Character.codePointBefore(text, index))
        val afterIsWord = index < text.length && isWordCodePoint(Character.codePointAt(text, index))
        return beforeIsWord != afterIsWord
    }

    private fun matchKey(source: String): String {
        val key = StringBuilder(source.length)
        for (character in source) if (character != '\u200c') key.append(foldCharacter(character))
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

    private fun matchEnd(text: String, start: Int, source: String): Int {
        var textIndex = start
        for (ruleIndex in source.indices) {
            val expected = source[ruleIndex]
            if (expected == '\u200c') {
                if (textIndex < text.length && (text[textIndex] == '\u200c' || text[textIndex] == ' ')) textIndex++
                continue
            }
            if (textIndex >= text.length || foldCharacter(text[textIndex]) != foldCharacter(expected)) return -1
            textIndex++
        }
        return textIndex
    }

    fun transform(text: String, rulesJson: String): String = transform(text, loadRules(rulesJson))

    fun transform(text: String, rules: List<Rule>): String {
        val sourceText = text
        val result = StringBuilder(sourceText.length)
        var originalCursor = 0
        var index = 0
        while (index < sourceText.length) {
            var matchedRule: Rule? = null
            var matchEndIndex = -1
            for (rule in rules) {
                if (!hasWordBoundary(sourceText, index)) break
                val end = matchEnd(sourceText, index, rule.source)
                if (end >= 0 && hasWordBoundary(sourceText, end)) {
                    matchedRule = rule
                    matchEndIndex = end
                    break
                }
            }
            if (matchedRule == null) {
                index++
            } else {
                result.append(sourceText, originalCursor, index)
                result.append(matchedRule.replacement)
                originalCursor = matchEndIndex
                index = matchEndIndex
            }
        }
        result.append(sourceText, originalCursor, sourceText.length)
        return result.toString()
    }
}
