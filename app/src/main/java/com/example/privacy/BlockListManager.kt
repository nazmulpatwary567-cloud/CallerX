package com.example.privacy

import com.example.domain.model.BlockedNumber
import com.example.domain.repository.BlockedNumberRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.EvaluateIncomingCallUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockListManager @Inject constructor(
    private val blockedNumberRepository: BlockedNumberRepository,
    private val settingsRepository: SettingsRepository
) {
    fun observeBlockedNumbers(): Flow<List<BlockedNumber>> =
        blockedNumberRepository.observeBlockedNumbers()

    fun observeBlockUnknownNumbers(): Flow<String?> =
        settingsRepository.observeSetting(EvaluateIncomingCallUseCase.KEY_BLOCK_UNKNOWN)

    suspend fun setBlockUnknownNumbers(enabled: Boolean) {
        settingsRepository.setSetting(
            EvaluateIncomingCallUseCase.KEY_BLOCK_UNKNOWN,
            enabled.toString()
        )
    }

    suspend fun addBlockedNumber(number: String, reason: String = "Manually blocked") {
        blockedNumberRepository.blockNumber(number, reason)
    }

    suspend fun removeBlockedNumber(id: Long) {
        blockedNumberRepository.unblockNumber(id)
    }

    suspend fun isBlocked(number: String): Boolean =
        blockedNumberRepository.isBlocked(number)
}
