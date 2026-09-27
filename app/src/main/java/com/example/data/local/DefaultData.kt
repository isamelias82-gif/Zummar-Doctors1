package com.example.data.local

import com.example.data.model.Doctor
import com.example.data.model.Laboratory
import com.example.data.model.Pharmacy

object DefaultData {
    val initialDoctors = listOf(
        Doctor(
            id = 1,
            name = "د. أحمد يونس الجبوري",
            title = "أخصائي أمراض القلب والباطنية",
            specialty = "الباطنية والقلبية",
            days = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس"),
            startHour = 15,
            startMinute = 30,
            endHour = 20,
            endMinute = 30,
            workingHoursText = "من 3:30 عصراً إلى 8:30 مساءً",
            addressLandmark = "ناحية زمار - الشارع الرئيسي - مقابل صيدلية الشفاء المركزية",
            phoneNumbers = listOf("07875023922", "07701548231"),
            notes = "تخطيط قلب رقمي (ECG)، متابعة أمراض الضغط والسكري، الحجز مسبقاً",
            isEmergencyAvailable = true,
            orderIndex = 1
        ),
        Doctor(
            id = 2,
            name = "د. سارة فاضل الزبيدي",
            title = "أخصائية طب الأطفال وحديثي الولادة",
            specialty = "طب الأطفال",
            days = listOf("السبت", "الأحد", "الإثنين", "الأربعاء", "الخميس"),
            startHour = 16,
            startMinute = 0,
            endHour = 20,
            endMinute = 0,
            workingHoursText = "من 4:00 عصراً إلى 8:00 مساءً",
            addressLandmark = "زمار - قرب المركز الصحي النموذجي - مجمع السلام الطبي",
            phoneNumbers = listOf("07503928172", "07821948371"),
            notes = "معاينة حديثي الولادة والخدج، لقاحات ومتابعة نمو الطفل",
            isEmergencyAvailable = true,
            orderIndex = 2
        ),
        Doctor(
            id = 3,
            name = "د. عمر خالد الحديدي",
            title = "أخصائي جراحة العظام والمفاصل والكسور",
            specialty = "جراحة العظام والمفاصل",
            days = listOf("السبت", "الإثنين", "الثلاثاء", "الخميس"),
            startHour = 16,
            startMinute = 30,
            endHour = 21,
            endMinute = 0,
            workingHoursText = "من 4:30 عصراً إلى 9:00 مساءً",
            addressLandmark = "السوق العام - مجاور مختبر زمار المركزي - الطابق الأول",
            phoneNumbers = listOf("07712398456", "07804561234"),
            notes = "معالجة الانزلاق الغضروفي، تبديل المفاصل، تجبير ومعالجة الكسور",
            isEmergencyAvailable = false,
            orderIndex = 3
        ),
        Doctor(
            id = 4,
            name = "د. فاطمة عبد الرحمن النعيمي",
            title = "أخصائية طب وجراحة النسائية والتوليد والعقم",
            specialty = "النسائية والتوليد",
            days = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء"),
            startHour = 15,
            startMinute = 0,
            endHour = 19,
            endMinute = 30,
            workingHoursText = "من 3:00 عصراً إلى 7:30 مساءً",
            addressLandmark = "زمار - شارع المستوصف - عمارة الشفاء - قرب صيدلية بلسم",
            phoneNumbers = listOf("07519827364", "07831092847"),
            notes = "فحص سونار ملون، متابعة الحمل الحرج، علاج العقم وتأخر الإنجاب",
            isEmergencyAvailable = true,
            orderIndex = 4
        ),
        Doctor(
            id = 5,
            name = "د. زيد إبراهيم الجرجري",
            title = "طبيب وجراح الفم والأسنان",
            specialty = "طب وجراحة الأسنان",
            days = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس"),
            startHour = 16,
            startMinute = 0,
            endHour = 21,
            endMinute = 30,
            workingHoursText = "من 4:00 عصراً إلى 9:30 مساءً",
            addressLandmark = "الشارع التجاري العام - عمارة الرافدين - قرب مصرف الرافدين",
            phoneNumbers = listOf("07709812734", "07504381920"),
            notes = "زراعة أسنان، تقويم الأسنان، حشوات تجميلية وابتسامة هوليوود",
            isEmergencyAvailable = false,
            orderIndex = 5
        ),
        Doctor(
            id = 6,
            name = "د. حسن علي البكوع",
            title = "استشاري الجراحة العامة والناظورية",
            specialty = "الجراحة العامة والناظورية",
            days = listOf("الأحد", "الثلاثاء", "الخميس"),
            startHour = 17,
            startMinute = 0,
            endHour = 21,
            endMinute = 0,
            workingHoursText = "من 5:00 عصراً إلى 9:00 مساءً",
            addressLandmark = "مدخل زمار الرئيسي - قرب ساحة الاحتفالات - مجمع النور الطبي",
            phoneNumbers = listOf("07812938475"),
            notes = "عمليات المرارة والزائدة بالناظور، الفتق والجراحة الصغرى",
            isEmergencyAvailable = true,
            orderIndex = 6
        ),
        Doctor(
            id = 7,
            name = "د. رامي سعد الهاشمي",
            title = "أخصائي طب وجراحة العيون وتصحيح البصر",
            specialty = "العيون وجراحتها",
            days = listOf("السبت", "الإثنين", "الأربعاء"),
            startHour = 15,
            startMinute = 30,
            endHour = 19,
            endMinute = 30,
            workingHoursText = "من 3:30 عصراً إلى 7:30 مساءً",
            addressLandmark = "شارع السوق الكبير - مجاور نظارات البصائر",
            phoneNumbers = listOf("07721839405", "07518293041"),
            notes = "فحص بصر بالكمبيوتر، فحص قاع العين، علاج الماء الأبيض والأزرق",
            isEmergencyAvailable = false,
            orderIndex = 7
        ),
        Doctor(
            id = 8,
            name = "د. يوسف طه العبيدي",
            title = "أخصائي جراحة الأنف والأذن والحنجرة",
            specialty = "الأنف والأذن والحنجرة",
            days = listOf("الأحد", "الإثنين", "الأربعاء", "الخميس"),
            startHour = 16,
            startMinute = 0,
            endHour = 20,
            endMinute = 0,
            workingHoursText = "من 4:00 عصراً إلى 8:00 مساءً",
            addressLandmark = "زمار - مجمع الأطباء الاستشاري - مقابل كراج الموصل",
            phoneNumbers = listOf("07809182736"),
            notes = "ناظور الأنف والجيوب، تخطيط السمع، تنظيف الأذن بالميكروسكوب",
            isEmergencyAvailable = false,
            orderIndex = 8
        ),
        Doctor(
            id = 9,
            name = "د. مروة سلام الدليمي",
            title = "أخصائية الأمراض الجلدية والتجميل والليزر",
            specialty = "الجلدية والتجميل",
            days = listOf("السبت", "الأحد", "الثلاثاء", "الخميس"),
            startHour = 14,
            startMinute = 30,
            endHour = 18,
            endMinute = 30,
            workingHoursText = "من 2:30 بعد الظهر إلى 6:30 مساءً",
            addressLandmark = "زمار - الشارع العام - عمارة الشفاء الطبية - الطابق الثاني",
            phoneNumbers = listOf("07507182934", "07706192837"),
            notes = "علاج حب الشباب والتصبغات، جلسات نضارة وبوتوكس، إزالة الزوائد الجلدية",
            isEmergencyAvailable = false,
            orderIndex = 9
        ),
        Doctor(
            id = 10,
            name = "د. أنس كمال الكردي",
            title = "أخصائي جراحة الكلى والمسالك البولية والعقم",
            specialty = "المسالك البولية والكلى",
            days = listOf("السبت", "الإثنين", "الأربعاء"),
            startHour = 16,
            startMinute = 30,
            endHour = 20,
            endMinute = 30,
            workingHoursText = "من 4:30 عصراً إلى 8:30 مساءً",
            addressLandmark = "شارع المستشفى القديم - مجمع العافية التخصصي",
            phoneNumbers = listOf("07823491827"),
            notes = "تفتيت حصى الكلى، علاج البروستات، تشخيص أمراض الجهاز البولي",
            isEmergencyAvailable = false,
            orderIndex = 10
        ),
        Doctor(
            id = 11,
            name = "د. نادية إحسان البدراني",
            title = "أخصائية الأشعة والسونار والدوبلر الملون",
            specialty = "الأشعة والسونار",
            days = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس"),
            startHour = 15,
            startMinute = 0,
            endHour = 20,
            endMinute = 0,
            workingHoursText = "من 3:00 عصراً إلى 8:00 مساءً",
            addressLandmark = "زمار - الشارع العام - مقابل البريد - مجمع الحياة السريري",
            phoneNumbers = listOf("07719283746", "07519284710"),
            notes = "سونار رباعي الأبعاد 4D، سونار البطن والحوض، فحص شرايين وأوردة دوبلر",
            isEmergencyAvailable = true,
            orderIndex = 11
        )
    )

