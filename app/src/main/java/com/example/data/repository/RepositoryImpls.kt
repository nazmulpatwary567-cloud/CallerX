package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.local.BlockedNumberDao
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.local.RecordingDao
import com.example.data.local.RecordingEntity
import com.example.data.local.SettingsDao
import com.example.data.local.SettingsEntity
import com.example.data.local.SipAccountDao
import com.example.data.local.SipAccountEntity
import com.example.data.local.SmsDao
import com.example.data.local.SmsEntity
import com.example.data.local.SpeedDialDao
import com.example.data.local.SpeedDialEntity
import com.example.domain.model.BlockedNumber
import com.example.domain.model.CallLog
import com.example.domain.model.CallType
import com.example.domain.model.Contact
import com.example.domain.model.HistoryFilter
import com.example.domain.model.Recording
import com.example.domain.model.SipAccount
import com.example.domain.model.SmsMessage
import com.example.domain.model.SmsThread
import com.example.domain.model.SpeedDial
import com.example.domain.repository.BlockedNumberRepository
import com.example.domain.repository.CallLogRepository
import com.example.domain.repository.ContactRepository
import com.example.domain.repository.RecordingRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SipRepository
import com.example.domain.repository.SmsRepository
import com.example.domain.repository.SpeedDialRepository
import com.example.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private fun SipAccountEntity.toDomain() = SipAccount(
    id = id,
    username = username,
    password = password,
    domain = domain,
    port = port,
    transport = transport,
    enabled = enabled,
    displayName = displayName
)

private fun SipAccount.toEntity() = SipAccountEntity(
    id = id,
    username = username,
    password = password,
    domain = domain,
    port = port,
    transport = transport,
    enabled = enabled,
    displayName = displayName
)

private fun CallLogEntity.toDomain() = CallLog(
    id = id,
    number = number,
    contactName = contactName,
    type = type,
    route = route,
    simSubscriptionId = simSubscriptionId,
    sipAccountId = sipAccountId,
    routeLabel = routeLabel,
    startTime = startTime,
    duration = duration,
    recordingPath = recordingPath,
    isBlocked = isBlocked
)

private fun CallLog.toEntity() = CallLogEntity(
    id = id,
    number = number,
    contactName = contactName,
    type = type,
    route = route,
    simSubscriptionId = simSubscriptionId,
    sipAccountId = sipAccountId,
    routeLabel = routeLabel,
    startTime = startTime,
    duration = duration,
    recordingPath = recordingPath,
    isBlocked = isBlocked
)

private fun ContactEntity.toDomain() = Contact(
    id = id,
    name = name,
    number = number,
    tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    note = note,
    totalTalkTimeSeconds = totalTalkTimeSeconds,
    photoUri = photoUri
)

private fun Contact.toEntity() = ContactEntity(
    id = id,
    name = name,
    number = number,
    tags = tags.joinToString(","),
    note = note,
    totalTalkTimeSeconds = totalTalkTimeSeconds,
    photoUri = photoUri
)

private fun BlockedNumberEntity.toDomain() = BlockedNumber(
    id = id,
    number = number,
    reason = reason,
    timestamp = timestamp
)

private fun SmsEntity.toDomain() = SmsMessage(
    id = id,
    address = address,
    body = body,
    timestamp = timestamp,
    isRead = isRead,
    threadId = threadId,
    isOutgoing = isOutgoing
)

private fun RecordingEntity.toDomain() = Recording(
    id = id,
    callLogId = callLogId,
    filePath = filePath,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    timestamp = timestamp,
    displayTitle = displayTitle
)

@Singleton
class SipRepositoryImpl @Inject constructor(
    private val dao: SipAccountDao
) : SipRepository {
    override fun observeAccounts(): Flow<List<SipAccount>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllAccounts(): List<SipAccount> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getAccountById(id: Long): SipAccount? =
        dao.getById(id)?.toDomain()

    override suspend fun saveAccount(account: SipAccount): Long =
        dao.upsert(account.toEntity())

    override suspend fun deleteAccount(id: Long) =
        dao.deleteById(id)
}

