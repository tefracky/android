package io.homeassistant.companion.android.nfc

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import io.homeassistant.companion.android.BaseActivity
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.util.UrlHandler

class NfcSetupActivity : BaseActivity() {

    // private val viewModel: NfcViewModel by viewModels()
    private lateinit var viewModel: NfcViewModel
    private var mNfcAdapter: NfcAdapter? = null
    private var simpleWrite = false
    private var messageId: Int = -1

    companion object {
        val TAG = NfcSetupActivity::class.simpleName
        const val EXTRA_TAG_VALUE = "tag_value"
        const val EXTRA_MESSAGE_ID = "message_id"

        fun newInstance(context: Context, tagId: String? = null, messageId: Int = -1): Intent {
            return Intent(context, NfcSetupActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE_ID, messageId)
                if (tagId != null)
                    putExtra(EXTRA_TAG_VALUE, tagId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_setup)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        viewModel = ViewModelProvider(this).get(NfcViewModel::class.java)

        intent.getStringExtra(EXTRA_TAG_VALUE)?.let {
            simpleWrite = true
            viewModel.nfcWriteTagEvent.postValue(it)
        }

        messageId = intent.getIntExtra(EXTRA_MESSAGE_ID, -1)
    }

    override fun onResume() {
        super.onResume()
        mNfcAdapter?.let {
            NFCUtil.enableNFCInForeground(it, this, javaClass)
        }
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.let {
            NFCUtil.disableNFCInForeground(it, this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("NFC Debug", "NFC Debug\n\n\n\n\n\n\n\n\n\nStart")
        Log.d("NFC Debug", "intent = $intent")
        Log.d("NFC Debug", ("NfcAdapter.ACTION_TECH_DISCOVERED == intent.action = " + NfcAdapter.ACTION_TECH_DISCOVERED == intent.action).toString())

        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val nfcTagToWriteUUID = viewModel.nfcWriteTagEvent.value
            Log.d("NFC Debug", "nfcTagToWriteUUID = $nfcTagToWriteUUID")

            // Create new nfc tag
            if (nfcTagToWriteUUID == null) {
                Log.d("NFC Debug", "nfcTagToWriteUUID = null")
                val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                Log.d("NFC Debug", "rawMessages = $rawMessages")
                val ndefMessage = rawMessages?.firstOrNull() as NdefMessage?
                Log.d("NFC Debug", "ndefMessage = $ndefMessage")
                val url = ndefMessage?.records?.get(0)?.toUri().toString()
                Log.d("NFC Debug", "url = $url")
                val nfcTagId = UrlHandler.splitNfcTagId(url)
                Log.d("NFC Debug", "nfcTagId = $nfcTagId")
                if (nfcTagId == null) {
                    Log.d("NFC Debug", "nfcTagId = null")
                    Log.w(TAG, "Unable to read tag!")
                    Toast.makeText(this, R.string.nfc_invalid_tag, Toast.LENGTH_LONG).show()
                } else {
                    Log.d("NFC Debug", "nfcTagId != null")
                    val value = viewModel.nfcReadEvent.postValue(nfcTagId)
                    Log.d("NFC Debug", "value = $value")
                }
            } else {
                Log.d("NFC Debug", "nfcTagToWriteUUID != null")
                try {
                    val nfcTagUrl = "https://www.home-assistant.io/tag/$nfcTagToWriteUUID"
                    Log.d("NFC Debug", "nfcTagUrl = $nfcTagUrl")
                    val ret = NFCUtil.createNFCMessage(nfcTagUrl, intent)
                    Log.d("NFC Debug", "ret = $ret")
                    Log.d(TAG, "Wrote nfc tag with url: $nfcTagUrl")
                    val message = R.string.nfc_write_tag_success
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

                    viewModel.nfcReadEvent.value = nfcTagToWriteUUID
                    Log.d("NFC Debug", " viewModel.nfcReadEvent.value = " +  viewModel.nfcReadEvent.value)
                    viewModel.nfcWriteTagDoneEvent.value = nfcTagToWriteUUID
                    Log.d("NFC Debug", " viewModel.nfcWriteTagDoneEvent.value = " +  viewModel.nfcWriteTagDoneEvent.value)
                    // If we are a simple write it means the fontend asked us to write.  This means
                    // we should return the user as fast as possible back to the UI to continue what
                    // they were doing!
                    Log.d("NFC Debug", "nfcTagUrl = $nfcTagUrl")
                    if (simpleWrite) {
                        Log.d("NFC Debug", "simpleWrite = $simpleWrite")
                        val res = setResult(messageId)
                        Log.d("NFC Debug", "res = $res")
                        finish()
                    }
                } catch (e: Exception) {
                    val message = R.string.nfc_write_tag_error
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Unable to write tag.", e)
                }
                Log.d("NFC Debug", "NFC Debug\n\n\n\n\n\n\n\n\n\nEnd")
            }
        }
    }
}
