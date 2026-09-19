package com.wobok.bibilili.data.network

import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.core.bili.error.BiliError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * B 站接口的统一外壳。
 *
 * 两个坑：
 *  1. **HTTP 200 + `code != 0` 是常态**，不能只看 HTTP 状态码；
 *  2. **正文字段名不统一**——`x/` 开头的接口放在 `data`，而 `pgc/` 开头的（排行榜、
 *     剧集详情、时间表）放在 `result`。只认 `data` 会让这几个接口全部拿到空值，
 *     表现就是「排行榜和播放页什么都加载不出来」。
 */
@Serializable
data class BiliResponse<T>(
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String = "",
    @SerialName("ttl") val ttl: Int = 1,
    @SerialName("data") val data: T? = null,
    @SerialName("result") val result: T? = null,
) {
    /** 正文，不管接口把它叫 data 还是 result。 */
    val payload: T? get() = data ?: result
}

/** 外壳拆包。`code == 0` 但正文为空时按报文异常处理，而不是静默给 null。 */
fun <T> BiliResponse<T>.toResult(): ApiResult<T> {
    val body = payload
    return when {
        code != 0 -> ApiResult.Failure(BiliError.Api(code, message))
        body != null -> ApiResult.Success(body)
        else -> ApiResult.Failure(
            BiliError.Malformed(IllegalStateException("code=0 但 data / result 都为空，接口字段可能变了"))
        )
    }
}

/** 把网络与解析异常收进 [ApiResult]，不让异常穿透到 ViewModel。 */
inline fun <T> runApi(block: () -> BiliResponse<T>): ApiResult<T> = try {
    block().toResult()
} catch (e: kotlinx.serialization.SerializationException) {
    ApiResult.Failure(BiliError.Malformed(e))
} catch (e: java.io.IOException) {
    ApiResult.Failure(BiliError.Network(e))
}
