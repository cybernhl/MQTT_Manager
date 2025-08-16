package idv.neo.protocol.communication

class JVMPlatform : Platform {
    override val name: String = System.getProperty("os.name")
}

actual fun getPlatform(): Platform = JVMPlatform()