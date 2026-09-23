package com.mehfil.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalDate
import java.time.YearMonth

/**
 * Savers for the date types the screens keep in state. `rememberSaveable` needs them because neither
 * [LocalDate] nor [YearMonth] is Parcelable, and losing the browsed month or the selected day on process
 * death is the difference between the app feeling reliable and feeling broken.
 */
val LocalDateSaver: Saver<LocalDate, String> = Saver(save = { it.toString() }, restore = LocalDate::parse)

val YearMonthSaver: Saver<YearMonth, String> = Saver(save = { it.toString() }, restore = YearMonth::parse)

@Composable
fun rememberSaveableDate(initial: () -> LocalDate) = rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(initial()) }

@Composable
fun rememberSaveableMonth(initial: () -> YearMonth) = rememberSaveable(stateSaver = YearMonthSaver) { mutableStateOf(initial()) }
