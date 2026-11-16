package com.vladiko.voicebridge

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import okhttp3.*
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.net.URLEncoder

interface SpeechInput {
    fun start()
    fun finish()
    fun cancel()
}

// Audio goes through the already paired, certificate-pinned relay. Claude OAuth never
// leaves the PC. One instance owns one stream; all listener calls run on the UI thread.
class RemoteSpeechRecognizer(
    private val client: OkHttpClient,
    relayUrl: String,
    token: String,
    chatName: String,
    private val muted: () -> Boolean,
    private val listener: Listener,
    private val source: PcmSource = AndroidPcmSource(Cfg.micVoiceComm)
) : SpeechInput {
    interface Listener {
        fun onReady()
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onDone()
        fun onError(code: String)
    }
    interface PcmSource {
        fun start()
        fun read(buffer: ByteArray): Int
        fun stop()
        fun close()
    }
    private val main = Handler(Looper.getMainLooper())
    private val url = relayUrl.trimEnd('/').replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") + "/sub?role=stt&token=" + encode(token) + "&name=" + encode(chatName)
    @Volatile private var socket: WebSocket? = null
    @Volatile private var ended = false
    @Volatile private var finishing = false
    @Volatile private var recording = false
    private var started = false
    private var ready = false
    private var worker: Thread? = null
    private val connectTimeout = Runnable { fail("connect_failed") }
    private val finishTimeout = Runnable { fail("finish_timeout") }

    override fun start() {
        if (started || ended) return
        started = true
        main.postDelayed(connectTimeout, 15000)
        socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                main.post {
                    if (ended) return@post
                    try {
                        val event = JSONObject(text)
                        when (event.optString("type")) {
                            "ready" -> if (!ready) {
                                ready = true
                                main.removeCallbacks(connectTimeout)
                                if (!finishing) capture() else sendFinish()
                            }
                            "partial" -> listener.onPartial(event.getString("text"))
                            "final" -> listener.onFinal(event.getString("text"))
                            "done" -> { end(); listener.onDone() }
                            "error" -> fail(event.optString("code", "transcription_failed"))
                        }
                    } catch (_: Exception) { fail("protocol") }
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                main.post { fail("connection_lost") }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                main.post { if (!ended) fail("connection_lost") }
            }
        })
    }

    private fun capture() {
        if (worker != null || ended || finishing) return
        recording = true
        worker = Thread({
            try {
                source.start()
                main.post { if (!ended && !finishing) listener.onReady() }
                val buffer = ByteArray(3200) // 100 ms, PCM16LE, 16 kHz, mono
                while (recording && !ended) {
                    val n = source.read(buffer)
                    if (!recording || ended) break
                    if (n <= 0 || n % 2 != 0) throw IllegalStateException("microphone")
                    // Keep the connection alive with silence during our own spoken cues.
                    if (muted()) buffer.fill(0, 0, n)
                    val ws = socket ?: break
                    if (ws.queueSize() > 512 * 1024 || !ws.send(buffer.toByteString(0, n))) {
                        main.post { fail("connection_lost") }; break
                    }
                }
            } catch (_: Exception) {
                if (recording && !ended) main.post { fail("microphone") }
            } finally {
                source.close()
                main.post { if (finishing && !ended) sendFinish() }
            }
        }, "ClAudio-speech").apply { start() }
    }

    override fun finish() {
        if (ended || finishing) return
        finishing = true
        recording = false
        source.stop()
        // Finalize only after the capture thread has sent its last frame.
        if (worker == null && ready) sendFinish()
        main.postDelayed(finishTimeout, 8000)
    }
    private fun sendFinish() {
        if (socket?.send("{\"type\":\"finish\"}") != true) fail("connection_lost")
    }
    private fun fail(code: String) {
        if (ended) return
        end(); listener.onError(code)
    }
    private fun end() {
        if (ended) return
        ended = true; recording = false
        main.removeCallbacks(connectTimeout); main.removeCallbacks(finishTimeout)
        source.stop()
        socket?.close(1000, null)
        if (worker == null) source.close()
    }
    override fun cancel() { end(); socket?.cancel() }

    companion object {
        private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    }
}

private class AndroidPcmSource(private val voiceComm: Boolean) : RemoteSpeechRecognizer.PcmSource {
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var stopped = false
    override fun start() {
        if (stopped) return
        val size = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(size > 0)
        val r = AudioRecord(if (voiceComm) MediaRecorder.AudioSource.VOICE_COMMUNICATION else MediaRecorder.AudioSource.VOICE_RECOGNITION,
            16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(size, 12800))
        recorder = r
        check(r.state == AudioRecord.STATE_INITIALIZED)
        if (!stopped) r.startRecording()
    }
    override fun read(buffer: ByteArray): Int = recorder?.read(buffer, 0, buffer.size) ?: -1
    override fun stop() { stopped = true; try { recorder?.stop() } catch (_: Exception) {} }
    override fun close() { try { recorder?.release() } catch (_: Exception) {}; recorder = null }
}