    val initialPharmacies = listOf(
        Pharmacy(
            id = 1,
            name = "صيدلية الشفاء المركزية",
            pharmacist = "د. مروان أحمد الصيدلاني",
            addressLandmark = "ناحية زمار - الشارع العام - مقابل مجمع الأطباء",
            phoneNumbers = listOf("07875023922", "07708192837"),
            onCallDays = "خافرة يومي الجمعة والسبت (24 ساعة) وطيلة أيام الأسبوع حتى منتصف الليل",
            workingHours = "مفتوح يومياً 8:00 صباحاً – 12:00 منتصف الليل (خافرة الجمعة)",
            isOnDutyTonight = true,
            notes = "تتوفر كافة الأدوية المزمنة، إبر الأنسولين، وحليب الأطفال التخصصي"
        ),
        Pharmacy(
            id = 2,
            name = "صيدلية بلسم زمار",
            pharmacist = "د. حسام الجبوري",
            addressLandmark = "زمار - شارع المركز الصحي - قرب مجمع السلام",
            phoneNumbers = listOf("07501827364"),
            onCallDays = "خافرة يومي الإثنين والثلاثاء",
            workingHours = "8:30 صباحاً – 11:30 مساءً",
            isOnDutyTonight = false,
            notes = "قياس ضغط وسكر مجاناً، أجهزة استنشاق ومستلزمات طبية"
        ),
        Pharmacy(
            id = 3,
            name = "صيدلية النور",
            pharmacist = "د. عمار الحديدي",
            addressLandmark = "السوق التجاري الرئيسي - قرب جامع زمار الكبير",
            phoneNumbers = listOf("07819283746"),
            onCallDays = "خافرة يومي الأربعاء والخميس",
            workingHours = "9:00 صباحاً – 11:00 مساءً",
            isOnDutyTonight = false,
            notes = "خدمة توصيل أدوية الحالات الطارئة متوفرة داخل زمار"
        ),
        Pharmacy(
            id = 4,
            name = "صيدلية الرافدين",
            pharmacist = "د. علي الخالدي",
            addressLandmark = "الشارع الرئيسي - قرب مصرف الرافدين زمار",
            phoneNumbers = listOf("07728192830"),
            onCallDays = "خافرة يوم الأحد بالتناوب",
            workingHours = "8:00 صباحاً – 11:00 مساءً",
            isOnDutyTonight = false,
            notes = "أدوية السرطان والمناعة بالتنسيق مع مراكز الأورام"
        )
    )

