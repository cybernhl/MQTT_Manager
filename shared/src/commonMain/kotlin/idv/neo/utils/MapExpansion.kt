package idv.neo.utils

import kotlinx.serialization.json.*

fun Map<String, Any?>.toJsonObject(): JsonObject = buildJsonObject {
    this@toJsonObject.forEach { (key, value) ->
        put(key, value.toJsonElement())
    }
}

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> {
        val jsonObject = buildJsonObject {
            this@toJsonElement.forEach { (k, v) ->
                if (k is String) put(k, v.toJsonElement())
            }
        }
        jsonObject
    }
    is List<*> -> JsonArray(this.map { it.toJsonElement() })
    else -> JsonPrimitive(this.toString())
}