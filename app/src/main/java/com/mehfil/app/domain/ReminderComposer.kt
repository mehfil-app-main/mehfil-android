package com.mehfil.app.domain

import android.content.Context
import com.mehfil.app.R
import com.mehfil.app.model.Client
import com.mehfil.app.model.Event
import com.mehfil.app.model.Fmt
import com.mehfil.app.model.Milestone
import com.mehfil.app.model.ReminderTone
import com.mehfil.app.model.VendorProfile

/**
 * Composes the payment reminder a vendor sends over WhatsApp or SMS.
 *
 * These are the only strings in the app a *client* reads, so they live in `strings.xml` with ordered arguments
 * — a vendor working in another language needs to reword them without touching Kotlin, and the argument order
 * differs between languages.
 */
class ReminderComposer(private val context: Context) {

    fun compose(tone: ReminderTone, milestone: Milestone, event: Event, client: Client, vendor: VendorProfile): String {
        val amount = Fmt.inr(milestone.amount)
        val due = Fmt.dayMonthLong(milestone.due)
        val item = milestone.name.lowercase()
        val hasUpi = vendor.upi.isNotEmpty()

        return when (tone) {
            ReminderTone.GENTLE -> context.getString(
                R.string.reminder_gentle,
                client.firstName, item, amount, event.type.word, due, vendor.ownerFirstName, vendor.businessName,
            )
            ReminderTone.STANDARD -> context.getString(
                R.string.reminder_standard,
                client.firstName, item, amount, event.type.word, Fmt.dayMonthLong(event.day), due,
                if (hasUpi) context.getString(R.string.reminder_pay_upi_or_bank, vendor.upi) else context.getString(R.string.reminder_pay_bank),
                vendor.ownerFirstName, vendor.businessName,
            )
            ReminderTone.FIRM -> context.getString(
                R.string.reminder_firm,
                client.firstName, item, amount, event.type.word, Fmt.dayMonthLong(event.day), Fmt.daysLate(milestone.due),
                if (hasUpi) context.getString(R.string.reminder_pay_upi_today, vendor.upi) else context.getString(R.string.reminder_pay_today),
                vendor.ownerFirstName, vendor.businessName,
            )
        }
    }
}