    val initialLaboratories = listOf(
        Laboratory(
            id = 1,
            name = "مختبر زمار المركزي للتحليلات التخصصية",
            specialist = "د. قاسم محمد (ماجستير تحليلات مرضية)",
            addressLandmark = "الشارع العام - مجمع الشفاء - الطابق الأول",
            phoneNumbers = listOf("07875023922", "07709182736"),
            workingDays = "السبت إلى الخميس",
            workingHours = "8:00 صباحاً – 8:30 مساءً متواصل",
            services = listOf(
                "فحوصات الدم الشاملة (CBC)",
                "تحاليل الهرمونات وفيتامين D و B12",
                "فحص وظائف الكبد والكلى والدهون",
                "تحاليل السكر التراكمي (HbA1c) بدقائق",
                "زرع الجراثيم والحساسية الدوائية"
            ),
            notes = "أجهزة ألمانية وأمريكية رقمية حديثة، سحب دم منزلي للمرضى وكبار السن"
        ),
        Laboratory(
            id = 2,
            name = "مختبر الرافدين الطبي الحديث",
            specialist = "أ. ليث سالم (بكالوريوس تقنيات طبية)",
            addressLandmark = "قرب مستوصف زمار - عمارة السلام",
            phoneNumbers = listOf("07508291029"),
            workingDays = "طيلة أيام الأسبوع عدا الجمعة",
            workingHours = "8:30 صباحاً – 2:00 ظهراً، 4:00 عصراً – 9:00 مساءً",
            services = listOf(
                "فحوصات ما قبل الزواج المعتمدة",
                "تحاليل السيولة والتخثر (PT/INR)",
                "فحوصات المناعة والأجسام المضادة",
                "تحاليل الحمل المبكر الرقمي"
            ),
            notes = "استلام النتائج عبر الواتساب فور صدورها"
        )
    )
}
