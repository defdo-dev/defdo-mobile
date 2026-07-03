package dev.defdo.mobile.auth

internal object JsonParser {
    @Suppress("UNCHECKED_CAST")
    fun parseObject(text: String): Map<String, Any?> {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{")) throw IllegalArgumentException("not a JSON object")
        return parseObject(trimmed, 0).first
    }
}

internal fun parseSimpleJson(text: String): Map<String, Any?> = JsonParser.parseObject(text)

private fun parseObject(json: String, start: Int): Pair<Map<String, Any?>, Int> {
    val map = linkedMapOf<String, Any?>()
    var i = start + 1
    while (i < json.length) {
        val c = json[i]
        if (c == '}') return map to (i + 1)
        if (c == ',' || c.isWhitespace()) { i++; continue }
        val (key, keyEnd) = parseString(json, i)
        i = skipWhitespace(json, keyEnd)
        require(json[i] == ':') { "expected colon at $i" }
        i = skipWhitespace(json, i + 1)
        val (value, valueEnd) = parseValue(json, i)
        map[key] = value
        i = valueEnd
    }
    return map to i
}

private fun parseValue(json: String, i: Int): Pair<Any?, Int> {
    return when (json[i]) {
        '"' -> {
            val (s, end) = parseString(json, i)
            s to end
        }
        '{' -> parseObject(json, i)
        '[' -> {
            val list = mutableListOf<Any?>()
            var idx = i + 1
            while (idx < json.length) {
                if (json[idx] == ']') return list to (idx + 1)
                if (json[idx] == ',' || json[idx].isWhitespace()) { idx++; continue }
                val (v, end) = parseValue(json, idx)
                list.add(v)
                idx = end
            }
            list to idx
        }
        't' -> true to (i + 4)
        'f' -> false to (i + 5)
        'n' -> null to (i + 4)
        else -> {
            val start = i
            var end = i
            while (end < json.length && (json[end].isDigit() || json[end] == '.' || json[end] == '-')) end++
            val num = json.substring(start, end)
            (num.toLongOrNull() ?: num.toDoubleOrNull() ?: num) to end
        }
    }
}

private fun parseString(json: String, start: Int): Pair<String, Int> {
    val sb = StringBuilder()
    var i = start + 1
    while (i < json.length) {
        val c = json[i]
        if (c == '"') return sb.toString() to (i + 1)
        if (c == '\\') {
            i++
            when (json[i]) {
                '"' -> sb.append('"')
                '\\' -> sb.append('\\')
                '/' -> sb.append('/')
                'n' -> sb.append('\n')
                't' -> sb.append('\t')
            }
        } else {
            sb.append(c)
        }
        i++
    }
    return sb.toString() to i
}

private fun skipWhitespace(json: String, i: Int): Int {
    var idx = i
    while (idx < json.length && json[idx].isWhitespace()) idx++
    return idx
}
