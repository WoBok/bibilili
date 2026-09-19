package com.wobok.bibilili.data.auth

import com.wobok.bibilili.core.bili.auth.Credentials

/**
 * 扫码登录的状态机。
 *
 * 之所以选扫码作为唯一登录方式：它是**唯一不经过人机验证**的路径。
 * 短信和密码登录都必须先过极验滑块，而极验是行为验证（采集轨迹判定真人），
 * 没有可调用的 API，原生端只能塞 WebView，且风控评分更低。
 * 扫码由已登录的官方客户端授权，凭证不经过我们的代码。
 */
sealed interface QrLoginState {

    data object Idle : QrLoginState

    /** 二维码已就绪，等待扫描。[content] 交给 ZXing 本地渲染，可做成素笺风格。 */
    data class WaitingScan(
        val qrcodeKey: String,
        val content: String,
        val expiresAtMillis: Long,
    ) : QrLoginState

    /** 已扫描，等用户在手机上点确认。 */
    data class WaitingConfirm(val qrcodeKey: String) : QrLoginState

    /** 二维码过期，需要重新生成。 */
    data object Expired : QrLoginState

    data class Success(val credentials: Credentials) : QrLoginState

    data class Failed(val message: String) : QrLoginState
}

/** `/x/passport-login/web/qrcode/poll` 的业务码。 */
enum class QrPollCode(val code: Int) {
    SUCCESS(0),
    EXPIRED(86038),
    WAITING_CONFIRM(86090),
    WAITING_SCAN(86101),
    ;

    companion object {
        fun from(code: Int): QrPollCode? = entries.firstOrNull { it.code == code }
    }
}

object QrLoginDefaults {
    /** 轮询间隔。2 秒是官方页面的节奏，再快没有意义且增加风控风险。 */
    const val POLL_INTERVAL_MILLIS = 2_000L

    /** 二维码有效期。 */
    const val VALID_MILLIS = 180_000L

    /** 剩余这么久时把边框染成强调色，提示快过期了。 */
    const val WARN_REMAINING_MILLIS = 20_000L
}
