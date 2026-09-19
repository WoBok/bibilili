package com.wobok.bibilili.data.network

import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.core.bili.error.BiliError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * B 站接口的统一外壳。
 *
 * **HTTP 200 + `code != 0` 是常态**，所以不能只看 HTTP 状态码。
 */
@Serializable
data class BiliResponse<T>(
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String = "",
    @SerialName("ttl") val ttl: Int = 1,
    @SerialName("data") val data: T? = null,
)

/** 外壳拆包。`code == 0` 但 `data` 为空时按报文异常处理，而不是静默给 null。 */
fun <T> BiliResponse<T>.toResult(): ApiResult<T> = when {
    code != 0 -> ApiResult.Failure(BiliError.Api(code, message))
    data != null -> ApiResult.Success(data)
    else -> ApiResult.Failure(
        BiliError.Malformed(IllegalStateException("code=0 但 data 为空，接口字段可能变了"))
    )
}

/** 把网络与解析异常收进 [ApiResult]，不让异常穿透到 ViewModel。 */
inline fun <T> runApi(block: () -> BiliResponse<T>): ApiResult<T> = try {
    block().toResult()
} catch (e: kotlinx.serialization.SerializationException) {
    ApiResult.Failure(BiliError.Malformed(e))
} catch (e: java.io.IOException) {
    ApiResult.Failure(BiliError.Network(e))
}
