package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Pharmacy
import com.example.data.repository.PharmacyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PharmacyViewModel(
    application: Application,
    private val repository: PharmacyRepository = PharmacyRepository.getInstance(application)
) : AndroidViewModel(application) {

    val pharmacies: StateFlow<List<Pharmacy>> = repository.pharmaciesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.pharmaciesFlow.value
        )

    fun addPharmacy(pharmacy: Pharmacy): Long {
        return repository.addPharmacy(pharmacy)
    }

    fun updatePharmacy(pharmacy: Pharmacy) {
        viewModelScope.launch {
            repository.updatePharmacy(pharmacy)
        }
    }

    fun deletePharmacy(pharmacyId: Long) {
        viewModelScope.launch {
            repository.deletePharmacy(pharmacyId)
        }
    }

    fun deletePharmacy(pharmacy: Pharmacy) {
        deletePharmacy(pharmacy.id)
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }
}
