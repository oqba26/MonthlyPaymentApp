package com.oqba26.monthlypaymentapp.utils

import saman.zamani.persiandate.PersianDate

fun getCurrentShamsiYear(): Int = PersianDate().shYear

fun getCurrentShamsiMonth(): Int = PersianDate().shMonth