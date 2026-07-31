package com.oqba26.monthlypaymentapp.core.manager

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.viewmodel.PersonUiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun sendSmsReminder(person: PersonUiModel, selectedCard: String? = null) {
        val intent = getSmsIntent(person, selectedCard) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getSmsIntent(person: PersonUiModel, selectedCard: String? = null): Intent? {
        if (person.phoneNumber.isNullOrBlank()) return null

        val monthsText = if (person.unpaidMonthsNames.size == 1) {
            "بابت ماه ${person.unpaidMonthsNames.first()}"
        } else {
            "بابت ماه‌های ${person.unpaidMonthsNames.joinToString("، ")}"
        }

        var message = """
            سلام ${person.name} عزیز،
            وقت بخیر. پرداخت حقوق شما $monthsText هنوز ثبت نشده است. 
            مبلغ بدهی: ${formatNumberAsPersian(person.totalDebtAmount)} تومان
            لطفاً در اسرع وقت بررسی نمایید. باتشکر.
        """.trimIndent()

        if (!selectedCard.isNullOrBlank()) {
            message += "\nشماره کارت جهت واریز:\n$selectedCard"
        }

        return Intent(Intent.ACTION_VIEW).apply {
            data = "sms:${person.phoneNumber}".toUri()
            putExtra("sms_body", message)
        }
    }
}
