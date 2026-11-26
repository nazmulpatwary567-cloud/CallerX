package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domain.model.CallRouteType
import com.example.domain.model.CallType
import kotlinx.coroutines.flow.Flow

@Dao
interface SipAccountDao {
    @Query("SELECT * FROM sip_accounts ORDER BY id ASC")
    fun observeAll(): Flow<List<SipAccountEntity>>

    @Query("SELECT * FROM sip_accounts ORDER BY id ASC")
    suspend fun getAll(): List<SipAccountEntity>

    @Query("SELECT * FROM sip_accounts WHERE id = :id")
    suspend fun getById(id: Long): SipAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SipAccountEntity): Long

    @Query("DELETE FROM sip_accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sip_accounts")
    suspend fun deleteAll()
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY startTime DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs")
    suspend fun getAllSnapshot(): List<CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE type = :type ORDER BY startTime DESC")
    fun observeByType(type: CallType): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE route = :route ORDER BY startTime DESC")
    fun observeByRoute(route: CallRouteType): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE route IN ('SIM1', 'SIM2') ORDER BY startTime DESC")
    fun observeSimLogs(): Flow<List<CallLogEntity>>

    @Query(
        "SELECT * FROM call_logs WHERE number LIKE '%' || :query || '%' OR contactName LIKE '%' || :query || '%' ORDER BY startTime DESC"
    )
    fun search(query: String): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs WHERE type = 'MISSED' AND isBlocked = 0")
    fun observeMissedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CallLogEntity): Long

    @Update
    suspend fun update(entity: CallLogEntity)

    @Query("UPDATE call_logs SET recordingPath = :path WHERE id = :callLogId")
    suspend fun updateRecordingPath(callLogId: Long, path: String)

    @Query("UPDATE call_logs SET duration = :durationSeconds WHERE id = :callLogId")
    suspend fun updateDuration(callLogId: Long, durationSeconds: Long)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllSnapshot(): List<ContactEntity>

    @Query(
        "SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR number LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC"
    )
    fun search(query: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE number = :number LIMIT 1")
    suspend fun findExactByNumber(number: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContactEntity): Long

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}

@Dao
interface BlockedNumberDao {
    @Query("SELECT * FROM blocked_numbers ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers")
    suspend fun getAllSnapshot(): List<BlockedNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedNumberEntity): Long

    @Query("DELETE FROM blocked_numbers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_numbers WHERE number = :number")
    suspend fun deleteByNumber(number: String)

    @Query("DELETE FROM blocked_numbers")
    suspend fun deleteAll()
}

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SmsEntity>>

    @Query("SELECT * FROM sms_messages")
    suspend fun getAllSnapshot(): List<SmsEntity>

    @Query("SELECT * FROM sms_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun observeThread(threadId: Long): Flow<List<SmsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SmsEntity): Long

    @Query("UPDATE sms_messages SET isRead = 1 WHERE threadId = :threadId")
    suspend fun markThreadRead(threadId: Long)

    @Query("DELETE FROM sms_messages")
    suspend fun deleteAll()
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings")
    suspend fun getAllSnapshot(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecordingEntity): Long

    @Delete
    suspend fun delete(entity: RecordingEntity)

    @Query("DELETE FROM recordings")
    suspend fun deleteAll()
}

@Dao
interface SpeedDialDao {
    @Query("SELECT * FROM speed_dials ORDER BY `key` ASC")
    fun observeAll(): Flow<List<SpeedDialEntity>>

    @Query("SELECT * FROM speed_dials")
    suspend fun getAllSnapshot(): List<SpeedDialEntity>

    @Query("SELECT * FROM speed_dials WHERE `key` = :key")
    suspend fun getByKey(key: Int): SpeedDialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SpeedDialEntity)

    @Query("DELETE FROM speed_dials WHERE `key` = :key")
    suspend fun deleteByKey(key: Int)

    @Query("DELETE FROM speed_dials")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun observeValue(key: String): Flow<String?>

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM settings")
    suspend fun getAllSnapshot(): List<SettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: SettingsEntity)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}
