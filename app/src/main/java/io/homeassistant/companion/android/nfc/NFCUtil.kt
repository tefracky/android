package io.homeassistant.companion.android.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import io.homeassistant.companion.android.BuildConfig
import java.io.IOException

object NFCUtil {
    @Throws(Exception::class)
    fun createNFCMessage(url: String, intent: Intent?): Boolean {
        Log.d("NFC Debug", "createNFCMessage")
        Log.d("NFC Debug", "url = $url")
        Log.d("NFC Debug", "intent = ${intent.toString()}")
        val nfcRecord = NdefRecord.createUri(url)
        Log.d("NFC Debug", "nfcRecord = $nfcRecord")
        val applicationRecord = NdefRecord.createApplicationRecord(BuildConfig.APPLICATION_ID)
        Log.d("NFC Debug", "applicationRecord = $applicationRecord")
        val nfcMessage = NdefMessage(arrayOf(nfcRecord, applicationRecord))
        Log.d("NFC Debug", "nfcMessage = $nfcMessage")
        val nfcFallbackMessage = NdefMessage(arrayOf(nfcRecord))
        Log.d("NFC Debug", "nfcFallbackMessage = $nfcFallbackMessage")
        intent?.let {
            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            Log.d("NFC Debug", "Start writing Tag")
            return writeMessageToTag(nfcMessage, nfcFallbackMessage, tag)
        }
        return false
    }

    fun disableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity) {
        Log.d("NFC Debug", "disableNFCInForeground")
        Log.d("NFC Debug", "nfcAdapter = $nfcAdapter")
        Log.d("NFC Debug", "activity = $activity")
        val disableForegroundDispatch = nfcAdapter.disableForegroundDispatch(activity)
        Log.d("NFC Debug", "disableForegroundDispatch = $disableForegroundDispatch")
    }

    fun <T> enableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity, classType: Class<T>) {
        Log.d("NFC Debug", "enableNFCInForeground")
        Log.d("NFC Debug", "nfcAdapter = $nfcAdapter")
        Log.d("NFC Debug", "activity = $activity")
        Log.d("NFC Debug", "classType = $classType")
        val pendingIntent = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, classType).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
        Log.d("NFC Debug", "pendingIntent = $pendingIntent")
        val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        Log.d("NFC Debug", "nfcIntentFilter = $nfcIntentFilter")
        val filters = arrayOf(nfcIntentFilter)
        Log.d("NFC Debug", "filters = $filters")

        val techLists =
            arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))
        Log.d("NFC Debug", "techLists = $techLists")
        val enableForegroundDispatch = nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
        Log.d("NFC Debug", "enableForegroundDispatch = $enableForegroundDispatch")
    }

    @Throws(Exception::class)
    private fun writeMessageToTag(
        nfcMessage: NdefMessage,
        fallbackMessage: NdefMessage,
        tag: Tag?
    ): Boolean {
        Log.d("NFC Debug", "enableNFCInForeground")
        Log.d("NFC Debug", "nfcMessage = $nfcMessage")
        Log.d("NFC Debug", "fallbackMessage = $fallbackMessage")
        Log.d("NFC Debug", "tag = $tag")

        val nDefTag: Ndef? = Ndef.get(tag)
        Log.d("NFC Debug", "nDefTag = $nDefTag")

        nDefTag?.let {
            Log.d("NFC Debug", "Start writing nfc tag")
            val connected = it.connect()
            Log.d("NFC Debug", "connected = $connected")
            var messageToWrite = nfcMessage
            Log.d("NFC Debug", "messageToWrite = $messageToWrite")
            Log.d("NFC Debug", "it.maxSize = " + it.maxSize)
            Log.d("NFC Debug", "nfcMessage.toByteArray().size = " + nfcMessage.toByteArray().size)
            if (it.maxSize < nfcMessage.toByteArray().size) {
                Log.d("NFC Debug", "Tag size to small")
                messageToWrite = fallbackMessage
                Log.d("NFC Debug", "messageToWrite = $messageToWrite")
            }
            Log.d("NFC Debug", "fallbackMessage.toByteArray().size = " + fallbackMessage.toByteArray().size)
            if (it.maxSize < fallbackMessage.toByteArray().size) {
                Log.d("NFC Debug", "Tag size to small for fallBackMessage")
                // Message to large to write to NFC tag
                throw Exception("Message is too large")
            }
            Log.d("NFC Debug", "it.isWritable = " + it.isWritable)
            return if (it.isWritable) {
                Log.d("NFC Debug", "Tag writable")
                val write = it.writeNdefMessage(messageToWrite)
                Log.d("NFC Debug", "write = $write")
                val close = it.close()
                Log.d("NFC Debug", "close = $close")
                // Message is written to tag
                true
            } else {
                throw Exception("NFC tag is read-only")
            }
        }

        val nDefFormatableTag = NdefFormatable.get(tag)
        Log.d("NFC Debug", "nDefFormatableTag = $nDefFormatableTag")

        nDefFormatableTag?.let {
            try {
                val connected = it.connect()
                Log.d("NFC Debug", "connected = $connected")
                val format = it.format(nfcMessage)
                Log.d("NFC Debug", "format = $format")
                val close = it.close()
                Log.d("NFC Debug", "close = $close")
                // The data is written to the tag
            } catch (e: IOException) {
                // Failed to format tag
                throw Exception("Failed to format tag", e)
            }
        }
        return true
    }
}
