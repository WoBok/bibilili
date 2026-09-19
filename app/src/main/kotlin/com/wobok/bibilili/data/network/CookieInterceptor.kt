package com.wobok.bibilili.data.network

import com.wobok.bibilili.data.auth.CredentialStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 把已保存的凭证挂到每个请求上。
 *
 * 未登录时仍然发出请求——接口会返回 `-101`，由 UI 层引导去登录，
 * 比在这里拦截更好排查。
 */
class CookieInterceptor @Inject constructor(
    private val store: CredentialStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = store.current.value ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Cookie", credentials.asCookieHeader())
            .build()
        return chain.proceed(request)
    }
}
