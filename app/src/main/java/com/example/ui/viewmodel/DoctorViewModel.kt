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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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

    private val db = AppDatabase.getInstance(application)
    private val repository = DoctorRepository(db.doctorDao(), application)

    val currentDayArabic: String = Doctor.getCurrentDayArabic()

    // Raw query for immediate UI response in the search field
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Debounced query with 120ms window to prevent jank and redundant allocations while typing rapidly
    @OptIn(FlowPreview::class)
    private val debouncedQuery = _searchQuery
        .debounce(120L)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

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

    // Dynamic lists for pharmacies and labs backed by Firebase Realtime Database
    val pharmacies: StateFlow<List<Pharmacy>> = repository.pharmaciesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DefaultData.initialPharmacies
        )

    val laboratories: StateFlow<List<Laboratory>> = repository.laboratoriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DefaultData.initialLaboratories
        )

    private val _sponsorBanner = MutableStateFlow(com.example.data.model.SponsorBanner.defaultBanner)
    val sponsorBanner: StateFlow<com.example.data.model.SponsorBanner> = _sponsorBanner

    private val _isPharmaciesEnabled = MutableStateFlow(false)
    val isPharmaciesEnabled: StateFlow<Boolean> = _isPharmaciesEnabled

    private val _isLaboratoriesEnabled = MutableStateFlow(false)
    val isLaboratoriesEnabled: StateFlow<Boolean> = _isLaboratoriesEnabled

    init {
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
        debouncedQuery,
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

            // Always show all doctors in the directory (do not filter out non-working doctors by day).
            // Day selection dynamically floats working doctors to the top via sorting.
            matchesQuery && matchesSpecialty
        }.sortedWith { d1, d2 ->
            // Dynamic priority sorting based on target day:
            // Priority 1: Available on target day
            // Priority 2: If viewing today, Open Now
            // Priority 3: orderIndex / name
            val d1Avail = d1.isAvailableOnDay(activeTargetDay)
            val d2Avail = d2.isAvailableOnDay(activeTargetDay)

            val isToday = (day == null || day == currentDayArabic)
            val d1Open = if (isToday) d1.isOpenNow(calendar) else false
            val d2Open = if (isToday) d2.isOpenNow(calendar) else false

            val d1Score = (if (d1Avail) 30 else 0) + (if (d1Open) 15 else 0)
            val d2Score = (if (d2Avail) 30 else 0) + (if (d2Open) 15 else 0)

            when {
                d1Score != d2Score -> d2Score.compareTo(d1Score) // higher score (available/open) first
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

    fun refreshDoctors() {
        viewModelScope.launch {
            repository.ensureDefaultDataLoaded()
        }
    }

    fun addPharmacy(pharmacy: Pharmacy) {
        val current = pharmacies.value.toMutableList()
        val newId = if (current.isEmpty()) 1L else current.maxOf { it.id } + 1
        current.add(pharmacy.copy(id = newId))
        repository.savePharmacies(current)
    }

    fun updatePharmacy(pharmacy: Pharmacy) {
        val current = pharmacies.value.toMutableList()
        val index = current.indexOfFirst { it.id == pharmacy.id }
        if (index >= 0) {
            current[index] = pharmacy
            repository.savePharmacies(current)
        }
    }

    fun deletePharmacy(pharmacy: Pharmacy) {
        val current = pharmacies.value.toMutableList()
        current.removeAll { it.id == pharmacy.id }
        repository.savePharmacies(current)
    }

    fun addLaboratory(laboratory: Laboratory) {
        val current = laboratories.value.toMutableList()
        val newId = if (current.isEmpty()) 1L else current.maxOf { it.id } + 1
        current.add(laboratory.copy(id = newId))
        repository.saveLaboratories(current)
    }

    fun updateLaboratory(laboratory: Laboratory) {
        val current = laboratories.value.toMutableList()
        val index = current.indexOfFirst { it.id == laboratory.id }
        if (index >= 0) {
            current[index] = laboratory
            repository.saveLaboratories(current)
        }
    }

    fun deleteLaboratory(laboratory: Laboratory) {
        val current = laboratories.value.toMutableList()
        current.removeAll { it.id == laboratory.id }
        repository.saveLaboratories(current)
    }
}
