package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.util.MediaPoolManager

data class MediaLink(
    val type: String,
    val id: String,
    val base64Data: String,
    val mimeType: String
)

object MediaLinkParser {
    private val LINK_PATTERN_PLAIN = Regex(
        """<link\s+type=\"(audio|video)\"\s+id=\"([^\"]+)\"\s*>.*?</link>""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val LINK_PATTERN_ESCAPED = Regex(
        """<link\s+type=\\\"(audio|video)\\\"\s+id=\\\"([^\"]+)\\\"\s*>.*?</link>""",
        RegexOption.DOT_MATCHES_ALL
    )

    fun hasMediaLinks(message: String): Boolean {
        return LINK_PATTERN_PLAIN.containsMatchIn(message) || LINK_PATTERN_ESCAPED.containsMatchIn(message)
    }

    fun extractMediaLinks(message: String): List<MediaLink> {
        val links = mutableListOf<MediaLink>()
        val seenIds = mutableSetOf<String>()

        fun collectFromPattern(pattern: Regex) {
            pattern.findAll(message).forEach { match ->
                val type = match.groupValues[1]
                val id = match.groupValues[2]

                if (id == "error") {
                    return@forEach
                }

                if (!seenIds.add("$type:$id")) {
                    return@forEach
                }

                val mediaData = MediaPoolManager.getMedia(id) ?: return@forEach
                links.add(
                    MediaLink(
                        type = type,
                        id = id,
                        base64Data = mediaData.base64,
                        mimeType = mediaData.mimeType
                    )
                )
            }
        }

        collectFromPattern(LINK_PATTERN_PLAIN)
        collectFromPattern(LINK_PATTERN_ESCAPED)

        return links
    }

    fun removeMediaLinks(message: String): String {
        return message
            .replace(LINK_PATTERN_PLAIN, "")
            .replace(LINK_PATTERN_ESCAPED, "")
    }
}
