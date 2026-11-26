package com.example.backup

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.example.data.local.BlockedNumberDao
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.local.RecordingDao
import com.example.data.local.RecordingEntity
import com.example.data.local.SecurePrefsManager
import com.example.data.local.SettingsDao
import com.example.data.local.SettingsEntity
import com.example.data.local.SipAccountDao
import com.example.data.local.SipAccountEntity
import com.example.data.local.SmsDao
import com.example.data.local.SmsEntity
import com.example.data.local.SpeedDialDao
import com.example.data.local.SpeedDialEntity
import com.example.data.remote.GoogleDriveUploadApi
import com.example.domain.model.BackupProvider
import com.example.domain.model.BlockedNumber
import com.example.domain.model.CallLog
import com.example.domain.model.Contact
import com.example.domain.model.Recording
import com.example.domain.model.SipAccount
import com.example.domain.model.SmsMessage
import com.example.domain.model.SpeedDial
import com.example.util.CryptoUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val sipAccounts: List<SipAccount> = emptyList(),
    val callLogs: List<CallLog> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val blockedNumbers: List<BlockedNumber> = emptyList(),
    val smsMessages: List<SmsMessage> = emptyList(),
    val recordings: List<Recording> = emptyList(),
    val speedDials: List<SpeedDial> = emptyList(),
    val settings: Map<String, String> = emptyMap()
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sipAccountDao: SipAccountDao,
    private val callLogDao: CallLogDao,
    private val contactDao: ContactDao,
    private val blockedNumberDao: BlockedNumberDao,
    private val smsDao: SmsDao,
    private val recordingDao: RecordingDao,
    private val speedDialDao: SpeedDialDao,
    private val settingsDao: SettingsDao,
    private val securePrefsManager: SecurePrefsManager,
    private val googleDriveUploadApi: GoogleDriveUploadApi
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    suspend fun createBackup(password: String): File = withContext(Dispatchers.IO) {
        require(password.length >= 6) { "Backup password must be at least 6 characters" }

        val payload = BackupPayload(
            sipAccounts = sipAccountDao.getAll().map {
                SipAccount(it.id, it.username, it.password, it.domain, it.port, it.transport, it.enabled, it.displayName)
            },
            callLogs = callLogDao.getAllSnapshot().map {
                CallLog(it.id, it.number, it.contactName, it.type, it.route, it.simSubscriptionId, it.sipAccountId, it.routeLabel, it.startTime, it.duration, it.recordingPath, it.isBlocked)
            },
            contacts = contactDao.getAllSnapshot().map {
                Contact(
                    it.id,
                    it.name,
                    it.number,
                    it.tags.split(",").map { t -> t.trim() }.filter { t -> t.isNotEmpty() },
                    it.note,
                    it.totalTalkTimeSeconds,
                    it.photoUri
                )
            },
            blockedNumbers = blockedNumberDao.getAllSnapshot().map {
                BlockedNumber(it.id, it.number, it.reason, it.timestamp)
            },
            smsMessages = smsDao.getAllSnapshot().map {
                SmsMessage(it.id, it.address, it.body, it.timestamp, it.isRead, it.threadId, it.isOutgoing)
            },
            recordings = recordingDao.getAllSnapshot().map {
                Recording(it.id, it.callLogId, it.filePath, it.durationMs, it.sizeBytes, it.timestamp, it.displayTitle)
            },
            speedDials = speedDialDao.getAllSnapshot().map {
                SpeedDial(it.key, it.number)
            },
            settings = settingsDao.getAllSnapshot().associate { it.key to it.value }
        )

        val serialized = json.encodeToString(BackupPayload.serializer(), payload).toByteArray(Charsets.UTF_8)
        val encrypted = CryptoUtils.encryptAes256Gcm(serialized, password)

        val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(backupDir, "callerx_backup_${System.currentTimeMillis()}.callerx")
        file.writeBytes(encrypted)
        securePrefsManager.setLastBackupTimestamp(System.currentTimeMillis())
        file
    }

    suspend fun restoreBackup(file: File, password: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = file.readBytes()
            val decrypted = CryptoUtils.decryptAes256Gcm(raw, password)
            val payload = json.decodeFromString(BackupPayload.serializer(), String(decrypted, Charsets.UTF_8))

            sipAccountDao.deleteAll()
            callLogDao.deleteAll()
            contactDao.deleteAll()
            blockedNumberDao.deleteAll()
            smsDao.deleteAll()
            recordingDao.deleteAll()
            speedDialDao.deleteAll()
            settingsDao.deleteAll()

            payload.sipAccounts.forEach {
                sipAccountDao.upsert(
                    SipAccountEntity(it.id, it.username, it.password, it.domain, it.port, it.transport, it.enabled, it.displayName)
                )
            }
            payload.callLogs.forEach {
                callLogDao.insert(
                    CallLogEntity(it.id, it.number, it.contactName, it.type, it.route, it.simSubscriptionId, it.sipAccountId, it.routeLabel, it.startTime, it.duration, it.recordingPath, it.isBlocked)
                )
            }
            payload.contacts.forEach {
                contactDao.upsert(
                    ContactEntity(it.id, it.name, it.number, it.tags.joinToString(","), it.note, it.totalTalkTimeSeconds, it.photoUri)
                )
            }
            payload.blockedNumbers.forEach {
                blockedNumberDao.insert(BlockedNumberEntity(it.id, it.number, it.reason, it.timestamp))
            }
            payload.smsMessages.forEach {
                smsDao.insert(SmsEntity(it.id, it.address, it.body, it.timestamp, it.isRead, it.threadId, it.isOutgoing))
            }
            payload.recordings.forEach {
                recordingDao.insert(RecordingEntity(it.id, it.callLogId, it.filePath, it.durationMs, it.sizeBytes, it.timestamp, it.displayTitle))
            }
            payload.speedDials.forEach {
                speedDialDao.upsert(SpeedDialEntity(it.key, it.number))
            }
            payload.settings.forEach { (k, v) ->
                settingsDao.put(SettingsEntity(k, v))
            }

            payload.sipAccounts.size + payload.contacts.size + payload.callLogs.size
        }
    }

    suspend fun uploadToConfiguredProvider(backupFile: File): Result<String> = withContext(Dispatchers.IO) {
        when (securePrefsManager.getBackupProvider()) {
            BackupProvider.LOCAL -> Result.success("Saved locally: ${backupFile.name}")
            BackupProvider.GOOGLE_DRIVE -> uploadToGoogleDrive(backupFile)
            BackupProvider.DROPBOX -> uploadToDropbox(backupFile)
        }
    }

    suspend fun uploadToGoogleDrive(backupFile: File): Result<String> = withContext(Dispatchers.IO) {
        val token = securePrefsManager.getGoogleDriveAccessToken()
            ?: return@withContext Result.failure(IllegalStateException("Google Drive OAuth token not configured"))
        runCatching {
            val metadataJson = """{"name":"${backupFile.name}","parents":["appDataFolder"]}"""
            val metadataBody = metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            val fileBody = MultipartBody.Part.createFormData(
                "file",
                backupFile.name,
                backupFile.asRequestBody("application/octet-stream".toMediaType())
            )
            val response = googleDriveUploadApi.uploadToAppDataFolder(
                bearerToken = "Bearer $token",
                metadata = metadataBody,
                file = fileBody
            )
            if (!response.isSuccessful) {
                error("Google Drive upload failed (${response.code()})")
            }
            "Uploaded ${backupFile.name} to Google Drive appDataFolder"
        }
    }

    suspend fun uploadToDropbox(backupFile: File): Result<String> = withContext(Dispatchers.IO) {
        val token = securePrefsManager.getDropboxAccessToken()
            ?: return@withContext Result.failure(IllegalStateException("Dropbox access token not configured"))
        runCatching {
            val config = DbxRequestConfig.newBuilder("CallerX/1.0").build()
            val client = DbxClientV2(config, token)
            val bytes = backupFile.readBytes()
            ByteArrayInputStream(bytes).use { input ->
                client.files()
                    .uploadBuilder("/${backupFile.name}")
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(input)
            }
            "Uploaded ${backupFile.name} to Dropbox"
        }
    }
}
