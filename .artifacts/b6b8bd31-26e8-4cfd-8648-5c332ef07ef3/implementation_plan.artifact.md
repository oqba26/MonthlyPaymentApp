# سیستم آپدیت اجباری و مدیریت دسترسی‌ها

این طرح شامل پیاده‌سازی مکانیزم بررسی نسخه جدید اپلیکیشن از طریق فایل JSON در گیتهاب و همچنین الزامی کردن دسترسی‌های مورد نیاز برنامه (مشابه پروژه ابزار فروش) در بدو ورود کاربر است.

## User Review Required

- **لینک فایل آپدیت:** فعلاً از لینک `https://raw.githubusercontent.com/oqba26/MonthlyPaymentApp/main/update.json` استفاده شده است. لطفاً مطمئن شوید این فایل در مخزن شما وجود دارد.
- **دسترسی‌های اجباری:** دسترسی‌های Contacts، Camera و Storage (برای نسخه‌های قدیمی) به صورت پیش‌فرض اجباری در نظر گرفته شده‌اند.

## Proposed Changes

### [Dependencies]

#### [MODIFY] [libs.versions.toml](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/gradle/libs.versions.toml)
افزودن کتابخانه‌های Ktor برای Content Negotiation و Serialization جهت پردازش فایل JSON آپدیت.

#### [MODIFY] [build.gradle.kts](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/build.gradle.kts)
اضافه کردن وابستگی‌های جدید به ماژول اپلیکیشن.

---

### [Update System]

#### [NEW] [UpdateManager.kt](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/util/UpdateManager.kt)
کلاسی برای چک کردن نسخه جدید، دانلود APK و اجرای فرآیند نصب.

#### [NEW] [UpdateDialog.kt](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/ui/components/UpdateDialog.kt)
رابط کاربری نمایش وجود نسخه جدید و گزارش پیشرفت دانلود.

---

### [Permissions & Manifest]

#### [NEW] [PermissionDialogs.kt](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/ui/components/PermissionDialogs.kt)
دیالوگ‌های نمایش دسترسی‌های الزامی و اخطار عدم دسترسی.

#### [MODIFY] [AndroidManifest.xml](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/AndroidManifest.xml)
افزودن مجوز `REQUEST_INSTALL_PACKAGES` و تعریف `FileProvider` برای نصب خودکار فایل APK.

#### [NEW] [file_paths.xml](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/res/xml/file_paths.xml)
تعریف مسیرهای مجاز برای `FileProvider`.

---

### [Integration]

#### [MODIFY] [MainActivity.kt](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/MainActivity.kt)
یکپارچه‌سازی فرآیند بررسی آپدیت و چک کردن دسترسی‌ها در زمان اجرای برنامه.

## Verification Plan

### Manual Verification
1. اجرای برنامه و مشاهده دیالوگ دسترسی‌ها (در صورت عدم تایید قبلی).
2. شبیه‌سازی وجود آپدیت جدید با تغییر دستی `versionCode` در کد و بررسی نمایش دیالوگ آپدیت.
3. تست فرآیند دانلود و باز شدن صفحه نصب اندروید.
