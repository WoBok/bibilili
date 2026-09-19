package com.wobok.bibilili.data.network

import com.wobok.bibilili.core.bili.sign.WbiKeys
import com.wobok.bibilili.core.bili.sign.WbiSigner
import com.wobok.bibilili.data.api.BiliApi
import com.wobok.bibilili.data.auth.AuthApi
import com.wobok.bibilili.data.auth.CredentialStore
import com.wobok.bibilili.data.local.SettingsStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val BASE_URL = "https://api.bilibili.com/"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    fun okHttp(
        credentialStore: CredentialStore,
        wbiKeyProvider: WbiKeyProvider,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(BiliHeaderInterceptor())
        .addInterceptor(CookieInterceptor(credentialStore))
        .addInterceptor(WbiInterceptor(wbiKeyProvider))
        .build()

    /** 图片专用：只带必需的两个头，放宽并发，不碰 Cookie 与 WBI。 */
    fun imageOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .dispatcher(
            okhttp3.Dispatcher().apply {
                maxRequests = 48
                maxRequestsPerHost = 16
            }
        )
        .connectionPool(okhttp3.ConnectionPool(16, 5, TimeUnit.MINUTES))
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", BiliHeaderInterceptor.BROWSER_UA)
                    .header("Referer", BiliHeaderInterceptor.REFERER)
                    .build()
            )
        }
        .build()

    fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun biliApi(retrofit: Retrofit): BiliApi = retrofit.create(BiliApi::class.java)
    fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}

/**
 * WBI key 的缓存与刷新。两把 key 全站统一、**每日更替**。
 *
 * 拦截器工作在 OkHttp 线程上，只能同步取值，所以这里用 [runBlocking] 读 DataStore；
 * 命中缓存时不涉及 IO，过期时才会去请求一次 nav。
 */
class CachedWbiKeyProvider(
    private val settings: SettingsStore,
    private val navFetcher: suspend () -> Pair<String, String>?,
) : WbiKeyProvider {

    @Volatile private var cached: WbiKeys? = null
    @Volatile private var cachedAt: Long = 0

    override fun blockingKeys(): WbiKeys? {
        val now = System.currentTimeMillis()
        cached?.let { if (now - cachedAt < REFRESH_INTERVAL_MILLIS) return it }

        return runBlocking {
            val img = settings.wbiImgKey.first()
            val sub = settings.wbiSubKey.first()
            val at = settings.wbiFetchedAt.first()

            if (!img.isNullOrBlank() && !sub.isNullOrBlank() && now - at < REFRESH_INTERVAL_MILLIS) {
                WbiKeys(img, sub).also { cached = it; cachedAt = at }
            } else {
                val fetched = runCatching { navFetcher() }.getOrNull() ?: return@runBlocking cached
                val keys = WbiKeys(
                    imgKey = WbiSigner.keyFromUrl(fetched.first),
                    subKey = WbiSigner.keyFromUrl(fetched.second),
                )
                settings.saveWbiKeys(keys.imgKey, keys.subKey, now)
                cached = keys
                cachedAt = now
                keys
            }
        }
    }

    private companion object {
        /** 每日更替，留一点余量，12 小时刷一次。 */
        const val REFRESH_INTERVAL_MILLIS = 12 * 60 * 60 * 1000L
    }
}
