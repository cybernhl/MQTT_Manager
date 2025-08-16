package idv.neo.utils

import idv.neo.utils.log.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.Charsets

fun stringToMap(input: String): Map<String, Any> {
    val regex = """(\w+)[:=]([^,]+)""".toRegex()
    val matches = regex.findAll(input)
//    if (matches.sumOf { it.groups.size - 1 } * 2 != input.split(',').size * 2) {
//        return emptyMap()
//    }
    if (matches.none()) {
        return emptyMap()
    }

    return matches.associate { matchResult ->
        val (key, value) = matchResult.destructured
        val parsedValue: Any = when {
            value.toIntOrNull() != null -> value.toInt()
            value.toBooleanStrictOrNull() != null -> value.toBooleanStrict()
            else -> value
        }
        key to parsedValue
    }
}

//fun String.toMap(): Map<String, Any> {
//    // 正則驗證整個字串格式 (key:value 或 key=value 的逗號分隔)
//    if (!matches("^(\\w+[:=][^,]+)(,\\w+[:=][^,]+)*$".toRegex())) {
//        return emptyMap()
//    }
//
//    val pairs = split(',')
//    return pairs.associate { pair ->
//        val (key, value) = pair.split("[:=]".toRegex(), 2)
//        val parsedValue = when {
//            value.toIntOrNull() != null -> value.toInt()
//            value.toBooleanStrictOrNull() != null -> value.toBooleanStrict()
//            else -> value
//        }
//        key to parsedValue
//    }
//}

fun String.toMap(): Map<String, Any> = runCatching {
    val trimmed = removeSurrounding("{", "}")
    //    if (!trimmed.matches("^(\\w+[:=][^,]+)(,\\w+[:=][^,]+)*$".toRegex())) {
//        return emptyMap()
//    }
    trimmed.split(",")
        .map { it.trim() } // 去除 key-value 前後空白
        .mapNotNull { entry ->
            val parts = entry.split("[:=]".toRegex(), 2)
            if (parts.size != 2) return@mapNotNull null
            val key = parts[0].trim()
            val rawValue = parts[1].trim()
            val value: Any = when {
                rawValue.toIntOrNull() != null && !rawValue.startsWith("0") -> rawValue.toInt()
                rawValue.toLongOrNull() != null && !rawValue.startsWith("0") -> rawValue.toLong()
                rawValue.equals("true", true) || rawValue.equals("false", true) -> rawValue.toBooleanStrict()
                else -> rawValue
            }
            key to value
        }.toMap()
//  removeSurrounding("{", "}")
//        .split(",")
//        .mapNotNull {
//            val parts = it.split("[:=]".toRegex(), 2)
//            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
//        }.toMap()
}.getOrElse { emptyMap() }

@OptIn(ExperimentalEncodingApi::class)
fun String.toBase64(): String {
    return Base64.encode(this.toByteArray(Charsets.UTF_8))
}

@OptIn(ExperimentalEncodingApi::class)
fun String.fromBase64(): String {
    return Base64.decode(this).toString(Charsets.UTF_8)
}

fun String.maybeBase64Decoded(): String = runCatching {
    fromBase64()
}.getOrElse {
    Log.e("Failed to decode base64: \"$this\"\nException: ${it.message}")
    this
}

fun String.toJsonElement(): JsonElement {
    return Json.parseToJsonElement(this)
}