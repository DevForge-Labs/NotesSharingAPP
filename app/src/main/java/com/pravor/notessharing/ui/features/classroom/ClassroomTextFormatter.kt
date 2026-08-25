package com.pravor.notessharing.ui.features.classroom

sealed interface ClassroomTextBlock {
    data class Paragraph(val text: String) : ClassroomTextBlock
    data class KeyValue(val label: String, val value: String) : ClassroomTextBlock
    data class BulletList(val items: List<String>) : ClassroomTextBlock
    data class NumberedList(val items: List<Pair<String, String>>) : ClassroomTextBlock
    data class Callout(val text: String) : ClassroomTextBlock
}

object ClassroomTextFormatter {

    private val KEY_VALUE_REGEX = Regex("^([A-Za-z0-9\\s()/-]{2,32}):\\s*(.*)$")
    private val NUMBERED_REGEX = Regex("^(\\d+[.)])\\s+(.+)$")
    private val BULLET_PREFIXES = listOf("•", "-", "*", "▪", "▫", "–", "—")

    private val CALLOUT_KEYWORDS = listOf(
        "note:",
        "important:",
        "warning:",
        "instructions:",
        "instruction:",
        "please note:",
        "nb:",
        "n.b.",
        "any absence",
        "strictly prohibited",
        "mandatory"
    )

    fun format(rawText: String): List<ClassroomTextBlock> {
        if (rawText.isBlank()) return emptyList()

        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
        val rawParagraphs = normalized.split(Regex("\n{2,}"))

        val blocks = mutableListOf<ClassroomTextBlock>()

        for (para in rawParagraphs) {
            val trimmedPara = para.trim()
            if (trimmedPara.isBlank()) continue

            val lines = trimmedPara.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            var i = 0
            while (i < lines.size) {
                val line = lines[i]

                // 1. Check for Bullet List Item
                if (isBulletLine(line)) {
                    val bulletItems = mutableListOf<String>()
                    while (i < lines.size && isBulletLine(lines[i])) {
                        bulletItems.add(stripBulletPrefix(lines[i]))
                        i++
                    }
                    blocks.add(ClassroomTextBlock.BulletList(bulletItems))
                    continue
                }

                // 2. Check for Numbered List Item
                if (NUMBERED_REGEX.matches(line)) {
                    val numberedItems = mutableListOf<Pair<String, String>>()
                    while (i < lines.size) {
                        val match = NUMBERED_REGEX.find(lines[i])
                        if (match != null) {
                            val number = match.groupValues[1]
                            val text = match.groupValues[2]
                            numberedItems.add(Pair(number, text))
                            i++
                        } else {
                            break
                        }
                    }
                    blocks.add(ClassroomTextBlock.NumberedList(numberedItems))
                    continue
                }

                // 3. Check for Key-Value Pair (e.g. "Time: 20 mins", "Syllabus: ...")
                val kvMatch = KEY_VALUE_REGEX.find(line)
                if (kvMatch != null && isValidLabel(kvMatch.groupValues[1])) {
                    val label = kvMatch.groupValues[1].trim()
                    val value = kvMatch.groupValues[2].trim()
                    blocks.add(ClassroomTextBlock.KeyValue(label, value))
                    i++
                    continue
                }

                // 4. Check for Warning / Callout Paragraph or Line
                if (isCalloutLine(line)) {
                    blocks.add(ClassroomTextBlock.Callout(line))
                    i++
                    continue
                }

                // 5. Default Regular Paragraph / Sentence
                blocks.add(ClassroomTextBlock.Paragraph(line))
                i++
            }
        }

        return blocks
    }

    private fun isBulletLine(line: String): Boolean {
        return BULLET_PREFIXES.any { prefix ->
            line.startsWith("$prefix ") || (line.startsWith(prefix) && line.length > 1 && line[1] != prefix[0])
        }
    }

    private fun stripBulletPrefix(line: String): String {
        for (prefix in BULLET_PREFIXES) {
            if (line.startsWith("$prefix ")) {
                return line.removePrefix("$prefix ").trim()
            }
            if (line.startsWith(prefix)) {
                return line.removePrefix(prefix).trim()
            }
        }
        return line
    }

    private fun isValidLabel(candidate: String): Boolean {
        val trimmed = candidate.trim()
        if (trimmed.length < 2 || trimmed.length > 35) return false
        // Exclude common sentences that happen to have colons, e.g., "The details of the test are as follows:"
        if (trimmed.contains("follows", ignoreCase = true) ||
            trimmed.contains("inform you", ignoreCase = true) ||
            trimmed.contains("note that", ignoreCase = true) ||
            trimmed.split(" ").size > 5
        ) {
            return false
        }
        return true
    }

    private fun isCalloutLine(line: String): Boolean {
        val lower = line.lowercase()
        return CALLOUT_KEYWORDS.any { lower.contains(it) }
    }
}
