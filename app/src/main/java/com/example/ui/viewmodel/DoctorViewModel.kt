package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy
import com.example.data.model.SponsorBanner
import com.example.data.repository.DoctorRepository
import com.example.util.ArabicSearchUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppTab {
    DOCTORS,
    PHARMACIES,
    LABORATORIES
}

class DoctorViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "DoctorViewModel"
    }

    private val db: AppDatabase? = try {
        AppDatabase.getInstance(application)
    } catch (e: Exception) {
        Log.e(TAG, "Error initializing database: ${e.message}")
        null
    }

    private val repository: DoctorRepository? = try {
        db?.let { DoctorRepository(it.doctorDao(), application) }
    } catch (e: Exception) {
        Log.e(TAG, "Error initializing repository: ${e.message}")
        null
    }

    val currentDayArabic: String = try {
        Doctor.getCurrentDayArabic()
    } catch (e: Exception) {
        "السبت"
    }

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

    // UI readiness state for startup safety
    private val _isReady = MutableStateFlow(true)
    val isReady: StateFlow<Boolean> = _isReady

    // Dynamic lists for pharmacies and labs backed by Firebase Realtime Database
    val pharmacies: StateFlow<List<Pharmacy>> = (repository?.getPharmaciesFlow() ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val laboratories: StateFlow<List<Laboratory>> = (repository?.getLaboratoriesFlow() ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sponsorBanner = MutableStateFlow(SponsorBanner.defaultBanner)
    val sponsorBanner: StateFlow<SponsorBanner> = _sponsorBanner

    init {
        try {
            repository?.let { repo ->
                _sponsorBanner.value = repo.getSponsorBanner()
                repo.reconnectRealtime()
            }
        } catch (e: Exception) {
            Log.e(TAG, "ViewModel init exception handled safely: ${e.message}")
        }
    }

    val doctors: StateFlow<List<Doctor>> = (repository?.getDoctorsFlow() ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredDoctors: StateFlow<List<Doctor>> = combine(
        doctors,
        debouncedQuery,
        _selectedDay,
        _selectedSpecialty
    ) { docList, query, day, specialty ->
        try {
            val list = docList
            val trimmedQuery = query.trim()
            val isSearching = trimmedQuery.isNotEmpty()
            val calendar = Calendar.getInstance()
            val activeTargetDay = day ?: currentDayArabic

            list.filter { doc ->
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

                matchesQuery && matchesSpecialty
            }.sortedWith { d1, d2 ->
                val d1Avail = d1.isAvailableOnDay(activeTargetDay)
                val d2Avail = d2.isAvailableOnDay(activeTargetDay)

                val isToday = (day == null || day == currentDayArabic)
                val d1Open = if (isToday) d1.isOpenNow(calendar) else false
                val d2Open = if (isToday) d2.isOpenNow(calendar) else false

                val d1Score = (if (d1Avail) 30 else 0) + (if (d1Open) 15 else 0)
                val d2Score = (if (d2Avail) 30 else 0) + (if (d2Open) 15 else 0)

                when {
                    d1Score != d2Score -> d2Score.compareTo(d1Score)
                    d1.orderIndex != d2.orderIndex -> d1.orderIndex.compareTo(d2.orderIndex)
                    else -> d1.name.compareTo(d2.name)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in filter/sort combine: ${e.message}")
            emptyList()
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
        _currentTab.value = tab
    }
}
