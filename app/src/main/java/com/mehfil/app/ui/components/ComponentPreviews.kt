package com.mehfil.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.AttendanceState
import com.mehfil.app.ui.theme.MehfilTheme
import com.mehfil.app.ui.theme.Space

/**
 * Previews for the design system, in both themes.
 *
 * The app has no XML layouts, so these are the only way to review a component's states without building and
 * navigating to the screen that uses it — and they are what catches a contrast regression in the dark set.
 */
@Preview(name = "Buttons · light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Buttons · dark", showBackground = true, backgroundColor = 0xFF222222, uiMode = 0x20)
@Composable
private fun ButtonPreviews() = PreviewFrame {
    MButton("Primary", {})
    MButton("Secondary", {}, kind = ButtonKind.SECONDARY)
    MButton("Tertiary", {}, kind = ButtonKind.TERTIARY, compact = true)
    MButton("Destructive", {}, kind = ButtonKind.DESTRUCTIVE)
    MButton("Disabled", {}, enabled = false)
    MButton("Working", {}, loading = true)
}

@Preview(name = "Fields · light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Fields · dark", showBackground = true, backgroundColor = 0xFF222222, uiMode = 0x20)
@Composable
private fun FieldPreviews() = PreviewFrame {
    var text by remember { mutableStateOf("Mehfil Decor") }
    var secret by remember { mutableStateOf("Secret123") }
    var phone by remember { mutableStateOf("98110") }
    InputField("Business name", text, { text = it })
    InputField("Password", secret, { secret = it }, password = true)
    PhoneField(phone, { phone = it })
    InputField("With an error", "", {}, error = "This one is required.")
}

@Preview(name = "Status chips · light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Status chips · dark", showBackground = true, backgroundColor = 0xFF222222, uiMode = 0x20)
@Composable
private fun ChipPreviews() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        ChipKind.entries.take(4).forEach { StatusChip(it) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        ChipKind.entries.drop(4).forEach { StatusChip(it) }
    }
}

@Preview(name = "Avatars", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AvatarPreviews() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        listOf("Vikram Kapoor", "Ritu Sharma", "Harpreet Gill", "Ajay", "").forEach {
            Avatar(null, it)
        }
    }
}

@Preview(name = "Attendance states", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AttendancePreviews() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        AttendanceState.entries.forEach { TriState(it) {} }
    }
}

@Preview(name = "Empty state", showBackground = true, backgroundColor = 0xFFFFFFFF, heightDp = 320)
@Composable
private fun EmptyStatePreview() = PreviewFrame {
    EmptyState(
        headline = "No events booked",
        line = "Create your first event and Mehfil starts watching the dates, the money and the crew.",
        actionTitle = "Create event",
        primary = true,
    ) {}
}

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    MehfilTheme {
        Column(
            Modifier.fillMaxWidth().padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) { content() }
    }
}
