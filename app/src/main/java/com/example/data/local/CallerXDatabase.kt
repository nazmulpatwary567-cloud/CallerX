package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SipAccountEntity::class,
        CallLogEntity::class,
        ContactEntity::class,
        BlockedNumberEntity::class,
        SmsEntity::class,
        RecordingEntity::class,
        SpeedDialEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CallerXDatabase : RoomDatabase() {
    abstract fun sipAccountDao(): SipAccountDao
    abstract fun callLogDao(): CallLogDao
    abstract fun contactDao(): ContactDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun smsDao(): SmsDao
    abstract fun recordingDao(): RecordingDao
    abstract fun speedDialDao(): SpeedDialDao
    abstract fun settingsDao(): SettingsDao
}
