package idv.neo.utils

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

fun getNetworkClazzC():String {
    return NetworkInterface.getNetworkInterfaces()
        .asSequence()
        .flatMap { it.inetAddresses.asSequence() }
        .filterIsInstance<Inet4Address>()
        .filter { inetAddress ->
            inetAddress.isSiteLocalAddress && !inetAddress.hostAddress.contains(":") &&
                    inetAddress.hostAddress != "127.0.0.1"
        }
        .map {
            it.hostAddress.substringBeforeLast('.')
        }
        .first ()
        .split('.')[2]
}

fun getSelfIpAddress(): String? {
    return try {
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filter { inetAddress ->
                inetAddress.isSiteLocalAddress && !inetAddress.hostAddress.contains(":") &&
                        inetAddress.hostAddress != "127.0.0.1"
            }
            .filterIsInstance<Inet4Address>()
            .firstOrNull()
            ?.hostAddress
    } catch (e: Exception) {
        null
    }
}

fun getSelfPrivateIpAddress(): String? {
    return try {
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .filter { it.isUp && !it.isLoopback } // 過濾有效且非迴環的接口
            .flatMap { it.inetAddresses.asSequence() }
            .filter { inetAddress ->
                inetAddress.isSiteLocalAddress && !inetAddress.hostAddress.contains(":") &&
                        inetAddress.hostAddress != "127.0.0.1"
            }
            .filterIsInstance<Inet4Address>()
            .find { isPrivateIPv4(it) }          // 使用私有 IP 判斷函式
            ?.hostAddress
    } catch (e: Exception) {
        null
    }
}

fun getSubnetPrefix(): String {
    return NetworkInterface.getNetworkInterfaces()
        .asSequence()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.asSequence() }
        .filterIsInstance<Inet4Address>()
        .find { isPrivateIPv4(it) }
        ?.hostAddress
        ?.split('.')
        ?.let { parts ->
            when (parts.firstOrNull()) {
                "10" -> parts.take(1)
                "172" -> if (parts.size >= 2) parts.take(2) else emptyList()
                "192" -> if (parts.size >= 3) parts.take(3) else emptyList()
                else -> emptyList()
            }
        }
        ?.joinToString(".")
        ?: "192.168.1"
}

private fun isPrivateIPv4(address: Inet4Address): Boolean {
    val ipBytes = address.address
    return when (ipBytes[0].toInt() and 0xFF) {
        10 -> true                              // Class A: 10.0.0.0/8
        172 -> ipBytes[1].toInt() and 0xFF in 16..31 // Class B: 172.16.0.0/12
        192 -> ipBytes[1].toInt() and 0xFF == 168    // Class C: 192.168.0.0/16
        else -> false
    }
}

fun getInetAddress(): InetAddress {
    return  NetworkInterface.getNetworkInterfaces()
        .asSequence()
        .flatMap { it.inetAddresses.asSequence() }
        .filter { inetAddress ->
            inetAddress.isSiteLocalAddress && !inetAddress.hostAddress.contains(":") &&
                    inetAddress.hostAddress != "127.0.0.1"
        }
        .filterIsInstance<Inet4Address>().first()
}