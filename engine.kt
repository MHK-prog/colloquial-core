import org.json.JSONArray

object PersianTransformer {
    data class Rule(val source: String, val replacement: String, val matchSource: String = matchKey(source))

    private fun isSeparator(character: Char): Boolean = character == ' ' || character == '\u200c'

    private fun foldCharacter(character: Char): Char = if (character == '\u0622') '\u0627' else character

    private fun matchKey(source: String): String {
        val key = StringBuilder(source.length)
        for (character in source) if (!isSeparator(character)) key.append(foldCharacter(character))
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
            if (isSeparator(expected)) {
                val separatorStart = textIndex
                while (textIndex < text.length && isSeparator(text[textIndex])) textIndex++
                val isSourceEdge = ruleIndex == 0 || ruleIndex == source.length - 1
                if (isSourceEdge && textIndex == separatorStart) return -1
                continue
            }
            if (textIndex >= text.length || isSeparator(text[textIndex])
                || foldCharacter(text[textIndex]) != foldCharacter(expected)) return -1
            textIndex++
        }
        return textIndex
    }

    fun transform(text: String, rulesJson: String): String = transform(text, loadRules(rulesJson))

    fun transform(text: String, rules: List<Rule>): String {
        var sourceText = text
        for (rule in rules) {
            if (rule.source.length == 1) sourceText = sourceText.replace(rule.source, rule.replacement)
        }
        val textRules = rules.filter { it.source.length != 1 }
        val result = StringBuilder(sourceText.length)
        var originalCursor = 0
        var index = 0
        while (index < sourceText.length) {
            var matchedRule: Rule? = null
            var matchEndIndex = -1
            for (rule in textRules) {
                val end = matchEnd(sourceText, index, rule.source)
                if (end >= 0) {
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
