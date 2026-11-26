package com.example.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Recording
import com.example.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    val recordings: StateFlow<List<Recording>> = recordingRepository.observeRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedRecording = MutableStateFlow<Recording?>(null)
    val selectedRecording: StateFlow<Recording?> = _selectedRecording.asStateFlow()

    fun loadRecording(id: Long) {
        viewModelScope.launch {
            _selectedRecording.value = recordingRepository.getRecordingById(id)
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(recording)
        }
    }
}
