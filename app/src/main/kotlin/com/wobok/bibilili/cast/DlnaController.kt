package com.wobok.bibilili.cast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 通过 UPnP AVTransport 控制投屏设备。
 *
 * 只用到三条指令：设置播放地址、播放、停止。够用了。
 */
class DlnaController(private val client: OkHttpClient) {

    suspend fun play(device: CastDevice, videoUrl: String, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                soap(device, "SetAVTransportURI", buildString {
                    append("<InstanceID>0</InstanceID>")
                    append("<CurrentURI>").append(videoUrl.xmlEscape()).append("</CurrentURI>")
                    append("<CurrentURIMetaData>")
                    append(metadata(videoUrl, title).xmlEscape())
                    append("</CurrentURIMetaData>")
                })
                soap(device, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
            }
        }

    suspend fun stop(device: CastDevice): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { soap(device, "Stop", "<InstanceID>0</InstanceID>") }
    }

    private fun soap(device: CastDevice, action: String, inner: String) {
        val body = buildString {
            append("""<?xml version="1.0" encoding="utf-8"?>""")
            append("""<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" """)
            append("""s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body>""")
            append("""<u:$action xmlns:u="$SERVICE_TYPE">""")
            append(inner)
            append("""</u:$action></s:Body></s:Envelope>""")
        }

        val request = Request.Builder()
            .url(device.controlUrl)
            .addHeader("SOAPACTION", "\"$SERVICE_TYPE#$action\"")
            .post(body.toRequestBody("text/xml; charset=\"utf-8\"".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "$action 失败：HTTP ${response.code}" }
        }
    }

    /** DIDL-Lite 元数据，电视上显示的标题来自这里。 */
    private fun metadata(url: String, title: String): String = buildString {
        append("""<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" """)
        append("""xmlns:dc="http://purl.org/dc/elements/1.1/" """)
        append("""xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">""")
        append("""<item id="0" parentID="-1" restricted="1">""")
        append("<dc:title>").append(title.xmlEscape()).append("</dc:title>")
        append("<upnp:class>object.item.videoItem</upnp:class>")
        append("""<res protocolInfo="http-get:*:video/mp4:*">""")
        append(url.xmlEscape())
        append("</res></item></DIDL-Lite>")
    }

    private companion object {
        const val SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"
    }
}

private fun String.xmlEscape(): String = buildString(length) {
    this@xmlEscape.forEach {
        when (it) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(it)
        }
    }
}
