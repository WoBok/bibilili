package com.wobok.bibilili

import android.content.Context
import com.wobok.bibilili.cast.DlnaController
import com.wobok.bibilili.data.api.BiliApi
import com.wobok.bibilili.data.auth.AuthApi
import com.wobok.bibilili.data.auth.CredentialStore
import com.wobok.bibilili.data.auth.QrLoginRepository
import com.wobok.bibilili.data.local.AppDatabase
import com.wobok.bibilili.data.local.SettingsStore
import com.wobok.bibilili.data.network.CachedWbiKeyProvider
import com.wobok.bibilili.data.network.NetworkModule
import com.wobok.bibilili.data.repo.CatalogRepository
import com.wobok.bibilili.data.repo.FollowRepository
import com.wobok.bibilili.data.repo.HistoryRepository
import com.wobok.bibilili.data.repo.PlaybackRepository

/**
 * 手写的依赖容器。
 *
 * 没用 Hilt：一个自用 App 的依赖图就这么大，注解处理器带来的构建复杂度
 * 换不回对应的好处。全部单例，跟随 Application 生命周期。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val credentialStore = CredentialStore(appContext)
    val settings = SettingsStore(appContext)

    private val database = AppDatabase.create(appContext)

    /**
     * WBI key 需要 nav 接口，而 nav 又走同一个 OkHttp——这里有个先有鸡先有蛋。
     * 用延迟引用打破：拦截器拿到的是 provider，provider 里再去找 authApi。
     */
    private val wbiKeyProvider = CachedWbiKeyProvider(
        settings = settings,
        navFetcher = {
            val nav = runCatching { authApi.nav() }.getOrNull()?.payload?.wbiImg
            if (nav == null || nav.imgUrl.isBlank()) null else nav.imgUrl to nav.subUrl
        },
    )

    val okHttpClient = NetworkModule.okHttp(credentialStore, wbiKeyProvider)

    /**
     * 图片单独一个客户端。
     *
     * 早先和接口层共用一个 OkHttp，结果封面大面积加载不出来：接口客户端上挂着
     * Cookie 拦截器和会 runBlocking 读 DataStore 的 WBI 拦截器，首屏几十张图并发
     * 打进去，连接池和调度线程都被拖住，超时的就再也不重试了。
     * 图片只需要 Referer 和浏览器 UA，别的都是负担。
     */
    val imageOkHttpClient = NetworkModule.imageOkHttp()
    private val retrofit = NetworkModule.retrofit(okHttpClient)

    val biliApi: BiliApi = NetworkModule.biliApi(retrofit)
    val authApi: AuthApi = NetworkModule.authApi(retrofit)

    val qrLogin = QrLoginRepository(authApi, credentialStore)
    val historyRepo = HistoryRepository(biliApi, database.historyDao(), settings, credentialStore)
    val followRepo = FollowRepository(biliApi, database.followDao(), credentialStore)
    val catalogRepo = CatalogRepository(biliApi)
    val playbackRepo = PlaybackRepository(biliApi, appContext.cacheDir)
    val dlna = DlnaController(okHttpClient)

    /** 注销：凭证、本地库、设置一起清。 */
    suspend fun logout() {
        credentialStore.clear()
        historyRepo.clear()
        followRepo.clear()
        settings.clear()
    }
}
