package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultData
import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy
import com.example.data.repository.DoctorRepository
import com.example.util.ArabicSearchUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppTab {
    DOCTORS,
    PHARMACIES,
    LABORATORIES,
    ADMIN
}

class DoctorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DoctorRepository

    val currentDayArabic: String = Doctor.getCurrentDayArabic()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Selected day filter: null means all days, or specific day like "السبت"
    private val _selectedDay = MutableStateFlow<String?>(currentDayArabic)
    val selectedDay: StateFlow<String?> = _selectedDay

    // Specialty filter: null means all
    private val _selectedSpecialty = MutableStateFlow<String?>(null)
    val selectedSpecialty: StateFlow<String?> = _selectedSpecialty

    // Current Active Tab
    private val _currentTab = MutableStateFlow(AppTab.DOCTORS)
    val currentTab: StateFlow<AppTab> = _currentTab

    // Admin authentication state
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated

    // Static list for pharmacies and labs (seeded from DefaultData)
    val pharmacies: List<Pharmacy> = DefaultData.initialPharmacies
    val laboratories: List<Laboratory> = DefaultData.initialLaboratories

    private val _sponsorBanner = MutableStateFlow(com.example.data.model.SponsorBanner.defaultBanner)
    val sponsorBanner: StateFlow<com.example.data.model.SponsorBanner> = _sponsorBanner

    private val _isPharmaciesEnabled = MutableStateFlow(false)
    val isPharmaciesEnabled: StateFlow<Boolean> = _isPharmaciesEnabled

    private val _isLaboratoriesEnabled = MutableStateFlow(false)
    val isLaboratoriesEnabled: StateFlow<Boolean> = _isLaboratoriesEnabled

    init {
        val db = AppDatabase.getInstance(application)
        repository = DoctorRepository(db.doctorDao(), application)
        _sponsorBanner.value = repository.getSponsorBanner()
        _isPharmaciesEnabled.value = repository.isPharmaciesEnabled()
        _isLaboratoriesEnabled.value = repository.isLaboratoriesEnabled()
        viewModelScope.launch {
            repository.ensureDefaultDataLoaded()
        }
    }

    val doctors: StateFlow<List<Doctor>> = repository.allDoctors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DefaultData.initialDoctors
        )

    val filteredDoctors: StateFlow<List<Doctor>> = combine(
        doctors,
        _searchQuery,
        _selectedDay,
        _selectedSpecialty
    ) { docList, query, day, specialty ->
        val trimmedQuery = query.trim()
        val isSearching = trimmedQuery.isNotEmpty()
        val calendar = Calendar.getInstance()
        val activeTargetDay = day ?: currentDayArabic

        docList.filter { doc ->
            // Live real-time search across doctor names, sections/specialties, landmarks, and details
            val matchesQuery = if (!isSearching) {
                true
            } else {
                ArabicSearchUtils.matchesDoctor(doc, trimmedQuery)
            }

            val matchesSpecialty = if (specialty == null) {
                true
            } else {
                doc.specialty.equals(specialty, ignoreCase = true)
            }

            // During live search, search across the entire directory so users immediately find doctors,
            // with today's doctors dynamically prioritized at the top of the results.
            val matchesDayFilter = if (isSearching || day == null) {
                true
            } else {
                doc.days.contains(day)
            }

            matchesQuery && matchesSpecialty && matchesDayFilter
        }.sortedWith { d1, d2 ->
            // Dynamic sorting:
            // Priority 1: Available on target day and Open Now
            // Priority 2: Available on target day
            // Priority 3: Other days
            val d1Avail = d1.isAvailableOnDay(activeTargetDay)
            val d2Avail = d2.isAvailableOnDay(activeTargetDay)

            val d1Open = d1.isOpenNow(calendar)
            val d2Open = d2.isOpenNow(calendar)

            val d1Score = (if (d1Avail) 10 else 0) + (if (d1Open) 5 else 0)
            val d2Score = (if (d2Avail) 10 else 0) + (if (d2Open) 5 else 0)

            when {
                d1Score != d2Score -> d2Score.compareTo(d1Score) // higher score first
                d1.orderIndex != d2.orderIndex -> d1.orderIndex.compareTo(d2.orderIndex)
                else -> d1.name.compareTo(d2.name)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onDaySelected(day: String?) {
        _selectedDay.value = day
    }

    fun onSpecialtySelected(specialty: String?) {
        _selectedSpecialty.value = specialty
    }

    fun setTab(tab: AppTab) {
        if (tab == AppTab.PHARMACIES && !_isPharmaciesEnabled.value) {
            return
        }
        if (tab == AppTab.LABORATORIES && !_isLaboratoriesEnabled.value) {
            return
        }
        _currentTab.value = tab
    }

    fun verifyPin(pin: String): Boolean {
        val valid = repository.verifyPin(pin)
        if (valid) {
            _isAdminAuthenticated.value = true
        }
        return valid
    }

    fun logoutAdmin() {
        _isAdminAuthenticated.value = false
        if (_currentTab.value == AppTab.ADMIN) {
            _currentTab.value = AppTab.DOCTORS
        }
    }

    fun updateAdminPin(newPin: String) {
        repository.setAdminPin(newPin)
    }

    fun addDoctor(doctor: Doctor) {
        viewModelScope.launch {
            repository.insertDoctor(doctor)
        }
    }

    fun updateDoctor(doctor: Doctor) {
        viewModelScope.launch {
            repository.updateDoctor(doctor)
        }
    }

    fun deleteDoctor(doctor: Doctor) {
        viewModelScope.launch {
            repository.deleteDoctor(doctor)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaultData()
        }
    }

    suspend fun exportJson(): String {
        return repository.exportDatabaseToJson()
    }

    suspend fun importJson(json: String): Result<Int> {
        val res = repository.importDatabaseFromJson(json)
        if (res.isSuccess) {
            _sponsorBanner.value = repository.getSponsorBanner()
            _isPharmaciesEnabled.value = repository.isPharmaciesEnabled()
            _isLaboratoriesEnabled.value = repository.isLaboratoriesEnabled()
        }
        return res
    }

    fun updateSponsorBanner(banner: com.example.data.model.SponsorBanner) {
        repository.saveSponsorBanner(banner)
        _sponsorBanner.value = banner
    }

    fun setPharmaciesEnabled(enabled: Boolean) {
        repository.setPharmaciesEnabled(enabled)
        _isPharmaciesEnabled.value = enabled
        if (!enabled && _currentTab.value == AppTab.PHARMACIES) {
            _currentTab.value = AppTab.DOCTORS
        }
    }

    fun setLaboratoriesEnabled(enabled: Boolean) {
        repository.setLaboratoriesEnabled(enabled)
        _isLaboratoriesEnabled.value = enabled
        if (!enabled && _currentTab.value == AppTab.LABORATORIES) {
            _currentTab.value = AppTab.DOCTORS
        }
    }
}
