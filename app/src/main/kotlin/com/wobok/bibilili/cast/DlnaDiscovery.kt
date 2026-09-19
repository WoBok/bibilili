package com.wobok.bibilili.cast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

/**
 * 局域网里的一台可投屏设备。
 */
data class CastDevice(
    val friendlyName: String,
    val manufacturer: String,
    val location: String,
    /** AVTransport 服务的控制地址，投屏指令发到这里。 */
    val controlUrl: String,
) {
    /**
     * 是不是哔哩哔哩自家的 TV 端。
     *
     * 识别出来只是为了在列表里标一下，**不改变投屏路径**——仍然先按标准 DLNA 试。
     * 官方 App 投到自家 TV 端走的是私有协议（传 ep_id 让 TV 端自己取流），
     * 那套没有公开规格，一期不碰。
     */
    val looksLikeBiliTv: Boolean
        get() = listOf(friendlyName, manufacturer).any { text ->
            BILI_HINTS.any { text.contains(it, ignoreCase = true) }
        }

    private companion object {
        val BILI_HINTS = listOf("bilibili", "哔哩哔哩", "云视听", "小电视")
    }
}

/**
 * SSDP 设备发现。
 *
 * 自己发 M-SEARCH 而不是引第三方 UPnP 库：需要的只是「找到 AVTransport 设备 +
 * 发一条 SetAVTransportURI」，一个完整的 UPnP 栈是杀鸡用牛刀，
 * 而且多一个依赖就多一份出问题的面。
 */
object DlnaDiscovery {

    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val SEARCH_TARGET = "urn:schemas-upnp-org:device:MediaRenderer:1"
    private const val TIMEOUT_MILLIS = 4_000

    suspend fun search(): List<CastDevice> = withContext(Dispatchers.IO) {
        val message = buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: 3\r\n")
            append("ST: $SEARCH_TARGET\r\n")
            append("\r\n")
        }.toByteArray()

        val locations = linkedSetOf<String>()

        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MILLIS
                socket.send(
                    DatagramPacket(
                        message, message.size,
                        InetAddress.getByName(SSDP_ADDRESS), SSDP_PORT,
                    )
                )

                val buffer = ByteArray(2048)
                val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
                while (System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        break
                    }
                    val response = String(packet.data, 0, packet.length)
                    response.headerValue("LOCATION")?.let(locations::add)
                }
            }
        }

        locations.mapNotNull { describe(it) }
    }

    /** 拉设备描述文档，取出名字和 AVTransport 的控制地址。 */
    private fun describe(location: String): CastDevice? = runCatching {
        val xml = URL(location).readText()
        val name = xml.tagValue("friendlyName") ?: return@runCatching null
        val manufacturer = xml.tagValue("manufacturer").orEmpty()

        val control = xml.avTransportControlUrl() ?: return@runCatching null
        val base = URL(location)
        val absolute = if (control.startsWith("http")) control else URL(base, control).toString()

        CastDevice(
            friendlyName = name,
            manufacturer = manufacturer,
            location = location,
            controlUrl = absolute,
        )
    }.getOrNull()
}

private fun String.headerValue(name: String): String? =
    lineSequence()
        .firstOrNull { it.startsWith("$name:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()

private fun String.tagValue(tag: String): String? =
    Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL).find(this)?.groupValues?.get(1)?.trim()

/** 在 serviceList 里找 AVTransport，取它的 controlURL。 */
private fun String.avTransportControlUrl(): String? {
    val services = Regex("<service>(.*?)</service>", RegexOption.DOT_MATCHES_ALL).findAll(this)
    for (service in services) {
        val block = service.groupValues[1]
        if (block.contains("AVTransport", ignoreCase = true)) {
            return block.tagValue("controlURL")
        }
    }
    return null
}
