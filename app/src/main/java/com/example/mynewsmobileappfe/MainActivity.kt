package com.example.mynewsmobileappfe

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.mynewsmobileappfe.core.jwt.TokenManager
import com.example.mynewsmobileappfe.core.navigation.MainScreen
import com.example.mynewsmobileappfe.core.navigation.Screen
import com.example.mynewsmobileappfe.core.ui.theme.MyNewsMobileAppFETheme
import com.example.mynewsmobileappfe.feature.auth.ui.AuthEffect
import com.example.mynewsmobileappfe.feature.auth.ui.viewmodel.AuthViewModel
import com.example.mynewsmobileappfe.feature.news.nfc.HceServiceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    private var pendingNfcArticleId by mutableStateOf<Long?>(null)

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(this) }
    private val aidBytes: ByteArray = hexToBytes("F0010203040506")
    private val apduGetArticleId: ByteArray = hexToBytes("00CA0000")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updatePendingFromIntent(intent)
        Log.d("MainActivity", "onCreate intent=$intent")

        setContent {
            MyNewsMobileAppFETheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()

                // ✅ NFC로 받은 기사ID가 있으면 상세로 이동
                LaunchedEffect(pendingNfcArticleId) {
                    val nfcId = pendingNfcArticleId ?: return@LaunchedEffect
                    Log.d("MainActivity", "NFC navigate to articleId=$nfcId")

                    navController.navigate(Screen.ArticleDetail.createRoute(nfcId)) {
                        launchSingleTop = true
                    }
                    pendingNfcArticleId = null
                }

                // AuthEffect 처리(기존 유지)
                LaunchedEffect(Unit) {
                    authViewModel.effect.collect { effect ->
                        when (effect) {
                            AuthEffect.NavigateHome -> {
                                if (pendingNfcArticleId != null) return@collect
                                navController.navigate(Screen.Politics.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }

                            AuthEffect.NavigateToAuthScreen -> {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }

                            is AuthEffect.Toast -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    effect.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                // 로그아웃 이벤트(기존 유지)
                LaunchedEffect(Unit) {
                    tokenManager.logoutEvent.collect {
                        Toast.makeText(
                            this@MainActivity,
                            "세션이 만료되었습니다. 다시 로그인해주세요.",
                            Toast.LENGTH_LONG
                        ).show()

                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }

                MainScreen(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updatePendingFromIntent(intent)
        Log.d("MainActivity", "onNewIntent intent=$intent")
    }

    override fun onResume() {
        super.onResume()

        // ✅ 송신 중이면 수신 ReaderMode는 끔 (충돌 방지)
        if (HceServiceManager.isSending()) {
            disableForegroundReaderMode()
            Log.d("NFC", "onResume: Sending=true -> ReaderMode OFF")
        } else {
            enableForegroundReaderMode()
            Log.d("NFC", "onResume: Sending=false -> ReaderMode ON")
        }
    }

    override fun onPause() {
        // ✅ 앱이 백그라운드로 가면 수신 OFF + 송신도 무조건 OFF
        disableForegroundReaderMode()
        HceServiceManager.disableSending(applicationContext)
        Log.d("NFC", "onPause: ReaderMode OFF + Sending OFF")

        super.onPause()
    }

    // ========= 수신 ReaderMode =========
    fun enableForegroundReaderMode() {
        val adapter = nfcAdapter
        if (adapter == null) {
            Log.w("NFC", "NFC not supported (adapter=null)")
            return
        }
        if (!adapter.isEnabled) {
            Log.w("NFC", "NFC is OFF")
            Toast.makeText(this, "NFC가 꺼져있어요. 설정에서 NFC를 켜주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val flags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NFC_A

        adapter.enableReaderMode(
            this,
            { tag ->
                Log.d("NFC", "onTagDiscovered tech=${tag.techList.joinToString()}")

                // 송신 중이면 읽기 무시
                if (HceServiceManager.isSending()) {
                    Log.d("NFC", "ignore read: sending=true")
                    return@enableReaderMode
                }

                val articleId = readArticleIdFromTag(tag)
                Log.d("NFC", "readArticleIdFromTag result=$articleId")

                if (articleId != null && articleId > 0) {
                    runOnUiThread {
                        pendingNfcArticleId = articleId
                        Toast.makeText(this, "NFC 수신: articleId=$articleId", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            flags,
            null
        )

        Log.d("NFC", "enableReaderMode ON")
    }

    fun disableForegroundReaderMode() {
        try {
            nfcAdapter?.disableReaderMode(this)
            Log.d("NFC", "enableReaderMode OFF")
        } catch (e: Exception) {
            Log.w("NFC", "disableReaderMode failed: ${e.message}")
        }
    }

    private fun readArticleIdFromTag(tag: Tag): Long? {
        val isoDep = IsoDep.get(tag) ?: run {
            Log.w("NFC", "IsoDep is null (not ISO-DEP)")
            return null
        }

        try {
            isoDep.connect()
            isoDep.timeout = 2500

            // SELECT AID
            val selectApdu = buildSelectAidApdu(aidBytes)
            val selectResp = isoDep.transceive(selectApdu)
            Log.d("NFC", "SELECT resp=${bytesToHex(selectResp)}")
            if (!isSuccess(selectResp)) return null

            // GET articleId
            val idResp = isoDep.transceive(apduGetArticleId)
            Log.d("NFC", "GET resp=${bytesToHex(idResp)}")
            if (!isSuccess(idResp)) return null

            val dataBytes = idResp.copyOfRange(0, idResp.size - 2)
            val idStr = dataBytes.toString(Charsets.UTF_8).trim()
            return idStr.toLongOrNull()
        } catch (e: Exception) {
            Log.e("NFC", "NFC read error", e)
            return null
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }

    private fun buildSelectAidApdu(aid: ByteArray): ByteArray {
        val header = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte())
        return header + aid
    }

    private fun isSuccess(resp: ByteArray?): Boolean {
        if (resp == null || resp.size < 2) return false
        return resp[resp.size - 2] == 0x90.toByte() && resp[resp.size - 1] == 0x00.toByte()
    }

    private fun updatePendingFromIntent(intent: Intent?) {
        val id = intent?.getLongExtra("nfc_article_id", -1L) ?: -1L
        if (id > 0) pendingNfcArticleId = id
    }

    private fun hexToBytes(s: String): ByteArray {
        val clean = s.replace(" ", "")
        val out = ByteArray(clean.length / 2)
        var i = 0
        while (i < clean.length) {
            out[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return out
    }

    private fun bytesToHex(bytes: ByteArray?): String {
        if (bytes == null) return "null"
        return bytes.joinToString(" ") { String.format("%02X", it) }
    }
}
