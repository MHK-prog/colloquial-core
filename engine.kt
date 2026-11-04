import java.util.regex.Pattern

object PersianTransformer {
    private const val ZWNJ = '\u200c'

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
        
        // در کاتلین برای نام‌گذاری گروه‌ها از (?<name>...) استفاده میشه
        val patterns = sortedSources.mapIndexed { i, src ->
            "(?<rule$i>${makeFlexiblePattern(src, anchored)})"
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

        val regex = Regex(regexStr, setOf(RegexOption.CANON_EQ, RegexOption.COMMENTS)) // یا فقط Regex(regexStr)

        // جایگزینی با استفاده از Matcher برای دسترسی به Named Groups
        val nativePattern = Pattern.compile(regexStr)
        val matcher = nativePattern.matcher(text)
        val sb = StringBuffer()

        while (matcher.find()) {
            var replacement = matcher.group(0) // پیش‌فرض: خود متن مچ شده
            for (i in sortedSources.indices) {
                if (matcher.group("rule$i") != null) {
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
}
