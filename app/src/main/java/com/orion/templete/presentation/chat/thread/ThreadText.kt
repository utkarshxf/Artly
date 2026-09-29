package com.orion.templete.presentation.chat.thread

import android.icu.text.BreakIterator
import android.util.Patterns
import java.util.Locale

// Text analysis for message bubbles (links, emoji-only messages). Runs in the ViewModel, never in composition.
object ThreadText {

    data class Info(val links: List<ThreadLink>, val emojiOnly: Boolean)

    private val EMPTY = Info(emptyList(), false)
    private const val TRAILING_PUNCTUATION = ".,;:!?'\")]}>"

    fun analyze(text: String): Info {
        if (text.isBlank()) return EMPTY
        val emojiOnly = isEmojiOnly(text)
        return Info(links = if (emojiOnly) emptyList() else findLinks(text), emojiOnly = emojiOnly)
    }

    // http(s) links and bare domains ("artistry.app/x"); e-mail addresses are left alone
    fun findLinks(text: String): List<ThreadLink> {
        if (text.length < 4 || text.indexOf('.') < 0) return emptyList()
        val matcher = try {
            Patterns.WEB_URL.matcher(text)
        } catch (e: Exception) {
            return emptyList()
        }
        var links: ArrayList<ThreadLink>? = null
        while (matcher.find()) {
            val start = matcher.start()
            var end = matcher.end()
            if (start > 0 && (text[start - 1] == '@' || text[start - 1].isLetterOrDigit())) continue
            if (end < text.length && text[end] == '@') continue
            while (end > start && TRAILING_PUNCTUATION.indexOf(text[end - 1]) >= 0) {
                // Keep a closing bracket that belongs to the link, e.g. wiki/Foo_(bar)
                val c = text[end - 1]
                if (c == ')' && text.substring(start, end).count { it == '(' } >= text.substring(start, end).count { it == ')' }) break
                end--
            }
            if (end <= start) continue
            val raw = text.substring(start, end)
            val lower = raw.lowercase(Locale.ROOT)
            val url = when {
                lower.startsWith("http://") || lower.startsWith("https://") -> raw
                lower.contains("://") -> continue // rtsp:// and friends are not opened from chat
                else -> "https://$raw"
            }
            val list = links ?: ArrayList<ThreadLink>(2).also { links = it }
            list += ThreadLink(start, end, url)
        }
        return links ?: emptyList()
    }

    // 1..3 emoji (spaces allowed between them) and nothing else
    fun isEmojiOnly(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 48) return false
        val iterator = try {
            BreakIterator.getCharacterInstance().also { it.setText(trimmed) }
        } catch (e: Exception) {
            return false
        }
        var count = 0
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val cluster = trimmed.substring(start, end)
            if (cluster.isNotBlank()) {
                if (!isEmojiCluster(cluster)) return false
                count++
                if (count > 3) return false
            }
            start = end
            end = iterator.next()
        }
        return count in 1..3
    }

    private fun isEmojiCluster(cluster: String): Boolean {
        val keycap = cluster.indexOf('⃣') >= 0
        var pictographic = false
        var i = 0
        while (i < cluster.length) {
            val cp = cluster.codePointAt(i)
            when {
                isPictographic(cp) -> pictographic = true
                cp == 0x200D || cp == 0xFE0F || cp == 0xFE0E || cp == 0x20E3 -> Unit // ZWJ, variation selectors, keycap
                cp in 0xE0020..0xE007F -> Unit // tag sequences (subdivision flags)
                keycap && (cp in '0'.code..'9'.code || cp == '#'.code || cp == '*'.code) -> pictographic = true
                else -> return false
            }
            i += Character.charCount(cp)
        }
        return pictographic
    }

    private fun isPictographic(cp: Int): Boolean = when {
        cp in 0x1F000..0x1FAFF -> true // pictographs, emoticons, transport, flags, supplemental symbols, skin tones
        cp in 0x2600..0x27BF -> true // misc symbols + dingbats (❤ ✨ ☀)
        cp in 0x2300..0x23FF -> true // ⌚ ⏰ ⏳
        cp in 0x2B00..0x2BFF -> true // ⭐ ⬛ ⬆
        cp in 0x2190..0x21FF -> true // ↔ ↩
        cp in 0x25A0..0x25FF -> true // ▶ ◀ ◼
        cp == 0x2934 || cp == 0x2935 || cp == 0x3030 || cp == 0x303D || cp == 0x3297 || cp == 0x3299 -> true
        cp == 0x00A9 || cp == 0x00AE || cp == 0x203C || cp == 0x2049 || cp == 0x2122 || cp == 0x2139 || cp == 0x24C2 -> true
        else -> false
    }
}
