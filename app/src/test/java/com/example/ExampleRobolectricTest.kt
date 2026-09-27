package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("أطباء زمار", appName)
  }

  @Test
  fun `doctor availability and working days check`() {
    val doctor = com.example.data.model.Doctor(
      name = "د. أحمد يونس",
      specialty = "الباطنية",
      days = listOf("السبت", "الأحد", "الإثنين"),
      startHour = 16,
      endHour = 20
    )
    assertEquals(true, doctor.isAvailableOnDay("السبت"))
    assertEquals(false, doctor.isAvailableOnDay("الجمعة"))
  }

  @Test
  fun `sponsor banner activity and expiry check`() {
    val activeBanner = com.example.data.model.SponsorBanner(
      bannerId = "sponsor_test",
      isActive = true,
      actionType = com.example.data.model.SponsorBanner.ACTION_WHATSAPP,
      actionValue = "+9647875023922",
      expiryDate = "2099-12-31"
    )
    assertEquals(true, activeBanner.isCurrentlyActive())

    val disabledBanner = activeBanner.copy(isActive = false)
    assertEquals(false, disabledBanner.isCurrentlyActive())

    val expiredBanner = activeBanner.copy(expiryDate = "2020-01-01")
    assertEquals(false, expiredBanner.isCurrentlyActive())
  }

  @Test
  fun `admin password verification test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.DoctorRepository(db.doctorDao(), context)

    assertEquals("200120012001", repository.getAdminPin())
    assertEquals(true, repository.verifyPin("200120012001"))
    assertEquals(false, repository.verifyPin("1982"))
    assertEquals(false, repository.verifyPin("1234"))
  }

  @Test
  fun `pharmacies and laboratories coming soon toggles`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.DoctorRepository(db.doctorDao(), context)

    // Default state: both sections disabled (coming soon)
    assertEquals(false, repository.isPharmaciesEnabled())
    assertEquals(false, repository.isLaboratoriesEnabled())

    // Admin enables pharmacies
    repository.setPharmaciesEnabled(true)
    assertEquals(true, repository.isPharmaciesEnabled())

    // Admin enables laboratories
    repository.setLaboratoriesEnabled(true)
    assertEquals(true, repository.isLaboratoriesEnabled())

    // Admin disables pharmacies again
    repository.setPharmaciesEnabled(false)
    assertEquals(false, repository.isPharmaciesEnabled())
  }

  @Test
  fun `instant live search matches one or two letters and sections`() {
    val doctor1 = com.example.data.model.Doctor(
      id = 101L,
      name = "د. أحمد الحبيب",
      title = "اختصاصي أطفال",
      specialty = "طب وجراحة الأطفال",
      days = listOf("السبت", "الأحد"),
      startHour = 9,
      startMinute = 0,
      endHour = 14,
      endMinute = 0,
      workingHoursText = "9:00 ص - 2:00 م",
      addressLandmark = "شارع الأطباء، قرب الصيدلية المركزية",
      phoneNumbers = listOf("07700000000"),
      notes = "استشارات الأطفال وحديثي الولادة"
    )

    val doctor2 = com.example.data.model.Doctor(
      id = 102L,
      name = "د. علي الخفاجي",
      title = "اختصاصي جلدية",
      specialty = "أمراض جلدية وتجميل",
      days = listOf("الاثنين", "الثلاثاء"),
      startHour = 10,
      startMinute = 0,
      endHour = 16,
      endMinute = 0,
      workingHoursText = "10:00 ص - 4:00 م",
      addressLandmark = "مجمع زمار الطبي، الطابق الثاني",
      phoneNumbers = listOf("07800000000"),
      notes = "علاج الأمراض الجلدية والليزر"
    )

    // Test 1-letter search
    assertEquals(true, com.example.util.ArabicSearchUtils.matchesDoctor(doctor1, "ا"))
    assertEquals(true, com.example.util.ArabicSearchUtils.matchesDoctor(doctor2, "ع"))

    // Test 2-letter search for doctor names
    assertEquals(true, com.example.util.ArabicSearchUtils.matchesDoctor(doctor1, "اح"))
    assertEquals(true, com.example.util.ArabicSearchUtils.matchesDoctor(doctor2, "عل"))

    // Test search without hamza matching text with hamza
    assertEquals(true, com.example.util.ArabicSearchUtils.matchesDoctor(doctor1, "احمد"))

    // Test section / specialty search with 2 letters
    assertEquals(true, com.example.util.ArabicSearchUtils.matchesDoctor(doctor1, "اط")) // أطفال
    assertEquals(true, com.example.util.ArabicSearchUtils.matchesDoctor(doctor2, "جل")) // جلدية
    assertEquals(false, com.example.util.ArabicSearchUtils.matchesDoctor(doctor1, "جل"))

    // Test multi-token search
    assertEquals(true, com.example.util.ArabicSearchUtils.matchesDoctor(doctor1, "احمد اطفال"))
  }
}
