package com.wobok.bibilili.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wobok.bibilili.core.bili.auth.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 凭证落盘。用 [EncryptedSharedPreferences]，密钥进 Android Keystore。
 *
 * 刻意不用 DataStore：DataStore 没有官方加密方案，而这里存的是能直接登录账号的东西。
 */
class CredentialStore(
    private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _current = MutableStateFlow(load())
    val current: StateFlow<Credentials?> = _current.asStateFlow()

    val isLoggedIn: Boolean get() = _current.value?.isUsable == true

    private fun load(): Credentials? {
        val sessData = prefs.getString(KEY_SESSDATA, null) ?: return null
        val mid = prefs.getString(KEY_MID, null) ?: return null
        return Credentials(
            sessData = sessData,
            biliJct = prefs.getString(KEY_JCT, "").orEmpty(),
            dedeUserId = mid,
            buvid3 = prefs.getString(KEY_BUVID3, "").orEmpty(),
        ).takeIf { it.isUsable }
    }

    fun save(credentials: Credentials) {
        prefs.edit()
            .putString(KEY_SESSDATA, credentials.sessData)
            .putString(KEY_JCT, credentials.biliJct)
            .putString(KEY_MID, credentials.dedeUserId)
            .putString(KEY_BUVID3, credentials.buvid3)
            .apply()
        _current.value = credentials
    }

    /** 注销：连同本地库一起清，由调用方负责 Room 那部分。 */
    fun clear() {
        prefs.edit().clear().apply()
        _current.value = null
    }

    private companion object {
        const val FILE_NAME = "bibilili_credentials"
        const val KEY_SESSDATA = "sessdata"
        const val KEY_JCT = "bili_jct"
        const val KEY_MID = "mid"
        const val KEY_BUVID3 = "buvid3"
    }
}
