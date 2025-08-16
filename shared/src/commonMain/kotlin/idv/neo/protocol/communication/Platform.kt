package idv.neo.protocol.communication

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform