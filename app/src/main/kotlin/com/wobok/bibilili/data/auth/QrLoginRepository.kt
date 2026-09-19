package com.wobok.bibilili.data.auth

import com.wobok.bibilili.core.bili.auth.parseCredentials
import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.data.network.runApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 扫码登录全流程。
 *
 * 发出一条状态流：等待扫描 → 已扫描待确认 → 成功 / 过期。
 * UI 只需要渲染状态，不关心轮询。
 */
class QrLoginRepository(
    private val api: AuthApi,
    private val store: CredentialStore,
) {

    fun login(): Flow<QrLoginState> = flow {
        val generated = runApi { api.generateQrCode() }
        val qr = when (generated) {
            is ApiResult.Failure -> {
                emit(QrLoginState.Failed("没能取到二维码"))
                return@flow
            }
            is ApiResult.Success -> generated.data
        }

        val expiresAt = System.currentTimeMillis() + QrLoginDefaults.VALID_MILLIS
        emit(QrLoginState.WaitingScan(qr.qrcodeKey, qr.url, expiresAt))

        var announcedScan = false
        while (true) {
            delay(QrLoginDefaults.POLL_INTERVAL_MILLIS)

            if (System.currentTimeMillis() > expiresAt) {
                emit(QrLoginState.Expired)
                return@flow
            }

            val response = try {
                api.pollQrCode(qr.qrcodeKey)
            } catch (e: java.io.IOException) {
                // 轮询期间断网不算失败，下一轮再试。
                continue
            }

            val body = response.body()?.payload ?: continue

            when (QrPollCode.from(body.code)) {
                QrPollCode.WAITING_SCAN -> Unit

                QrPollCode.WAITING_CONFIRM -> if (!announcedScan) {
                    announcedScan = true
                    emit(QrLoginState.WaitingConfirm(qr.qrcodeKey))
                }

                QrPollCode.EXPIRED -> {
                    emit(QrLoginState.Expired)
                    return@flow
                }

                QrPollCode.SUCCESS -> {
                    // 凭证在响应头的 Set-Cookie 里，不在 body。
                    val credentials = parseCredentials(response.headers().values("Set-Cookie"))
                    if (credentials == null) {
                        emit(QrLoginState.Failed("登录成功但没取到凭证"))
                    } else {
                        store.save(credentials)
                        emit(QrLoginState.Success(credentials))
                    }
                    return@flow
                }

                null -> {
                    emit(QrLoginState.Failed(body.message.ifBlank { "登录失败（${body.code}）" }))
                    return@flow
                }
            }
        }
    }
}
