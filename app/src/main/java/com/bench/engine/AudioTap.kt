package com.bench.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.ln

object AudioTap {
    const val RATE = 44100

    @SuppressLint("MissingPermission")
    fun frames(): Flow<ShortArray> = flow {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(min, 16384)
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return@flow
        }
        rec.startRecording()
        try {
            val buf = ShortArray(4096)
            while (currentCoroutineContext().isActive) {
                val got = rec.read(buf, 0, buf.size)
                if (got > 0) emit(buf.copyOf(got))
            }
        } finally {
            rec.stop()
            rec.release()
        }
    }.flowOn(Dispatchers.IO)
}

object Pitch {
    private val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** Note name with octave, and the offset in cents from the equal-tempered pitch. */
    fun note(hz: Double): Pair<String, Int> {
        if (hz <= 0.0) return Pair("—", 0)
        val midi = 12.0 * ln(hz / 440.0) / ln(2.0) + 69.0
        val r = Math.round(midi).toInt()
        val cents = Math.round((midi - r) * 100.0).toInt()
        return Pair(names[((r % 12) + 12) % 12] + (r / 12 - 1), cents)
    }
}
