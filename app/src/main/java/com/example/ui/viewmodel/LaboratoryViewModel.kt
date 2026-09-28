package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Laboratory
import com.example.data.repository.LaboratoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LaboratoryViewModel(
    application: Application,
    private val repository: LaboratoryRepository = LaboratoryRepository.getInstance(application)
) : AndroidViewModel(application) {

    val laboratories: StateFlow<List<Laboratory>> = repository.laboratoriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.laboratoriesFlow.value
        )

    fun addLaboratory(laboratory: Laboratory): Long {
        return repository.addLaboratory(laboratory)
    }

    fun updateLaboratory(laboratory: Laboratory) {
        viewModelScope.launch {
            repository.updateLaboratory(laboratory)
        }
    }

    fun deleteLaboratory(laboratoryId: Long) {
        viewModelScope.launch {
            repository.deleteLaboratory(laboratoryId)
        }
    }

    fun deleteLaboratory(laboratory: Laboratory) {
        deleteLaboratory(laboratory.id)
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }
}
