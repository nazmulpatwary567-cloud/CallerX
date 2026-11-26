package com.example.di

import com.example.data.repository.BlockedNumberRepositoryImpl
import com.example.data.repository.CallLogRepositoryImpl
import com.example.data.repository.ContactRepositoryImpl
import com.example.data.repository.RecordingRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.repository.SipRepositoryImpl
import com.example.data.repository.SmsRepositoryImpl
import com.example.data.repository.SpeedDialRepositoryImpl
import com.example.domain.repository.BlockedNumberRepository
import com.example.domain.repository.CallLogRepository
import com.example.domain.repository.ContactRepository
import com.example.domain.repository.RecordingRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SipRepository
import com.example.domain.repository.SmsRepository
import com.example.domain.repository.SpeedDialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindSipRepository(impl: SipRepositoryImpl): SipRepository

    @Binds @Singleton
    abstract fun bindCallLogRepository(impl: CallLogRepositoryImpl): CallLogRepository

    @Binds @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds @Singleton
    abstract fun bindBlockedNumberRepository(impl: BlockedNumberRepositoryImpl): BlockedNumberRepository

    @Binds @Singleton
    abstract fun bindSmsRepository(impl: SmsRepositoryImpl): SmsRepository

    @Binds @Singleton
    abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository

    @Binds @Singleton
    abstract fun bindSpeedDialRepository(impl: SpeedDialRepositoryImpl): SpeedDialRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
