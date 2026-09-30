package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Doctor
import com.example.data.model.SponsorBanner
import com.example.ui.components.isPhoneNumber
import com.example.util.ArabicSearchUtils
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
    val doctor = Doctor(
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
    val activeBanner = SponsorBanner(
      bannerId = "sponsor_test",
      isActive = true,
      actionType = SponsorBanner.ACTION_PHONE,
      actionValue = "07875023922",
      expiryDate = "2099-12-31"
    )
    assertEquals(true, activeBanner.isCurrentlyActive())

    val disabledBanner = activeBanner.copy(isActive = false)
    assertEquals(false, disabledBanner.isCurrentlyActive())

    val expiredBanner = activeBanner.copy(expiryDate = "2020-01-01")
    assertEquals(false, expiredBanner.isCurrentlyActive())
  }

  @Test
  fun `smart phone number versus url detection`() {
    // Phone numbers (triggers Intent.ACTION_DIAL)
    assertEquals(true, isPhoneNumber("07800000000", "AUTO"))
    assertEquals(true, isPhoneNumber("+9647875023922", "AUTO"))
    assertEquals(true, isPhoneNumber("0770 123 4567", "AUTO"))
    assertEquals(true, isPhoneNumber("0780-123-4567", "AUTO"))
    assertEquals(true, isPhoneNumber("tel:07800000000", "AUTO"))
    assertEquals(true, isPhoneNumber("07800000000", "PHONE"))

    // URLs (triggers Intent.ACTION_VIEW)
    assertEquals(false, isPhoneNumber("https://chat.crisp.chat/l/50ac8743-e9cf-4f46-a2f1-888d6724bd72", "AUTO"))
    assertEquals(false, isPhoneNumber("https://google.com", "AUTO"))
    assertEquals(false, isPhoneNumber("http://example.com/clinic", "AUTO"))
    assertEquals(false, isPhoneNumber("www.alnoor.iq", "AUTO"))
    assertEquals(false, isPhoneNumber("alnoor-clinic.com", "AUTO"))
    assertEquals(false, isPhoneNumber("https://wa.me/9647800000000", "AUTO"))
    assertEquals(false, isPhoneNumber("07800000000", "URL"))
    assertEquals(false, isPhoneNumber("07800000000", "WHATSAPP"))
  }

  @Test
  fun `instant live search matches one or two letters and sections`() {
    val doctor1 = Doctor(
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

    val doctor2 = Doctor(
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
    assertEquals(true, ArabicSearchUtils.matchesDoctor(doctor1, "ا"))
    assertEquals(true, ArabicSearchUtils.matchesDoctor(doctor2, "ع"))

    // Test 2-letter search for doctor names
    assertEquals(true, ArabicSearchUtils.matchesDoctor(doctor1, "اح"))
    assertEquals(true, ArabicSearchUtils.matchesDoctor(doctor2, "عل"))

    // Test search without hamza matching text with hamza
    assertEquals(true, ArabicSearchUtils.matchesDoctor(doctor1, "احمد"))

    // Test section / specialty search with 2 letters
    assertEquals(true, ArabicSearchUtils.matchesDoctor(doctor1, "اط")) // أطفال
    assertEquals(true, ArabicSearchUtils.matchesDoctor(doctor2, "جل")) // جلدية
    assertEquals(false, ArabicSearchUtils.matchesDoctor(doctor1, "جل"))

    // Test multi-token search
    assertEquals(true, ArabicSearchUtils.matchesDoctor(doctor1, "احمد اطفال"))
  }
}
