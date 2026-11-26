package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.BlockedNumberDao
import com.example.data.local.CallLogDao
import com.example.data.local.CallerXDatabase
import com.example.data.local.ContactDao
import com.example.data.local.RecordingDao
import com.example.data.local.SettingsDao
import com.example.data.local.SipAccountDao
import com.example.data.local.SmsDao
import com.example.data.local.SpeedDialDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CallerXDatabase {
        return Room.databaseBuilder(
            context,
            CallerXDatabase::class.java,
            "callerx_database.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides fun provideSipAccountDao(db: CallerXDatabase): SipAccountDao = db.sipAccountDao()
    @Provides fun provideCallLogDao(db: CallerXDatabase): CallLogDao = db.callLogDao()
    @Provides fun provideContactDao(db: CallerXDatabase): ContactDao = db.contactDao()
    @Provides fun provideBlockedNumberDao(db: CallerXDatabase): BlockedNumberDao = db.blockedNumberDao()
    @Provides fun provideSmsDao(db: CallerXDatabase): SmsDao = db.smsDao()
    @Provides fun provideRecordingDao(db: CallerXDatabase): RecordingDao = db.recordingDao()
    @Provides fun provideSpeedDialDao(db: CallerXDatabase): SpeedDialDao = db.speedDialDao()
    @Provides fun provideSettingsDao(db: CallerXDatabase): SettingsDao = db.settingsDao()
}
