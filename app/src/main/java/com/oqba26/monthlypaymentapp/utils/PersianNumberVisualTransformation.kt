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
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val number = originalText.toLongOrNull() ?: 0L
        val formattedText = numberFormat.format(number)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                var transformedOffset = 0
                var originalOffset = 0
                for (char in formattedText) {
                    if (char != '٬') {
                        originalOffset++
                    }
                    transformedOffset++
                    if (originalOffset == offset) break
                }
                return transformedOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset == 0) return 0
                var originalOffset = 0
                for (i in 0 until offset.coerceAtMost(formattedText.length)) {
                    if (formattedText[i] != '٬') {
                        originalOffset++
                    }
                }
                return originalOffset
            }
        }

        return TransformedText(
            text = AnnotatedString(formattedText),
            offsetMapping = offsetMapping
        )
    }
}

class PersianDigitsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = text.text.toPersianDigits()
        return TransformedText(
            text = AnnotatedString(transformed),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
