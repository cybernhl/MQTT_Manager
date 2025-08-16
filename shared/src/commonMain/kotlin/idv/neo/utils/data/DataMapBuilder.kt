package idv.neo.utils.data

import idv.neo.utils.maybeBase64Decoded

abstract class DataMapBuilder<B : DataMapBuilder<B>> protected constructor() {
    protected val map = mutableMapOf<String, Any?>(
        "topic" to null,
        "content" to null,
        "timestamp" to System.currentTimeMillis(),
        //FIXME :  status -  https://developer.mozilla.org/zh-TW/docs/Web/HTTP/Reference/Status
        "status_code" to "100"
    )
    fun channel(value: String) = apply { map["topic"] = value } as B
    fun content(value: String): B {
        map["content"] = value
        return this as B
    }
    fun timestamp(value: Long) = apply { map["timestamp"] = value } as B
    fun statusCode(value: String) = apply { map["status_code"] = value } as B

    open fun build(): Map<String, Any?> {
        return map.filterValues { it != null }
    }

    fun getContent(): String? {
        return map["content"]?.toString()?.maybeBase64Decoded()
    }

//    fun contentWithEncryption(
//        rawContent: Any,
//        en: Boolean,
//        keyExtractor: (topic: String?) -> Long? = { topic ->
//            topic?.replace("_respond", "")
//                ?.takeIf { it.all(Char::isDigit) }
//                ?.toLongOrNull()
//        }
//    ): B {
//        val finalContent = if (en) {
//            val topic = map["topic"] as? String
//            val keyId = keyExtractor(topic)
//
//            keyId?.let { KeyManager.getKey(it) }?.let { key ->
//                EncryptDecryptOperator.AESEncrypt(key, rawContent.toString())
//            } ?: rawContent.toString() // 加密失敗保留原始值
//        } else {
//            rawContent.toString()
//        }
//
//        return content(finalContent)
//    }

//    fun buildWithEncryptedContent(
//        keyProvider: (topic: String?) -> Long? = { topic ->
//            topic?.replace("_respond", "")
//                ?.takeIf { it.all(Char::isDigit) }
//                ?.toLongOrNull()
//        }
//    ): Map<String, Any?> {
//        val rawMap = build().toMutableMap()
//
//        // 提取必要參數
//        val topic = rawMap["topic"] as? String
//        val content = rawMap["content"] as? String ?: return rawMap
//
//        // 解析金鑰並加密
//        keyProvider(topic)?.let { keyId ->
//            KeyManager.getKey(keyId)?.let { key ->
//                rawMap.apply {
//                    put("content", EncryptDecryptOperator.AESEncrypt(key, content))
//                    put("is_encrypted", true) // 添加加密標記
//                }
//            }
//        }
//
//        return rawMap
//    }
}