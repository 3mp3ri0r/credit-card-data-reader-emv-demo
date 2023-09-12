package com.example.greetingcard

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greetingcard.ui.theme.GreetingCardTheme
import com.github.devnied.emvnfccard.exception.CommunicationException
import com.github.devnied.emvnfccard.model.EmvCard
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.github.devnied.emvnfccard.parser.IProvider
import java.io.IOException


class MainActivity : ComponentActivity() {
    // Create provider
    private val provider: Provider = Provider()
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null
    private var pendingIntent: PendingIntent? = null
    private var rawString: String = ""
    private val logName = "NfcActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(logName, "<------------------------------- HERE1!")

        val intent = Intent(this, javaClass).apply {
            Log.i(logName, "<------------------------------- HERE2!")
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        Log.i(logName, "<------------------------------- HERE3!")
        pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_MUTABLE)

        Log.i(logName, "<------------------------------- HERE4!")
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            Log.i(logName, "<------------------------------- HERE5!")
            try {
                Log.i(logName, "<------------------------------- HERE6!")
                addDataType("*/*")    /* Handles all MIME based dispatches.
                                 You should specify only the ones that you need. */
                Log.i(logName, "<------------------------------- HERE7!")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                Log.i(logName, "<------------------------------- HERE8!")
                throw RuntimeException("fail", e)
            }
        }

        Log.i(logName, "<------------------------------- HERE9!")
        intentFiltersArray = arrayOf(ndef)

        Log.i(logName, "<------------------------------- HERE10!")
        techListsArray = arrayOf(arrayOf<String>(NfcA::class.java.name))
        Log.i(logName, NfcA::class.java.name+"<------------------------------- HERE20!")

        setContent {
            GreetingCardTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting(rawString)
                }
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    public override fun onNewIntent(intent: Intent) {
        Log.i(logName, "<------------------------------- HERE11!")
        super.onNewIntent(intent)
        Log.i(logName, "<------------------------------- HERE12!")
        val tagFromIntent: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        Log.i(logName, "<------------------------------- HERE13!")
        provider.setmTagCom(IsoDep.get(tagFromIntent))
        Log.i(logName, "<------------------------------- HERE14!")

        // Define config
        val config: EmvTemplate.Config = EmvTemplate.Config()
            .setContactLess(true) // Enable contact less reading (default: true)
            .setReadAllAids(true) // Read all aids in card (default: true)
            .setReadTransactions(true) // Read all transactions (default: true)
            .setReadCplc(false) // Read and extract CPCLC data (default: false)
            .setRemoveDefaultParsers(false) // Remove default parsers for GeldKarte and EmvCard (default: false)
            .setReadAt(true) // Read and extract ATR/ATS and description

        Log.i(logName, "<------------------------------- HERE15!")
        // Create Parser
        val parser = EmvTemplate.Builder() //
            .setProvider(provider) // Define provider
            .setConfig(config) // Define config
            //.setTerminal(terminal) (optional) you can define a custom terminal implementation to create APDU
            .build()

        Log.i(logName, "<------------------------------- HERE16!")
        // Read card
        provider.connect()
        val card: EmvCard = parser.readEmvCard()
        Log.i(logName, card.toString())
        Log.i(logName, "<------------------------------- HERE17!")
//        val track1 = card.track1
        Log.i(logName, "<------------------------------- HERE18!")
//        rawString = String(track1.raw)
//        rawString = track1.cardNumber
        rawString = String(card.track2!!.raw)
        provider.close()
        Log.i(logName, "<------------------------------- HERE19!")
        Log.i(logName, rawString)
    }
}

@Composable
fun Greeting(data: String, modifier: Modifier = Modifier) {
    Surface(color = Color.Cyan) {
        Text(
            text = "$data!",
            modifier = modifier.padding(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GreetingCardTheme {
        Greeting("Christo")
    }
}

class Provider : IProvider {
    private var mTagCom: IsoDep? = null

    @Throws(CommunicationException::class)
    override fun transceive(pCommand: ByteArray): ByteArray {
        Log.i("PROVIDER", "<------------------------------- HERE30!")
//        mTagCom!!.connect()
        mTagCom!!.timeout = 5000
        Log.i("PROVIDER", "<------------------------------- HERE31!")
        val response: ByteArray = try {
            // send command to emv card
            mTagCom!!.transceive(pCommand)
        } catch (e: IOException) {
            throw CommunicationException(e.message)
        }
        Log.i("PROVIDER", "<------------------------------- HERE32!")
//        mTagCom!!.close()
        Log.i("PROVIDER", "<------------------------------- HERE33!")
        return response
    }

    override fun getAt(): ByteArray {
        // For NFC-A
        return mTagCom!!.historicalBytes
        // For NFC-B
        // return mTagCom.getHiLayerResponse();
    }

    fun setmTagCom(mTagCom: IsoDep?) {
        this.mTagCom = mTagCom
    }

    fun connect() {
        this.mTagCom!!.connect()
    }

    fun close() {
        this.mTagCom!!.close()
    }
}
