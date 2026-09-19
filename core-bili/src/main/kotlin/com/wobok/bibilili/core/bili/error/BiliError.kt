package com.wobok.bibilili.core.bili.error

/**
 * B 站接口的业务错误。HTTP 200 + `code != 0` 是常态，所以业务码必须单独建模，
 * 不能只看 HTTP 状态。
 */
sealed interface BiliError {

    /** 接口返回了非 0 业务码。 */
    data class Api(val code: Int, val message: String) : BiliError

    /** 网络层失败（连接、超时、DNS）。 */
    data class Network(val cause: Throwable) : BiliError

    /** 报文解析失败——通常意味着接口改了字段。 */
    data class Malformed(val cause: Throwable) : BiliError

    /** 本地还没有可用凭证。 */
    data object NotLoggedIn : BiliError
}

/**
 * 已知业务码。取名沿用接口语义，便于在 UI 层按类型给不同文案，
 * 而不是把原始数字透给用户。
 */
enum class BiliCode(val code: Int) {
    OK(0),
    BAD_REQUEST(-400),
    NOT_LOGGED_IN(-101),

    /** 请求被拦截：通常是缺 Referer / UA 不对 / 参数签名不合法。 */
    INTERCEPTED(-412),

    /** 触发风控：需要降低频率，必要时重新走一次人机校验。 */
    RISK_CONTROL(-352),

    /** 地区或会员限制。清晰度请求过高时也会命中，应自动降档重试。 */
    GEO_OR_VIP_LIMITED(-10403),

    /** appkey 与 access_token 不属于同一组。 */
    APPKEY_MISMATCH(-663),

    /** 目标用户隐私设置未公开。 */
    PRIVACY_CLOSED(53013),
    ;

    companion object {
        private val byCode = entries.associateBy(BiliCode::code)
        fun from(code: Int): BiliCode? = byCode[code]
    }
}

/**
 * 这些错误重试没有意义，调用方不该做自动重试。
 * `-412` / `-352` 属于风控，重试只会让情况更糟。
 */
fun BiliError.isRetryable(): Boolean = when (this) {
    is BiliError.Network -> true
    is BiliError.Api -> when (BiliCode.from(code)) {
        BiliCode.INTERCEPTED, BiliCode.RISK_CONTROL,
        BiliCode.NOT_LOGGED_IN, BiliCode.GEO_OR_VIP_LIMITED,
        BiliCode.PRIVACY_CLOSED, BiliCode.APPKEY_MISMATCH,
        BiliCode.BAD_REQUEST,
        -> false
        else -> false
    }
    is BiliError.Malformed -> false
    BiliError.NotLoggedIn -> false
}
