package idv.neo.protocol.communication

import kotlin.text.lowercase

public data class MqttConfig(
    val host: String,
    val port: Int = 8883, // 預設 SSL 連接埠
    val username: String,
    val password: ByteArray, // 密碼通常建議用 ByteArray 或 CharArray
    val clientIdPrefix: String = "android-client", // Client ID 前綴
    val useSsl: Boolean = true // 是否使用 SSL
) {
    // ByteArray 的 equals 和 hashCode 需要手動實作，否則 Data Class 的比較會有問題
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MqttConfig

        if (host != other.host) return false
        if (port != other.port) return false
        if (username != other.username) return false
        if (!password.contentEquals(other.password)) return false
        if (clientIdPrefix != other.clientIdPrefix) return false
        if (useSsl != other.useSsl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port
        result = 31 * result + username.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + clientIdPrefix.hashCode()
        result = 31 * result + useSsl.hashCode()
        return result
    }
}

//enum class Role(val value: String){
//    SERVER("server"),
//    CLIENT("client"),
////    UNKNOWN("unknown")
//    ;
//    companion object {
//        fun fromString(roleStr: String): Role {
////            return entries.find { it.value == roleStr.lowercase() } ?: UNKNOWN
//            return entries.find { it.value == roleStr.lowercase() } ?:SERVER
//        }
//    }
//}

enum class Role(val value: String, val displayName: String) {
    ENDPOINT_ONE("endpoint_one", "Endpoint One"),
    ENDPOINT_TWO("endpoint_two", "Endpoint Two"),
    // UNKNOWN("unknown", "Unknown"); // 如果需要默认或未选状态
    ;
    companion object {
        fun fromString(roleStr: String): Role? {
            return entries.find { it.value == roleStr.lowercase() }
        }

        // 如果需要一个默认值，可以在 App 逻辑中处理
        fun getDefault(): Role = ENDPOINT_ONE // 或者您希望的默认角色
    }
}