package com.oqba26.monthlypaymentapp.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

class PersianNumberVisualTransformation : VisualTransformation {

    private val numberFormat = NumberFormat.getNumberInstance(Locale("fa", "IR"))

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isBlank()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val number = originalText.toLongOrNull() ?: 0L
        val formattedText = numberFormat.format(number)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                try {
                    val thousandsSeparators = formattedText.count { it == '٬' }
                    return (offset + thousandsSeparators).coerceIn(0, formattedText.length)
                } catch (e: Exception) {
                    return formattedText.length
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                try {
                    val separatorsBeforeCursor = formattedText.substring(0, offset).count { it == '٬' }
                    return (offset - separatorsBeforeCursor).coerceIn(0, originalText.length)
                } catch (e: Exception) {
                    return originalText.length
                }
            }
        }

        return TransformedText(
            text = AnnotatedString(formattedText),
            offsetMapping = offsetMapping
        )
    }
}
