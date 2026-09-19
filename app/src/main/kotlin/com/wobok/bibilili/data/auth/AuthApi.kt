package com.wobok.bibilili.data.auth

import com.wobok.bibilili.data.network.BiliResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AuthApi {

    /** 申请二维码。扫码登录不需要任何人机验证。 */
    @GET("https://passport.bilibili.com/x/passport-login/web/qrcode/generate")
    suspend fun generateQrCode(): BiliResponse<QrGenerateDto>

    /**
     * 轮询扫码状态。
     * 用 [Response] 包一层是为了拿到 `Set-Cookie`——凭证在响应头里，不在 body。
     */
    @GET("https://passport.bilibili.com/x/passport-login/web/qrcode/poll")
    suspend fun pollQrCode(@Query("qrcode_key") key: String): Response<BiliResponse<QrPollDto>>

    /** 登录后的账号信息，同时带回当日的 WBI key。 */
    @GET("x/web-interface/nav")
    suspend fun nav(): BiliResponse<NavDto>
}

@Serializable
data class QrGenerateDto(
    /** 二维码承载的 URL，交给 ZXing 渲染。 */
    @SerialName("url") val url: String = "",
    @SerialName("qrcode_key") val qrcodeKey: String = "",
)

@Serializable
data class QrPollDto(
    @SerialName("url") val url: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("timestamp") val timestamp: Long = 0,
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String = "",
)

@Serializable
data class NavDto(
    @SerialName("isLogin") val isLogin: Boolean = false,
    @SerialName("mid") val mid: Long = 0,
    @SerialName("uname") val uname: String = "",
    @SerialName("face") val face: String = "",
    @SerialName("vipStatus") val vipStatus: Int = 0,
    @SerialName("vipType") val vipType: Int = 0,
    @SerialName("vip") val vip: VipDto? = null,
    @SerialName("level_info") val levelInfo: LevelDto? = null,
    @SerialName("wbi_img") val wbiImg: WbiImgDto? = null,
)

@Serializable
data class VipDto(
    @SerialName("status") val status: Int = 0,
    /** 毫秒时间戳。用户页的「大会员剩余 N 天」由它算出来。 */
    @SerialName("due_date") val dueDate: Long = 0,
)

@Serializable
data class LevelDto(
    @SerialName("current_level") val currentLevel: Int = 0,
)

@Serializable
data class WbiImgDto(
    @SerialName("img_url") val imgUrl: String = "",
    @SerialName("sub_url") val subUrl: String = "",
)
