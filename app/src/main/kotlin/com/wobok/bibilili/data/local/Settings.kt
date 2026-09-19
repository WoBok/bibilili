package com.wobok.bibilili.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wobok.bibilili.core.bili.play.PlaybackSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bibilili_settings")

/**
 * 会被记住的设置。全屏三点菜单里改完，竖屏播放页立刻生效，因为读的是同一份。
 */
class SettingsStore(private val context: Context) {

    val playback: Flow<PlaybackSettings> = context.dataStore.data.map { p ->
        PlaybackSettings(
            seekStepSeconds = p[SEEK_STEP] ?: PlaybackSettings.DEFAULT_SEEK_STEP,
            longPressSpeed = p[LONG_PRESS] ?: PlaybackSettings.DEFAULT_LONG_PRESS_SPEED,
            autoPlayNext = p[AUTO_NEXT] ?: true,
            skipOpeningEnding = p[SKIP_OP_ED] ?: true,
            preferredQn = p[PREFERRED_QN] ?: 120,
        )
    }

    suspend fun setSeekStep(seconds: Int) = context.dataStore.edit { it[SEEK_STEP] = seconds }
    suspend fun setLongPressSpeed(speed: Float) = context.dataStore.edit { it[LONG_PRESS] = speed }
    suspend fun setAutoPlayNext(on: Boolean) = context.dataStore.edit { it[AUTO_NEXT] = on }
    suspend fun setSkipOpeningEnding(on: Boolean) = context.dataStore.edit { it[SKIP_OP_ED] = on }
    suspend fun setPreferredQn(qn: Int) = context.dataStore.edit { it[PREFERRED_QN] = qn }

    /** WBI key 每日更替，缓存下来避免每次请求都去取 nav。 */
    val wbiImgKey: Flow<String?> = context.dataStore.data.map { it[WBI_IMG] }
    val wbiSubKey: Flow<String?> = context.dataStore.data.map { it[WBI_SUB] }
    val wbiFetchedAt: Flow<Long> = context.dataStore.data.map { it[WBI_AT] ?: 0L }

    suspend fun saveWbiKeys(img: String, sub: String, atMillis: Long) =
        context.dataStore.edit {
            it[WBI_IMG] = img
            it[WBI_SUB] = sub
            it[WBI_AT] = atMillis
        }

    /** 上次清理本地历史的时间，用来实现「7 天检查一次」。 */
    /**
     * 上一次增量同步追到的最新 view_at。
     *
     * 不能再用「本地库里最新的一条」当水位线：本机播放会立刻往库里写一条 `now`，
     * 下次同步第一页就判定「已经追上」，直接停在原地，别的设备上看的就再也同步不过来了。
     */
    val lastHistorySyncViewAt: Flow<Long> = context.dataStore.data.map { it[LAST_SYNC_VIEW_AT] ?: 0L }
    suspend fun setLastHistorySyncViewAt(seconds: Long) =
        context.dataStore.edit { it[LAST_SYNC_VIEW_AT] = seconds }

    val lastPurgeAt: Flow<Long> = context.dataStore.data.map { it[LAST_PURGE] ?: 0L }
    suspend fun setLastPurgeAt(millis: Long) = context.dataStore.edit { it[LAST_PURGE] = millis }

    suspend fun clear() = context.dataStore.edit { it.clear() }

    private companion object {
        val SEEK_STEP = intPreferencesKey("seek_step")
        val LONG_PRESS = floatPreferencesKey("long_press_speed")
        val AUTO_NEXT = booleanPreferencesKey("auto_play_next")
        val SKIP_OP_ED = booleanPreferencesKey("skip_op_ed")
        val PREFERRED_QN = intPreferencesKey("preferred_qn")
        val WBI_IMG = stringPreferencesKey("wbi_img")
        val WBI_SUB = stringPreferencesKey("wbi_sub")
        val WBI_AT = longPreferencesKey("wbi_at")
        val LAST_PURGE = longPreferencesKey("last_purge_at")
        val LAST_SYNC_VIEW_AT = longPreferencesKey("last_history_sync_view_at")
    }
}
