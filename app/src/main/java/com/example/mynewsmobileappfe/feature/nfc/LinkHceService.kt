package com.example.mynewsmobileappfe.feature.news.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import android.os.Handler
import android.os.Looper

class LinkHceService : HostApduService() {

    companion object {
        private const val TAG = "LinkHceService"

        fun isSendModeOn(): Boolean = sendModeOn.get()

        private val AID: ByteArray = hexToBytes("F0010203040506")

        private val sendModeOn = AtomicBoolean(false)

        @Volatile
        private var currentArticleId: Long? = null

        fun startSending(articleId: Long) {
            currentArticleId = articleId
            sendModeOn.set(true)
            Log.d(TAG, "startSending() called - articleId=$articleId, sendModeOn=true")
        }

        fun stopSending() {
            Log.d(TAG, "stopSending() called")
            sendModeOn.set(false)
            currentArticleId = null
        }

        // 상태 코드
        private val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SW_COND_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())

        private val APDU_GET_ARTICLE_ID = hexToBytes("00CA0000")

        private fun isSelectAid(apdu: ByteArray): Boolean {
            if (apdu.size < 5) return false
            if (apdu[0] != 0x00.toByte()) return false
            if (apdu[1] != 0xA4.toByte()) return false
            if (apdu[2] != 0x04.toByte()) return false
            if (apdu[3] != 0x00.toByte()) return false

            val lc = apdu[4].toInt() and 0xFF
            if (apdu.size < 5 + lc) return false

            val aidBytes = apdu.copyOfRange(5, 5 + lc)
            return aidBytes.contentEquals(AID)
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

    private val handler = Handler(Looper.getMainLooper())

    private fun stopSendingDelayed(ms: Long = 2000L) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            stopSending()
        }, ms)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val apdu = commandApdu ?: return SW_NOT_FOUND
        Log.d(TAG, "APDU (hex) = ${bytesToHex(apdu)}")

        // ✅ 1) SELECT는 항상 성공 처리 (수신 폰이 “송신폰 못찾음”으로 오판하는 케이스 감소)
        if (isSelectAid(apdu)) {
            Log.d(TAG, "SELECT AID matched -> 9000")
            return SW_SUCCESS
        }

        // ✅ 2) GET에서만 송신모드/ID 체크
        if (apdu.contentEquals(APDU_GET_ARTICLE_ID)) {
            val articleId = currentArticleId
            if (!sendModeOn.get() || articleId == null) {
                Log.w(TAG, "GET rejected: sendModeOff or articleId null")
                return SW_COND_NOT_SATISFIED // 6985
            }
            val payload = articleId.toString().toByteArray(Charsets.UTF_8)
            Log.d(TAG, "GET_ARTICLE_ID -> sending '$articleId'")
            return payload + SW_SUCCESS
        }

        return SW_NOT_FOUND
    }



    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "onDeactivated() called, reason=$reason -> stopSending()")
        stopSending()
    }

}
