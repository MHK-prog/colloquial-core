import org.json.JSONArray

object PersianTransformer {
    data class Rule(val source: String, val replacement: String)

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
            rules.add(Rule(source, replacement))
        }
        return rules.sortedByDescending { it.source.length }
    }

    fun transform(text: String, rulesJson: String): String = transform(text, loadRules(rulesJson))

    fun transform(text: String, rules: List<Rule>): String {
        var sourceText = text
        val characterRules = rules.filter { it.source.length == 1 }
        for (rule in characterRules) sourceText = sourceText.replace(rule.source, rule.replacement)
        val textRules = rules.filter { it.source.length != 1 }
        val result = StringBuilder(sourceText.length)
        var index = 0
        while (index < sourceText.length) {
            val rule = textRules.firstOrNull { sourceText.startsWith(it.source, index) }
            if (rule == null) result.append(sourceText[index++])
            else {
                result.append(rule.replacement)
                index += rule.source.length
            }
        }
        return result.toString()
    }
}
