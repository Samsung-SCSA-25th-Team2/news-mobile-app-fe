package com.example.mynewsmobileappfe.feature.news.nfc

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class NfcReaderActivity : Activity() {

    private val TAG = "NfcReaderActivity"

    private val aidBytes: ByteArray = hexToBytes("F0010203040506")
    private val apduGetArticleId: ByteArray = hexToBytes("00CA0000")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate, intent=$intent, action=${intent.action}, data=${intent.data}")
        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent, intent=$intent, action=${intent?.action}, data=${intent?.data}")
        if (intent != null) {
            setIntent(intent) // ✅ 중요: 새 intent를 Activity에 반영
            handleNfcIntent(intent)
        }
    }

    private fun handleNfcIntent(intent: Intent) {

        // ✅ 내가 송신모드면 절대 수신 시도하지 않음 (송신폰에서 “수신 실패” 토스트 방지)
        if (LinkHceService.isSendModeOn()) {
            Log.d(TAG, "Ignore NFC read because sendModeOn=true")
            finish()
            return
        }

        val action = intent.action
        Log.d(TAG, "handleNfcIntent, action=$action, data=${intent.data}")

        if (action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            action != NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            Log.w(TAG, "Not an NFC intent -> finish()")
            finish()
            return
        }

        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        Log.d(TAG, "Tag=$tag, techList=${tag?.techList?.joinToString()}")

        if (tag == null) {
            failAndFinish("Tag is null (EXTRA_TAG 없음)")
            return
        }

        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            failAndFinish("IsoDep is null (ISO-DEP 태그/HCE 아님)")
            return
        }

        try {
            isoDep.connect()
            isoDep.timeout = 2000 // ✅ 앱이 꺼져있을 때 부팅/전환 때문에 타임아웃이 짧으면 끊기기 쉬움
            Log.d(TAG, "IsoDep connected (timeout=${isoDep.timeout})")

            // 1) SELECT AID
            val selectApdu = buildSelectAidApdu(aidBytes)
            Log.d(TAG, "Sending SELECT AID: ${bytesToHex(selectApdu)}")
            val selectResp = isoDep.transceive(selectApdu)
            Log.d(TAG, "SELECT response: ${bytesToHex(selectResp)}")

            if (!isSuccess(selectResp)) {
                val sw = selectResp.takeLast(2).toByteArray()
                val swHex = bytesToHex(sw)

                if (swHex == "69 85") {
                    failAndFinish("상대가 송신 모드가 아니에요. (SW=6985)")
                } else {
                    failAndFinish("송신 폰을 찾지 못했어요. (SW=$swHex)")
                }
                return
            }

            // 2) 기사 ID 요청
            Log.d(TAG, "Sending GET_ARTICLE_ID APDU: ${bytesToHex(apduGetArticleId)}")
            val idResp = isoDep.transceive(apduGetArticleId)
            Log.d(TAG, "GET_ARTICLE_ID response: ${bytesToHex(idResp)}")

            if (!isSuccess(idResp)) {
                failAndFinish("기사 ID 수신 실패 (APDU 실패)")
                return
            }

            val dataBytes = idResp.copyOfRange(0, idResp.size - 2)
            val idStr = dataBytes.toString(Charsets.UTF_8).trim()
            Log.d(TAG, "Raw idStr='$idStr'")

            val articleId = idStr.toLongOrNull()
            Log.d(TAG, "Parsed articleId=$articleId")

            if (articleId == null) {
                failAndFinish("잘못된 기사 ID: '$idStr'")
                return
            }

            val next = Intent(this, com.example.mynewsmobileappfe.MainActivity::class.java).apply {
                putExtra("nfc_article_id", articleId)

                // 앱 켜져있든 꺼져있든 동일하게 동작하도록
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            Log.d(TAG, "Launching MainActivity with nfc_article_id=$articleId")
            startActivity(next)
            Log.d(TAG, "startActivity(next) done")

        } catch (e: Exception) {
            Log.e(TAG, "NFC 통신 오류", e)
            Toast.makeText(this, "NFC 통신 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
            finish()
        }
    }

    private fun failAndFinish(msg: String) {
        Log.w(TAG, msg)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun buildSelectAidApdu(aid: ByteArray): ByteArray {
        val header = byteArrayOf(
            0x00.toByte(),
            0xA4.toByte(),
            0x04.toByte(),
            0x00.toByte(),
            aid.size.toByte()
        )
        return header + aid
    }

    private fun isSuccess(resp: ByteArray?): Boolean {
        if (resp == null || resp.size < 2) return false
        val sw1 = resp[resp.size - 2]
        val sw2 = resp[resp.size - 1]
        return (sw1 == 0x90.toByte() && sw2 == 0x00.toByte())
    }

    private fun hexToBytes(s: String): ByteArray {
        val clean = s.replace(" ", "")
        val out = ByteArray(clean.length / 2)
        var i = 0
        while (i < clean.length) {
            out[i / 2] = ((Character.digit(clean[i], 16) shl 4)
                    + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return out
    }

    private fun bytesToHex(bytes: ByteArray?): String {
        if (bytes == null) return "null"
        return bytes.joinToString(" ") { String.format("%02X", it) }
    }
}
