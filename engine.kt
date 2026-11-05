import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Matcher
import java.util.regex.Pattern

object PersianTransformer {
    private const val ZWNJ = '\u200c'
    private val verbPrefixes = arrayOf("می‌", "نمی‌", "ب")
    private val verbEndings = arrayOf(
        "م" to "م", "ی" to "ی", "د" to "ه",
        "یم" to "یم", "ید" to "ین", "ند" to "ن"
    )
    @Volatile private var cachedRulesJson: String? = null
    @Volatile private var cachedRules: PreparedRules? = null

    /** Parses and prepares rules once, including all forms derived from verb_stems. */
    @Synchronized
    fun loadRules(rulesJson: String): PreparedRules {
        if (cachedRulesJson == rulesJson) cachedRules?.let { return it }
        val rules = JSONObject(rulesJson)
        val suffixes = readPairs(rules.optJSONArray("suffixes"))
        val prepared = PreparedRules(
            readPairs(rules.optJSONArray("normalizations")),
            readPairs(rules.optJSONArray("prefixes")),
            readPairs(rules.optJSONArray("phrases")),
            readContextualPairs(rules.optJSONArray("contextualVerbs")),
            suffixes,
            readPairs(rules.optJSONArray("words")),
            expandVerbStems(readPairs(rules.optJSONArray("verbs")), rules.optJSONArray("verb_stems"))
        )
        cachedRules = prepared
        cachedRulesJson = rulesJson
        return prepared
    }

    /** Keeps the existing JSON-string API while reusing its prepared rules. */
    fun transform(text: String, rulesJson: String): String = transform(text, loadRules(rulesJson))

    /** Transforms text using rules prepared by [loadRules]. */
    fun transform(text: String, rules: PreparedRules): String {
        var result = replacePairs(text, rules.normalizations)
        result = replacePairs(result, rules.prefixes, "prefix")
        result = replacePairs(result, rules.phrases)
        result = replaceContextualPairs(result, rules.contextualVerbs)
        result = replacePairs(result, rules.suffixes, "suffix")
        result = replacePairs(result, rules.words, "word", rules.suffixes)
        result = replacePairs(result, rules.verbs)
        return result
    }

    private fun expandVerbStems(manual: List<Pair<String, String>>, stems: JSONArray?): List<Pair<String, String>> {
        val result = ArrayList<Pair<String, String>>(manual.size)
        result.addAll(manual)
        val seenSources = HashSet<String>(manual.size * 2)
        for (pair in manual) seenSources.add(pair.first)
        if (stems == null) return result

        for (index in 0 until stems.length()) {
            val stem = stems.optJSONObject(index) ?: continue
            val formal = stem.optString("f", "")
            val colloquial = stem.optString("c", "")
            if (formal.isEmpty() || colloquial.isEmpty()) continue
            val linkedFormal = formal + stem.optString("link", "")
            for (prefix in verbPrefixes) {
                for ((formalEnding, colloquialEnding) in verbEndings) {
                    val source = prefix + linkedFormal + formalEnding
                    val replacement = prefix + colloquial + colloquialEnding
                    // An explicit verb rule takes precedence over generated forms.
                    if (source != replacement && seenSources.add(source)) result.add(source to replacement)
                }
            }
        }
        return result
    }

    class PreparedRules internal constructor(
        internal val normalizations: List<Pair<String, String>>,
        internal val prefixes: List<Pair<String, String>>,
        internal val phrases: List<Pair<String, String>>,
        internal val contextualVerbs: List<ContextualPair>,
        internal val suffixes: List<Pair<String, String>>,
        internal val words: List<Pair<String, String>>,
        internal val verbs: List<Pair<String, String>>
    )

    private fun readContextualPairs(array: JSONArray?): List<ContextualPair> {
        if (array == null) return emptyList()
        val result = ArrayList<ContextualPair>(array.length())
        for (index in 0 until array.length()) {
            val rule = array.optJSONArray(index) ?: continue
            if (rule.length() < 3) continue
            val source = rule.optString(0)
            val replacement = rule.optString(1)
            val contextsArray = rule.optJSONArray(2) ?: continue
            val contexts = ArrayList<String>(contextsArray.length())
            for (contextIndex in 0 until contextsArray.length()) {
                val context = contextsArray.optString(contextIndex).trim()
                if (context.isNotEmpty()) contexts.add(context)
            }
            if (source.isNotEmpty() && replacement.isNotEmpty() && contexts.isNotEmpty()) {
                result.add(ContextualPair(source, replacement, contexts))
            }
        }
        return result
    }

