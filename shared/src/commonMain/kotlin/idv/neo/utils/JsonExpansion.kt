package idv.neo.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.json.*

val customJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

//inline fun <reified T : Serializable> listSerializeToString(list: List<T>): String {
//////    return Json.encodeToString(list)
////    return customJson.encodeToString(list)
//    require(list.isNotEmpty()) { "List cannot be empty" }
//    return try {
//        customJson.encodeToString(list)
//    } catch (e: SerializationException) {
//        throw IllegalArgumentException("Serialization failed", e)
//    }
//}

// 序列化 List<T> 到 JSON 字串
inline fun <reified T : Any> listSerializeToString(list: List<@Serializable T>): String {
    require(list.isNotEmpty()) { "List cannot be empty" }
    return try {
        customJson.encodeToString(ListSerializer(serializer<T>()), list)
    } catch (e: SerializationException) {
        throw IllegalArgumentException("Serialization failed", e)
    }
}

////序列化处理空列表
//inline fun <reified T : Any> listSerializeToString(list: List<@Serializable T>): String {
//    return customJson.encodeToString(ListSerializer(serializer<T>()), list)
//}

//inline fun <reified T : Serializable> stringDeserializeToList(json: String): List<T> {
//////    return Json.decodeFromString(json)
////    return customJson.decodeFromString(json)
//    require(json.isNotBlank()) { "JSON string cannot be blank" }
//    return try {
//        customJson.decodeFromString(json)
//    } catch (e: SerializationException) {
//        throw IllegalArgumentException("Deserialization failed", e)
//    }
//}

// 反序列化 JSON 字串到 List<T>
inline fun <reified T : Any> stringDeserializeToList(json: String): List<T> {
    require(json.isNotBlank()) { "JSON string cannot be blank" }
    return try {
        customJson.decodeFromString(ListSerializer(serializer<T>()), json)
    } catch (e: SerializationException) {
        throw IllegalArgumentException("Deserialization failed", e)
    }
}

//// 反序列化处理空字符串与异常 // 新增参数控制错误处理
inline fun <reified T : Any> stringDeserializeToList(json: String,onErrorReturnEmpty: Boolean = true ): List<T> {
    if (json.isBlank()) return emptyList()

    return try {
        customJson.decodeFromString(ListSerializer(serializer<T>()), json)
    } catch (e: SerializationException) {
        if (onErrorReturnEmpty) emptyList()
        else throw IllegalArgumentException("Deserialization failed", e)
    }
}


//// 使用示例
// 序列化
//val smsList: List<SMS> = listOf(...)
//val smsJson = listSerializeToString(smsList) // 自動推斷 T = SMS
//
//val kkDayList: List<KKDay> = listOf(...)
//val kkDayJson = listSerializeToString(kkDayList) // 自動推斷 T = KKDay
//
//// 反序列化
//val decodedSms: List<SMS> = stringDeserializeToList(smsJson)
//val decodedKKDays: List<KKDay> = stringDeserializeToList(kkDayJson)

////性能优化建议  对于大型数据集：
// 使用流式处理 (需要添加依赖 kotlinx-serialization-json-jvm)
//val largeMessage = Message(/* 大量数据 */)
//val jsonBuffer = StringBuilder()
//customJson.encodeToJsonString(AppendingSerializer(Message.serializer()), largeMessage, jsonBuffer)

/** JsonObject -> Map<String, Any?> */
fun JsonObject.toMap(): Map<String, Any?> = this.mapValues { (_, value) ->
    value.toKotlinObject()
}

/** JsonElement -> Any? (遞迴處理) */
fun JsonElement.toKotlinObject(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> boolean
        longOrNull != null -> long
        doubleOrNull != null -> double
        else -> content
    }
    is JsonObject -> this.toMap()
    is JsonArray -> this.map { it.toKotlinObject() }
    else -> null
}
//
//fun JsonObject.toMap(): Map<String, Any?> = this.mapValues { it.value.toKotlinObject() }
//
//fun JsonElement.toKotlinObject(): Any? = when (this) {
//    is JsonNull -> null
//    is JsonPrimitive -> when {
//        isString -> content
//        booleanOrNull != null -> boolean
//        longOrNull != null -> long
//        doubleOrNull != null -> double
//        else -> content
//    }
//    is JsonObject -> this.toMap()
//    is JsonArray -> this.map { it.toKotlinObject() }
//    else -> null
//}
