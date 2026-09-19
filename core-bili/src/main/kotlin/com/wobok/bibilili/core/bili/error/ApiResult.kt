package com.wobok.bibilili.core.bili.error

/** 数据层统一返回类型。避免异常穿透到 ViewModel。 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: BiliError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

inline fun <T> ApiResult<T>.onFailure(action: (BiliError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) action(error)
    return this
}
