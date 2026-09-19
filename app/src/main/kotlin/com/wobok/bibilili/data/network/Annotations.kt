package com.wobok.bibilili.data.network

import retrofit2.http.Headers

/**
 * 标记需要 WBI 签名的接口。
 *
 * 用法：
 * ```
 * @Wbi
 * @GET("x/web-interface/wbi/search/type")
 * suspend fun searchByType(...): BiliResponse<SearchResult>
 * ```
 * 实际是给请求打一个内部 header，由 [WbiInterceptor] 摘掉并替换成签名参数。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Wbi

/** Retrofit 不支持自定义注解驱动拦截器，所以用 header 常量转接。 */
const val WBI_HEADER_LINE = "${WbiInterceptor.HEADER_WBI}: 1"
