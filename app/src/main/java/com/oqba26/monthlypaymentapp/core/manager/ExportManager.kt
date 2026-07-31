package com.oqba26.monthlypaymentapp.core.manager

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.utils.getPersianMonthName
import com.oqba26.monthlypaymentapp.utils.toPersianDigits
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun exportYearlyReport(year: Int, persons: List<Person>, payments: List<PaymentRecord>): Boolean {
        val csvContent = StringBuilder()
        csvContent.append("نام,")
        (1..12).forEach { csvContent.append("${getPersianMonthName(it)},") }
        csvContent.append("مجموع\n")

        persons.filter { !it.isArchived }.forEach { person ->
            csvContent.append("${person.name},")
            var total = 0.0
            (1..12).forEach { month ->
                val payment = payments.find { it.personId == person.id && it.shamsiMonth == month }
                if (payment != null) {
                    csvContent.append("${payment.amount},")
                    total += payment.amount
                } else {
                    csvContent.append("-,")
                }
            }
            csvContent.append("$total\n")
        }

        return try {
            val fileName = "Report_$year.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(csvContent.toString().toByteArray()) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "اشتراک‌گذاری گزارش سال $year")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareReceipt(payment: PaymentRecord, personName: String) {
        val message = """
            📜 رسید پرداخت ماهانه
            👤 نام: $personName
            📅 بابت: ${getPersianMonthName(payment.shamsiMonth)} ${payment.shamsiYear.toString().toPersianDigits()}
            💰 مبلغ: ${formatNumberAsPersian(payment.amount)} تومان
            📝 توضیحات: ${payment.description ?: "---"}
            ✅ با موفقیت ثبت شد.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "اشتراک‌گذاری رسید")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
