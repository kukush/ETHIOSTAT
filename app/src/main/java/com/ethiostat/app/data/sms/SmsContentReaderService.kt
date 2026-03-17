package com.ethiostat.app.data.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.ethiostat.app.data.local.entity.SmsLogEntity

class SmsContentReaderService(private val context: Context) {
    
    companion object {
        private const val SMS_URI = "content://sms/inbox"
        private val PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT
        )
    }
    
    /**
     * Reads SMS messages from specific sender since given timestamp
     */
    fun readMessagesBySenderSince(sender: String, fromTimestamp: Long): List<SmsLogEntity> {
        val messages = mutableListOf<SmsLogEntity>()
        
        try {
            val selection = "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(sender, fromTimestamp.toString())
            val sortOrder = "${Telephony.Sms.DATE} DESC"
            
            val cursor = context.contentResolver.query(
                Uri.parse(SMS_URI),
                PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val smsEntity = cursorToSmsLogEntity(c)
                    if (smsEntity != null) {
                        messages.add(smsEntity)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Handle permission denied
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return messages
    }
    
    /**
     * Reads latest SMS message from specific sender
     */
    fun readLatestMessageBySender(sender: String): SmsLogEntity? {
        try {
            val selection = "${Telephony.Sms.ADDRESS} = ?"
            val selectionArgs = arrayOf(sender)
            val sortOrder = "${Telephony.Sms.DATE} DESC"
            
            val cursor = context.contentResolver.query(
                Uri.parse(SMS_URI),
                PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    return cursorToSmsLogEntity(c)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }
    
    /**
     * Reads SMS messages from multiple senders since given timestamp
     */
    fun readMessagesBySendersSince(senders: List<String>, fromTimestamp: Long): List<SmsLogEntity> {
        val messages = mutableListOf<SmsLogEntity>()
        
        if (senders.isEmpty()) return messages
        
        try {
            val placeholders = senders.joinToString(",") { "?" }
            val selection = "${Telephony.Sms.ADDRESS} IN ($placeholders) AND ${Telephony.Sms.DATE} >= ?"
            val selectionArgs = senders.toTypedArray() + fromTimestamp.toString()
            val sortOrder = "${Telephony.Sms.DATE} DESC"
            
            val cursor = context.contentResolver.query(
                Uri.parse(SMS_URI),
                PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val smsEntity = cursorToSmsLogEntity(c)
                    if (smsEntity != null) {
                        messages.add(smsEntity)
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return messages
    }
    
    /**
     * Reads all SMS messages from telecom and telebirr senders in the last specified days
     */
    fun readTelecomAndTelebirrMessages(daysBack: Int = 7): List<SmsLogEntity> {
        val fromTimestamp = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
        val senders = listOf("251994", "*830*", "830")
        
        return readMessagesBySendersSince(senders, fromTimestamp)
    }
    
    /**
     * Checks if READ_SMS permission is granted
     */
    fun hasReadSmsPermission(): Boolean {
        return try {
            context.contentResolver.query(
                Uri.parse(SMS_URI),
                arrayOf(Telephony.Sms._ID),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 1"
            )?.use { true } ?: false
        } catch (e: SecurityException) {
            false
        }
    }
    
    private fun cursorToSmsLogEntity(cursor: Cursor): SmsLogEntity? {
        return try {
            val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
            val dateSentIndex = cursor.getColumnIndex(Telephony.Sms.DATE_SENT)
            
            if (addressIndex == -1 || bodyIndex == -1 || dateIndex == -1) {
                return null
            }
            
            val sender = cursor.getString(addressIndex) ?: return null
            val body = cursor.getString(bodyIndex) ?: return null
            val date = cursor.getLong(dateIndex)
            val dateSent = if (dateSentIndex != -1) cursor.getLong(dateSentIndex) else date
            
            // Use the more accurate timestamp (dateSent if available, otherwise date)
            val timestamp = if (dateSent > 0) dateSent else date
            
            SmsLogEntity(
                sender = sender,
                body = body,
                receivedAt = timestamp,
                parsed = false, // Will be determined during processing
                language = null,
                errorMessage = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
