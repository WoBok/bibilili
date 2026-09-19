package com.wobok.bibilili.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY viewAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    /** 「全部观看记录」页：本地留多少就给多少，不再截断。 */
    @Query("SELECT * FROM history ORDER BY viewAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE seasonId = :seasonId")
    suspend fun find(seasonId: Long): HistoryEntity?

    @Query("SELECT MAX(viewAt) FROM history")
    suspend fun newestViewAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HistoryEntity>)

    /** 周期清理：删掉超出本地保留期的记录。 */
    @Query("DELETE FROM history WHERE viewAt < :before")
    suspend fun purgeBefore(before: Long)

    @Query("SELECT COUNT(*) FROM history WHERE viewAt >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT SUM(CASE WHEN progressSeconds < 0 THEN durationSeconds ELSE progressSeconds END) FROM history WHERE viewAt >= :since")
    suspend fun watchedSecondsSince(since: Long): Long?

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface FollowDao {

    @Query("SELECT * FROM follow WHERE (:type = 0 OR type = :type) AND (:status = 0 OR followStatus = :status) ORDER BY newEpPubTime DESC")
    fun observe(type: Int, status: Int): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FollowEntity>)

    @Query("DELETE FROM follow")
    suspend fun clear()
}
