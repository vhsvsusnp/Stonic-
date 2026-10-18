package com.example.util

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.local.NoteEntity
import com.example.data.local.StonicDao
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

sealed class JarvisResult {
    data class Executed(
        val spokenFeedback: String,
        val displayFeedback: String,
        val actionType: String
    ) : JarvisResult()

    data object ForwardToAi : JarvisResult()
}

class JarvisActionExecutor(
    private val context: Context,
    private val stonicDao: StonicDao
) {

    /**
     * Checks if the user query is a direct device action or system request.
     * If so, executes the Android intent / local logic and returns feedback.
     * Otherwise, returns ForwardToAi.
     */
    suspend fun tryExecuteAction(query: String): JarvisResult {
        val trimmed = query.trim()
        val lower = trimmed.lowercase(Locale.ENGLISH)

        // 1. Time & Date queries
        if (lower in listOf("what time is it", "tell me the time", "current time", "what is the time", "time please")) {
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            return JarvisResult.Executed(
                spokenFeedback = "The current time is $time.",
                displayFeedback = "Current Time: $time",
                actionType = "Clock"
            )
        }

        if (lower in listOf("what is today's date", "what's the date", "today's date", "current date", "what day is it")) {
            val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            return JarvisResult.Executed(
                spokenFeedback = "Today is $date.",
                displayFeedback = "Date: $date",
                actionType = "Calendar"
            )
        }

        // 2. Open Camera
        if (lower in listOf("open camera", "take a picture", "launch camera", "start camera")) {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return if (safeStartActivity(intent)) {
                JarvisResult.Executed("Opening camera for you.", "Camera launched.", "Camera")
            } else {
                JarvisResult.Executed("Could not open camera app.", "Camera app unavailable.", "Camera")
            }
        }

        // 3. Open Settings
        if (lower in listOf("open settings", "device settings", "launch settings")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return if (safeStartActivity(intent)) {
                JarvisResult.Executed("Opening device settings.", "Settings opened.", "Settings")
            } else {
                JarvisResult.Executed("Unable to access system settings.", "Settings unavailable.", "Settings")
            }
        }

        // 4. Open YouTube
        if (lower.startsWith("open youtube") || lower == "launch youtube") {
            val pkg = "com.google.android.youtube"
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            safeStartActivity(intent)
            return JarvisResult.Executed("Opening YouTube.", "YouTube launched.", "App")
        }

        // 5. Open WhatsApp
        if (lower.startsWith("open whatsapp") || lower == "launch whatsapp") {
            val pkg = "com.whatsapp"
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            return if (intent != null && safeStartActivity(intent)) {
                JarvisResult.Executed("Opening WhatsApp.", "WhatsApp launched.", "App")
            } else {
                JarvisResult.Executed("WhatsApp is not installed on this device.", "WhatsApp not installed.", "App")
            }
        }

        // 6. Open Google Maps
        if (lower.startsWith("open maps") || lower.startsWith("open google maps")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            safeStartActivity(intent)
            return JarvisResult.Executed("Opening Maps.", "Google Maps launched.", "App")
        }

        // 7. General App Opener ("open [app]")
        if (lower.startsWith("open ")) {
            val appTarget = lower.removePrefix("open ").trim()
            val result = launchInstalledAppByName(appTarget)
            if (result != null) {
                return result
            }
        }

        // 8. Alarm / Timer
        if (lower.startsWith("set alarm") || lower.startsWith("set an alarm")) {
            return handleAlarmCommand(lower)
        }

        if (lower.startsWith("set timer") || lower.startsWith("set a timer")) {
            return handleTimerCommand(lower)
        }

        // 9. Quick Local Note Saving ("take a note", "save note", "note that")
        if (lower.startsWith("take a note") || lower.startsWith("note that") || lower.startsWith("save note")) {
            val noteContent = query
                .replace(Regex("^(take a note(:| that)?|note that|save note(:)?)", RegexOption.IGNORE_CASE), "")
                .trim()

            if (noteContent.isNotBlank()) {
                val title = if (noteContent.length > 30) noteContent.take(30) + "..." else noteContent
                stonicDao.insertNote(
                    NoteEntity(
                        title = title,
                        content = noteContent,
                        tag = "Voice Note"
                    )
                )
                return JarvisResult.Executed(
                    spokenFeedback = "Note saved successfully: $title",
                    displayFeedback = "Saved Note: \"$noteContent\"",
                    actionType = "Notes"
                )
            }
        }

        // 10. Math Calculation ("calculate 25 * 4", "what is 50 + 75")
        if (lower.startsWith("calculate ") || lower.matches(Regex("^(what is|what's) [0-9\\.\\s\\+\\-\\*/\\^%]+$"))) {
            val mathExpr = lower.removePrefix("calculate ")
                .removePrefix("what is ")
                .removePrefix("what's ")
                .trim()
            val eval = evaluateMath(mathExpr)
            if (eval != null) {
                return JarvisResult.Executed(
                    spokenFeedback = "The answer is $eval.",
                    displayFeedback = "$mathExpr = $eval",
                    actionType = "Calculator"
                )
            }
        }

        // 11. Web Search ("search for [query]", "google [query]")
        if (lower.startsWith("search for ") || lower.startsWith("google ") || lower.startsWith("search ")) {
            val searchQuery = query
                .replace(Regex("^(search for|google|search)", RegexOption.IGNORE_CASE), "")
                .trim()

            if (searchQuery.isNotBlank()) {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, searchQuery)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                safeStartActivity(intent)
                return JarvisResult.Executed(
                    spokenFeedback = "Searching the web for $searchQuery.",
                    displayFeedback = "Web Search: \"$searchQuery\"",
                    actionType = "WebSearch"
                )
            }
        }

        // Not a direct device action -> send to Gemini AI
        return JarvisResult.ForwardToAi
    }

    private fun handleAlarmCommand(query: String): JarvisResult {
        // Match patterns like "set alarm for 7:30 am", "set alarm for 8", "set alarm at 6:00 pm"
        val pattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(query)

        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 8
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val ampm = matcher.group(3)?.lowercase()

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Stonic AI Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return if (safeStartActivity(intent)) {
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                JarvisResult.Executed("Setting an alarm for $timeStr.", "Alarm set for $timeStr", "Alarm")
            } else {
                JarvisResult.Executed("Could not launch alarm application.", "Alarm permission/app unavailable.", "Alarm")
            }
        }

        return JarvisResult.Executed("Please specify the time for the alarm, e.g., 'set alarm for 7:30 am'.", "Time not recognized.", "Alarm")
    }

    private fun handleTimerCommand(query: String): JarvisResult {
        // e.g. "set timer for 5 minutes", "timer for 30 seconds"
        val numMatch = Regex("(\\d+)\\s*(minute|min|second|sec)", RegexOption.IGNORE_CASE).find(query)
        if (numMatch != null) {
            val amount = numMatch.groupValues[1].toIntOrNull() ?: 1
            val unit = numMatch.groupValues[2].lowercase()
            val seconds = if (unit.startsWith("min")) amount * 60 else amount

            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Stonic AI Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return if (safeStartActivity(intent)) {
                JarvisResult.Executed("Timer set for $amount $unit.", "Timer: $amount $unit", "Timer")
            } else {
                JarvisResult.Executed("Timer app unavailable.", "Could not set timer.", "Timer")
            }
        }
        return JarvisResult.Executed("Please specify minutes or seconds, e.g. 'set timer for 5 minutes'.", "Duration needed.", "Timer")
    }

    private fun launchInstalledAppByName(appName: String): JarvisResult? {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        val target = appName.trim().lowercase()

        for (app in packages) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label == target || label.contains(target)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    safeStartActivity(launchIntent)
                    return JarvisResult.Executed(
                        spokenFeedback = "Opening ${pm.getApplicationLabel(app)}.",
                        displayFeedback = "Launched ${pm.getApplicationLabel(app)}",
                        actionType = "App"
                    )
                }
            }
        }
        return null
    }

    private fun evaluateMath(expression: String): String? {
        return try {
            val clean = expression.replace("x", "*").replace(" ", "")
            // Simple arithmetic parser for +, -, *, /, %
            var result = 0.0
            if (clean.contains("+")) {
                val parts = clean.split("+")
                result = parts[0].toDouble() + parts[1].toDouble()
            } else if (clean.contains("-") && !clean.startsWith("-")) {
                val parts = clean.split("-")
                result = parts[0].toDouble() - parts[1].toDouble()
            } else if (clean.contains("*")) {
                val parts = clean.split("*")
                result = parts[0].toDouble() * parts[1].toDouble()
            } else if (clean.contains("/")) {
                val parts = clean.split("/")
                val denom = parts[1].toDouble()
                if (denom == 0.0) return "Cannot divide by zero"
                result = parts[0].toDouble() / denom
            } else if (clean.contains("%")) {
                val parts = clean.split("%")
                result = parts[0].toDouble() % parts[1].toDouble()
            } else {
                return null
            }
            if (result % 1.0 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.2f", result)
        } catch (_: Exception) {
            null
        }
    }

    private fun safeStartActivity(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
