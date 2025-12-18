package com.example.mynewsmobileappfe.feature.news.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

class LinkHceService : HostApduService() {

    companion object {
        private const val TAG = "LinkHceService"

        // AID (고정)
        private val AID: ByteArray = hexToBytes("F0010203040506")

        // 상태 코드
        private val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SW_COND_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())

        private val currentArticleIdRef = AtomicReference<Long?>(null)

        fun isSendModeOn(): Boolean = currentArticleIdRef.get() != null

        // ⚠️ UI에서 직접 호출 금지. HceServiceManager에서만 호출!
        fun startSending(articleId: Long) {
            currentArticleIdRef.set(articleId)
            Log.d(TAG, "startSending() articleId=$articleId")
        }

        // ⚠️ UI에서 직접 호출 금지. HceServiceManager에서만 호출!
        fun stopSending() {
            Log.d(TAG, "stopSending()")
            currentArticleIdRef.set(null)
        }

        private fun isSelectAid(apdu: ByteArray): Boolean {
            // SELECT: 00 A4 04 00 Lc [AID...]
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

        // ✅ 기기별로 00CA0000 뒤에 Le(00)가 붙기도 해서 prefix로 판단
        private fun isGetArticleId(apdu: ByteArray): Boolean {
            if (apdu.size < 4) return false
            return apdu[0] == 0x00.toByte() &&
                    apdu[1] == 0xCA.toByte() &&
                    apdu[2] == 0x00.toByte() &&
                    apdu[3] == 0x00.toByte()
        }

        private fun hexToBytes(s: String): ByteArray {
            val clean = s.replace(" ", "")
            val out = ByteArray(clean.length / 2)
            var i = 0
            while (i < clean.length) {
                out[i / 2] = ((Character.digit(clean[i], 16) shl 4) +
                        Character.digit(clean[i + 1], 16)).toByte()
                i += 2
            }
            return out
        }

        private fun bytesToHex(bytes: ByteArray?): String {
            if (bytes == null) return "null"
            return bytes.joinToString(" ") { String.format("%02X", it) }
        }
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val apdu = commandApdu ?: return SW_NOT_FOUND

        Log.d(TAG, "APDU = ${bytesToHex(apdu)}")

        // 1) SELECT는 항상 성공 (연결 안정화)
        if (isSelectAid(apdu)) {
            Log.d(TAG, "SELECT AID matched -> 9000")
            return SW_SUCCESS
        }

        // 2) GET에서만 articleId 전송
        if (isGetArticleId(apdu)) {
            val articleId = currentArticleIdRef.get()
            if (articleId == null) {
                Log.w(TAG, "GET rejected: sendModeOff (articleId=null)")
                return SW_COND_NOT_SATISFIED
            }
            val payload = articleId.toString().toByteArray(Charsets.UTF_8)
            Log.d(TAG, "GET_ARTICLE_ID -> sending '$articleId'")
            return payload + SW_SUCCESS
        }

        return SW_NOT_FOUND
    }

    override fun onDeactivated(reason: Int) {
        // 연결 끊김은 자주 발생. 여기서 stopSending() 하면 체감이 더 불안정해질 수 있음.
        Log.d(TAG, "onDeactivated(reason=$reason)")
    }
}