    private fun replaceContextualPairs(text: String, rules: List<ContextualPair>): String {
        var result = text
        for (rule in rules) {
            for (context in rule.contexts.sortedByDescending { it.length }) {
                val regex = "(?<![\\p{L}\\p{N}\\u200c])(${makeFlexiblePattern(context)})([ \\u200c]+)" +
                    makeFlexiblePattern(rule.source) + "(?![\\p{L}\\p{N}\\u200c])"
                val matcher = Pattern.compile(regex).matcher(result)
                val output = StringBuffer()
                while (matcher.find()) {
                    val replacement = matcher.group(1) + matcher.group(2) + rule.replacement
                    matcher.appendReplacement(output, Matcher.quoteReplacement(replacement))
                }
                matcher.appendTail(output)
                result = output.toString()
            }
        }
        return result
    }

    private fun readPairs(array: JSONArray?): List<Pair<String, String>> {
        if (array == null) return emptyList()
        val result = ArrayList<Pair<String, String>>(array.length())
        for (index in 0 until array.length()) {
            val pair = array.optJSONArray(index) ?: continue
            if (pair.length() < 2) continue
            val source = pair.optString(0)
            val replacement = pair.optString(1)
            if (source.isNotEmpty() && source != replacement) result.add(source to replacement)
        }
        return result
    }

    // تابع کمکی برای ساخت الگوی منعطف
    private fun makeFlexiblePattern(source: String, anchorStart: Boolean = false): String {
        val pattern = StringBuilder()
        source.forEachIndexed { i, char ->
            if (char == ZWNJ) {
                // نیم‌فاصله اول قاعده فقط در پسوند اجباریه تا وسط کلمه‌ها مچ نشه
                if (anchorStart && i == 0) {
                    pattern.append(ZWNJ)
                } else {
                    pattern.append("[ \\u200c]?")
                }
            } else {
                // Escape کردن کاراکترهای خاص Regex
                pattern.append(Regex.escape(char.toString()))
            }
        }
        return pattern.toString()
    }

    fun replacePairs(
        text: String,
        pairs: List<Pair<String, String>>,
        kind: String = "general",
        suffixes: List<Pair<String, String>> = emptyList()
    ): String {
        // فیلتر کردن جفت‌های معتبر و حذف موارد مساوی
        val mapping = pairs.filter { it.first.isNotEmpty() && it.first != it.second }.toMap()
        if (mapping.isEmpty()) return text

        // مرتب‌سازی: طولانی‌ترین اول
        val sortedSources = mapping.keys.sortedByDescending { it.length }

        val anchored = (kind == "suffix" || kind == "prefix")
        
        val patterns = sortedSources.mapIndexed { i, src ->
            "(${makeFlexiblePattern(src, anchored)})"
        }
        val combined = patterns.joinToString("|")

        val regexStr = when (kind) {
            "prefix" -> "(?<![\\p{L}\\p{N}\\u200c])(?:$combined)(?=[\\p{L}])"
            "suffix" -> "(?:$combined)(?![\\p{L}\\p{N}\\u200c])"
            "word" -> {
                val joined = suffixes
                    .filter { it.first.startsWith(ZWNJ.toString()) }
                    .map { makeFlexiblePattern(it.first.substring(1)) }
                
                val extra = if (joined.isNotEmpty()) {
                    "|(?=(?:${joined.joinToString("|")})(?![\\p{L}\\p{N}\\u200c]))"
                } else ""
                
                "(?<![\\p{L}\\p{N}\\u200c])(?:$combined)((?![\\p{L}\\p{N}])$extra)"
            }
            else -> "(?<![\\p{L}\\p{N}\\u200c])(?:$combined)(?![\\p{L}\\p{N}\\u200c])"
        }

        val nativePattern = Pattern.compile(regexStr)
        val matcher = nativePattern.matcher(text)
        val sb = StringBuffer()

        while (matcher.find()) {
            var replacement = matcher.group(0) // پیش‌فرض: خود متن مچ شده
            for (i in sortedSources.indices) {
                if (matcher.group(i + 1) != null) {
                    replacement = mapping[sortedSources[i]] ?: matcher.group(0)
                    break
                }
            }
            // Escape کردن کاراکترهای خاص در رشته جایگزین (مثل $)
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement))
        }
        matcher.appendTail(sb)

        return sb.toString()
    }

    internal data class ContextualPair(
        val source: String,
        val replacement: String,
        val contexts: List<String>
    )
}
