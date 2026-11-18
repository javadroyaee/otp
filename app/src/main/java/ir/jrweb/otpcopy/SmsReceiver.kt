package ir.jrweb.otpcopy

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.Telephony
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("jrweb_otp_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_enabled", true)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullMessage = messages.joinToString("") { it.messageBody ?: "" }

        val code = extractOtpCode(fullMessage) ?: return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("JRweb OTP", code)
        clipboard.setPrimaryClip(clip)

        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) { }

        Toast.makeText(context, "کد $code کپی شد (JRweb)", Toast.LENGTH_SHORT).show()
    }

    private fun extractOtpCode(message: String): String? {
        val normalized = convertPersianDigitsToEnglish(message)
        val lower = normalized.lowercase()

        val keywords = listOf(
            "رمز", "کد", "تایید", "تائید", "پویا", "یکبار", "ورود",
            "otp", "code", "pass", "verify", "verification", "pin"
        )
        if (keywords.none { lower.contains(it) }) return null

        val regex = Regex("""(?<!\d)\d{4,8}(?!\d)""")
        return regex.find(normalized)?.value
    }

    private fun convertPersianDigitsToEnglish(input: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persian[i], ('0' + i))
            result = result.replace(arabic[i], ('0' + i))
        }
        return result
    }
}
