package com.humanoidai.communication

import android.content.Context
import android.provider.CallLog
import android.util.Log
import java.util.*

class MissedCallDetector(private val context: Context) {

    fun getMissedCalls(): List<CommunicationItem> {
        val missedCalls = mutableListOf<CommunicationItem>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE
        )

        val selection = "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE}"
        val sortOrder = "${CallLog.Calls.DATE} DESC"

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val idIndex = it.getColumnIndex(CallLog.Calls._ID)

                while (it.moveToNext()) {
                    val number = it.getString(numberIndex)
                    val name = it.getString(nameIndex) ?: "Unknown Caller"
                    val date = it.getLong(dateIndex)
                    val id = it.getString(idIndex)

                    // Only consider calls from the last 24 hours for "What did I miss?"
                    if (System.currentTimeMillis() - date < 24 * 60 * 60 * 1000) {
                        missedCalls.add(
                            CommunicationItem(
                                id = "call_$id",
                                sourcePackage = "com.android.server.telecom",
                                sourceApp = "Phone",
                                sender = if (name == "Unknown Caller") number else name,
                                title = "Missed Call",
                                contentPreview = "Call from $number",
                                timestamp = date,
                                type = CommunicationType.MISSED_CALL
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("MissedCallDetector", "Permission denied for call log: ${e.message}")
        } catch (e: Exception) {
            Log.e("MissedCallDetector", "Error querying call log: ${e.message}")
        }

        return missedCalls
    }
}