@Singleton
class CallLogRepositoryImpl @Inject constructor(
    private val dao: CallLogDao,
    @ApplicationContext private val context: Context
) : CallLogRepository {
    override fun observeLogs(filter: HistoryFilter, query: String): Flow<List<CallLog>> {
        val baseFlow = when (filter) {
            HistoryFilter.ALL -> if (query.isBlank()) dao.observeAll() else dao.search(query)
            HistoryFilter.MISSED -> dao.observeByType(CallType.MISSED)
            HistoryFilter.SIP -> dao.observeByRoute(com.example.domain.model.CallRouteType.SIP)
            HistoryFilter.SIM -> dao.observeSimLogs()
        }
        return baseFlow.map { list ->
            val mapped = list.map { it.toDomain() }
            if (query.isBlank() || filter == HistoryFilter.ALL) mapped
            else mapped.filter {
                it.number.contains(query, ignoreCase = true) ||
                    (it.contactName?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    override fun observeMissedCount(): Flow<Int> = dao.observeMissedCount()

    override suspend fun insertLog(log: CallLog): Long = dao.insert(log.toEntity())

    override suspend fun updateLog(log: CallLog) = dao.update(log.toEntity())

    override suspend fun deleteLog(id: Long) = dao.deleteById(id)

    override suspend fun attachRecording(callLogId: Long, path: String) =
        dao.updateRecordingPath(callLogId, path)

    override suspend fun updateDuration(callLogId: Long, durationSeconds: Long) =
        dao.updateDuration(callLogId, durationSeconds)

    override suspend fun syncSystemCallLogs() {
        withContext(Dispatchers.IO) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) return@withContext

            val existingEntries = dao.getAllSnapshot()
                .map { Triple(PhoneUtils.normalizeNumber(it.number), it.type, it.startTime) }
                .toMutableList()

            val projection = arrayOf(
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.CACHED_NAME,
                android.provider.CallLog.Calls.TYPE,
                android.provider.CallLog.Calls.DATE,
                android.provider.CallLog.Calls.DURATION
            )
            runCatching {
                context.contentResolver.query(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${android.provider.CallLog.Calls.DATE} DESC LIMIT 100"
                )?.use { cursor ->
                    val numIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                    val nameIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                    val typeIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE)
                    val dateIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE)
                    val durIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.DURATION)

                    while (cursor.moveToNext()) {
                        val rawNum = cursor.getString(numIdx).orEmpty()
                        val normNum = PhoneUtils.normalizeNumber(rawNum).ifBlank { "Unknown" }
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                        val sysType = cursor.getInt(typeIdx)
                        val startMs = cursor.getLong(dateIdx)
                        val durSec = cursor.getLong(durIdx)

                        val mappedType = when (sysType) {
                            android.provider.CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                            android.provider.CallLog.Calls.MISSED_TYPE,
                            android.provider.CallLog.Calls.REJECTED_TYPE -> CallType.MISSED
                            else -> CallType.OUTGOING
                        }
                        val alreadyPresent = existingEntries.any { (exNum, exType, exTime) ->
                            exNum == normNum && exType == mappedType && kotlin.math.abs(exTime - startMs) < 5_000L
                        }
                        if (!alreadyPresent) {
                            existingEntries.add(Triple(normNum, mappedType, startMs))
                            dao.insert(
                                CallLogEntity(
                                    number = normNum,
                                    contactName = name,
                                    type = mappedType,
                                    route = com.example.domain.model.CallRouteType.SIM1,
                                    routeLabel = "SIM",
                                    startTime = startMs,
                                    duration = durSec
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun clearMissedFlags() {
        // Missed badge reflects missed entries
    }
}

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao,
    @ApplicationContext private val context: Context
) : ContactRepository {
    override fun observeContacts(): Flow<List<Contact>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun searchContacts(query: String): Flow<List<Contact>> =
        dao.observeAll().map { list ->
            val domain = list.map { it.toDomain() }
            if (query.isBlank()) domain else domain.filter { PhoneUtils.matchesT9(it, query) }
        }

    override suspend fun getContactById(id: Long): Contact? =
        dao.getById(id)?.toDomain()

    override suspend fun findByNumber(number: String): Contact? {
        val exact = dao.findExactByNumber(number)?.toDomain()
        if (exact != null) return exact
        return dao.getAllSnapshot()
            .map { it.toDomain() }
            .firstOrNull { PhoneUtils.numbersMatch(it.number, number) }
    }

    override suspend fun upsertContact(contact: Contact): Long =
        dao.upsert(contact.toEntity())

    override suspend fun deleteContact(id: Long) =
        dao.deleteById(id)

    override suspend fun addTalkTime(number: String, seconds: Long) {
        if (seconds <= 0) return
        val existing = findByNumber(number) ?: return
        dao.upsert(existing.copy(totalTalkTimeSeconds = existing.totalTalkTimeSeconds + seconds).toEntity())
    }

    override suspend fun syncDeviceContacts() {
        withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@withContext

        val existingNumbers = dao.getAllSnapshot().map { PhoneUtils.normalizeNumber(it.number) }.toMutableSet()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx)?.trim().orEmpty()
                val rawNum = cursor.getString(numIdx)?.trim().orEmpty()
                val norm = PhoneUtils.normalizeNumber(rawNum)
                val photo = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                if (name.isNotEmpty() && norm.isNotEmpty() && existingNumbers.add(norm)) {
                    dao.upsert(
                        ContactEntity(
                            name = name,
                            number = norm,
                            photoUri = photo
                        )
                    )
                }
            }
        }
        }
    }
}

@Singleton
class BlockedNumberRepositoryImpl @Inject constructor(
    private val dao: BlockedNumberDao
) : BlockedNumberRepository {
    override fun observeBlockedNumbers(): Flow<List<BlockedNumber>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun isBlocked(number: String): Boolean {
        return dao.getAllSnapshot().any { PhoneUtils.numbersMatch(it.number, number) }
    }

    override suspend fun blockNumber(number: String, reason: String) {
        val normalized = PhoneUtils.normalizeNumber(number)
        if (normalized.isBlank()) return
        if (!isBlocked(normalized)) {
            dao.insert(BlockedNumberEntity(number = normalized, reason = reason))
        }
    }

    override suspend fun unblockNumber(id: Long) = dao.deleteById(id)

    override suspend fun unblockByNumber(number: String) {
        val target = dao.getAllSnapshot().firstOrNull { PhoneUtils.numbersMatch(it.number, number) }
        if (target != null) dao.deleteById(target.id)
    }
}

@Singleton
class SmsRepositoryImpl @Inject constructor(
    private val smsDao: SmsDao,
    private val contactDao: ContactDao,
    @ApplicationContext private val context: Context
) : SmsRepository {

    override fun observeThreads(): Flow<List<SmsThread>> {
        return combine(smsDao.observeAll(), contactDao.observeAll()) { messages, contacts ->
            val domainContacts = contacts.map { it.toDomain() }
            messages.groupBy { it.threadId }.map { (threadId, threadMsgs) ->
                val sorted = threadMsgs.sortedByDescending { it.timestamp }
                val latest = sorted.first()
                val matched = domainContacts.firstOrNull {
                    PhoneUtils.numbersMatch(it.number, latest.address)
                }
                SmsThread(
                    threadId = threadId,
                    address = latest.address,
                    contactName = matched?.name,
                    lastMessage = latest.body,
                    timestamp = latest.timestamp,
                    unreadCount = sorted.count { !it.isRead && !it.isOutgoing }
                )
            }.sortedByDescending { it.timestamp }
        }
    }

    override fun observeMessagesForThread(threadId: Long): Flow<List<SmsMessage>> =
        smsDao.observeThread(threadId).map { list -> list.map { it.toDomain() } }

    override suspend fun sendSms(address: String, body: String): Result<SmsMessage> =
        withContext(Dispatchers.IO) {
            val cleanAddress = PhoneUtils.normalizeNumber(address)
            if (cleanAddress.isBlank() || body.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Recipient and message are required"))
            }
            val hasSendPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasSendPerm) {
                try {
                    val smsManager = context.getSystemService(SmsManager::class.java)
                        ?: @Suppress("DEPRECATION") SmsManager.getDefault()
                    val parts = smsManager.divideMessage(body)
                    if (parts.size > 1) {
                        smsManager.sendMultipartTextMessage(cleanAddress, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(cleanAddress, null, body, null, null)
                    }
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }
            } else {
                return@withContext Result.failure(SecurityException("SEND_SMS permission not granted"))
            }

            val threadId = computeThreadId(cleanAddress)
            val entity = SmsEntity(
                address = cleanAddress,
                body = body,
                timestamp = System.currentTimeMillis(),
                isRead = true,
                threadId = threadId,
                isOutgoing = true
            )
            val id = smsDao.insert(entity)
            Result.success(entity.copy(id = id).toDomain())
        }

    override suspend fun saveIncomingSms(address: String, body: String, timestamp: Long) {
        val clean = PhoneUtils.normalizeNumber(address)
        val threadId = computeThreadId(clean)
        smsDao.insert(
            SmsEntity(
                address = clean,
                body = body,
                timestamp = timestamp,
                isRead = false,
                threadId = threadId,
                isOutgoing = false
            )
        )
    }

    override suspend fun markThreadRead(threadId: Long) = smsDao.markThreadRead(threadId)

    override suspend fun syncSystemSms() {
        withContext(Dispatchers.IO) {
        val hasReadSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasReadSms) return@withContext

        val existingKeys = smsDao.getAllSnapshot()
            .map { "${it.address}|${it.timestamp}|${it.body.hashCode()}" }
            .toHashSet()

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.TYPE
        )
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 100"
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                val readIdx = cursor.getColumnIndex(Telephony.Sms.READ)
                val threadIdx = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                val typeIdx = cursor.getColumnIndex(Telephony.Sms.TYPE)

                while (cursor.moveToNext()) {
                    val address = PhoneUtils.normalizeNumber(cursor.getString(addrIdx).orEmpty())
                    val body = cursor.getString(bodyIdx).orEmpty()
                    val date = cursor.getLong(dateIdx)
                    val isRead = cursor.getInt(readIdx) == 1
                    val threadId = cursor.getLong(threadIdx).takeIf { it > 0 } ?: computeThreadId(address)
                    val isOutgoing = cursor.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_SENT
                    val key = "$address|$date|${body.hashCode()}"
                    if (address.isNotEmpty() && existingKeys.add(key)) {
                        smsDao.insert(
                            SmsEntity(
                                address = address,
                                body = body,
                                timestamp = date,
                                isRead = isRead,
                                threadId = threadId,
                                isOutgoing = isOutgoing
                            )
                        )
                    }
                }
            }
        }
        }
    }

    private fun computeThreadId(address: String): Long {
        val digits = address.filter { it.isDigit() }.takeLast(10)
        return (digits.ifEmpty { address }).hashCode().toLong() and 0x7FFFFFFFL
    }
}

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val dao: RecordingDao
) : RecordingRepository {
    override fun observeRecordings(): Flow<List<Recording>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getRecordingById(id: Long): Recording? =
        dao.getById(id)?.toDomain()

    override suspend fun insertRecording(recording: Recording): Long =
        dao.insert(
            RecordingEntity(
                id = recording.id,
                callLogId = recording.callLogId,
                filePath = recording.filePath,
                durationMs = recording.durationMs,
                sizeBytes = recording.sizeBytes,
                timestamp = recording.timestamp,
                displayTitle = recording.displayTitle
            )
        )

    override suspend fun deleteRecording(recording: Recording) {
        runCatching { File(recording.filePath).delete() }
        dao.delete(
            RecordingEntity(
                id = recording.id,
                callLogId = recording.callLogId,
                filePath = recording.filePath,
                durationMs = recording.durationMs,
                sizeBytes = recording.sizeBytes,
                timestamp = recording.timestamp,
                displayTitle = recording.displayTitle
            )
        )
    }
}

@Singleton
class SpeedDialRepositoryImpl @Inject constructor(
    private val dao: SpeedDialDao
) : SpeedDialRepository {
    override fun observeSpeedDials(): Flow<List<SpeedDial>> =
        dao.observeAll().map { list -> list.map { SpeedDial(it.key, it.number) } }

    override suspend fun getSpeedDial(key: Int): SpeedDial? =
        dao.getByKey(key)?.let { SpeedDial(it.key, it.number) }

    override suspend fun setSpeedDial(key: Int, number: String) {
        if (key in 2..9) {
            dao.upsert(SpeedDialEntity(key = key, number = PhoneUtils.normalizeNumber(number)))
        }
    }

    override suspend fun clearSpeedDial(key: Int) = dao.deleteByKey(key)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao
) : SettingsRepository {
    override fun observeSetting(key: String): Flow<String?> = dao.observeValue(key)
    override suspend fun getSetting(key: String): String? = dao.getValue(key)
    override suspend fun setSetting(key: String, value: String) =
        dao.put(SettingsEntity(key = key, value = value))
}
