package com.example.domain.repository

import com.example.domain.model.BlockedNumber
import com.example.domain.model.CallLog
import com.example.domain.model.Contact
import com.example.domain.model.HistoryFilter
import com.example.domain.model.Recording
import com.example.domain.model.SipAccount
import com.example.domain.model.SmsMessage
import com.example.domain.model.SmsThread
import com.example.domain.model.SpeedDial
import kotlinx.coroutines.flow.Flow

interface SipRepository {
    fun observeAccounts(): Flow<List<SipAccount>>
    suspend fun getAllAccounts(): List<SipAccount>
    suspend fun getAccountById(id: Long): SipAccount?
    suspend fun saveAccount(account: SipAccount): Long
    suspend fun deleteAccount(id: Long)
}

interface CallLogRepository {
    fun observeLogs(filter: HistoryFilter, query: String = ""): Flow<List<CallLog>>
    fun observeMissedCount(): Flow<Int>
    suspend fun insertLog(log: CallLog): Long
    suspend fun updateLog(log: CallLog)
    suspend fun deleteLog(id: Long)
    suspend fun attachRecording(callLogId: Long, path: String)
    suspend fun updateDuration(callLogId: Long, durationSeconds: Long)
    suspend fun syncSystemCallLogs()
    suspend fun clearMissedFlags()
}

interface ContactRepository {
    fun observeContacts(): Flow<List<Contact>>
    fun searchContacts(query: String): Flow<List<Contact>>
    suspend fun getContactById(id: Long): Contact?
    suspend fun findByNumber(number: String): Contact?
    suspend fun upsertContact(contact: Contact): Long
    suspend fun deleteContact(id: Long)
    suspend fun addTalkTime(number: String, seconds: Long)
    suspend fun syncDeviceContacts()
}

interface BlockedNumberRepository {
    fun observeBlockedNumbers(): Flow<List<BlockedNumber>>
    suspend fun isBlocked(number: String): Boolean
    suspend fun blockNumber(number: String, reason: String = "")
    suspend fun unblockNumber(id: Long)
    suspend fun unblockByNumber(number: String)
}

interface SmsRepository {
    fun observeThreads(): Flow<List<SmsThread>>
    fun observeMessagesForThread(threadId: Long): Flow<List<SmsMessage>>
    suspend fun sendSms(address: String, body: String): Result<SmsMessage>
    suspend fun saveIncomingSms(address: String, body: String, timestamp: Long)
    suspend fun markThreadRead(threadId: Long)
    suspend fun syncSystemSms()
}

interface RecordingRepository {
    fun observeRecordings(): Flow<List<Recording>>
    suspend fun getRecordingById(id: Long): Recording?
    suspend fun insertRecording(recording: Recording): Long
    suspend fun deleteRecording(recording: Recording)
}

interface SpeedDialRepository {
    fun observeSpeedDials(): Flow<List<SpeedDial>>
    suspend fun getSpeedDial(key: Int): SpeedDial?
    suspend fun setSpeedDial(key: Int, number: String)
    suspend fun clearSpeedDial(key: Int)
}

interface SettingsRepository {
    fun observeSetting(key: String): Flow<String?>
    suspend fun getSetting(key: String): String?
    suspend fun setSetting(key: String, value: String)
}
