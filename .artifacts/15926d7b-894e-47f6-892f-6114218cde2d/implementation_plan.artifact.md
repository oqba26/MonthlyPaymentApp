# برنامه اجرایی: مدرن‌سازی و استانداردسازی پروژه (فاز ۱)

این برنامه روی دو محور اصلی تمرکز دارد: پیاده‌سازی **Hilt** برای مدیریت وابستگی‌ها و **استانداردسازی متون** برنامه با استفاده از `strings.xml`.

## Proposed Changes

### ۱. زیرساخت و مدیریت وابستگی‌ها (Hilt)

#### [MODIFY] [libs.versions.toml](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/gradle/libs.versions.toml)
- افزودن نسخه‌ها و کتابخانه‌های مورد نیاز برای Hilt.
- افزودن پلاگین Hilt.

#### [MODIFY] [build.gradle.kts (Project)](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/build.gradle.kts)
- اعمال پلاگین Hilt.

#### [MODIFY] [build.gradle.kts (App)](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/build.gradle.kts)
- اعمال پلاگین `dagger.hilt.android.plugin`.
- افزودن وابستگی‌های `hilt-android` و `hilt-navigation-compose`.
- تنظیم KSP برای کامپایلر Hilt.

#### [MODIFY] [PaymentApplication.kt](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/core/PaymentApplication.kt)
- افزودن آنوتیشن `@HiltAndroidApp`.
- حذف مدیریت دستی Repositoryها و Factoryها.

#### [NEW] [DatabaseModule.kt](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/core/di/DatabaseModule.kt)
- ایجاد ماژول Hilt برای ارائه (Provide) دیتابیس Room و DAOها.

#### [NEW] [RepositoryModule.kt](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/core/di/RepositoryModule.kt)
- ایجاد ماژول Hilt برای ارائه Repositoryها.

#### [MODIFY] [ViewModels](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/viewmodel/)
- افزودن `@HiltViewModel` به `PersonViewModel` و `SettingsViewModel`.
- استفاده از `@Inject constructor` برای تزریق وابستگی‌ها.

#### [MODIFY] [MainActivity.kt](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/MainActivity.kt)
- افزودن آنوتیشن `@AndroidEntryPoint`.
- استفاده از `hiltViewModel()` به جای دستی ساختن ViewModelها.

---

### ۲. استانداردسازی متون (String Resources)

#### [MODIFY] [strings.xml](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/res/values/strings.xml)
- استخراج و تعریف متون فارسی از UI به فایل XML.

#### [MODIFY] [Compose Screens](file:///E:/AndroidStudioProjects/MonthlyPaymentApp/app/src/main/java/com/oqba26/monthlypaymentapp/ui/screens/)
- جایگزینی متون هاردکد شده با `stringResource(R.string.id)`.

## Verification Plan

### Automated Tests
- اجرای `gradle build` برای اطمینان از صحت پیکربندی Hilt.
- بررسی عدم وجود خطای کامپایل در کدهای تولید شده توسط KSP.

### Manual Verification
- اجرای برنامه و اطمینان از لود شدن صحیح داده‌ها از دیتابیس و شبکه (تایید کارکرد صحیح DI).
- بررسی نمایش صحیح متون در صفحات مختلف.
