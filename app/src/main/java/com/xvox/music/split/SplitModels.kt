package com.xvox.music.split

import android.net.Uri
import com.xvox.music.core.model.Song

enum class SplitStatus { QUEUED, DECODING, SEPARATING, READY, CANCELLED, FAILED }
data class SplitTrack(
    val id: Long, val title: String, val artist: String, val source: String, val artwork: String?,
    val duration: Long, val sizeBytes: Long = 0,
    val status: SplitStatus = SplitStatus.QUEUED, val progress: Float = 0f,
    val fileName: String? = null, val saved: Boolean = false, val error: String? = null
) {
    fun song() = Song(id, title, artist, Uri.parse(source), artwork?.let(Uri::parse), duration, sizeBytes)
    companion object {
        fun from(song: Song) = SplitTrack(song.id, song.title, song.artist, song.contentUri.toString(), song.artworkUri?.toString(), song.duration, song.sizeBytes)
    }
}
data class SplitState(
    val initialized: Boolean = false, val requested: Boolean = false, val running: Boolean = false,
    val active: Boolean = false, val autoPaused: Boolean = false, val modelReady: Boolean = false,
    val modelProgress: Float = 0f, val stage: String = "Not set up", val tracks: List<SplitTrack> = emptyList(),
    val queueIds: List<Long> = emptyList(), val normalIds: Set<Long> = emptySet(),
    val gateIds: List<Long> = emptyList(), val currentId: Long? = null,
    val readyCount: Int = 0, val processingId: Long? = null, val processingProgress: Float = 0f,
    val byId: Map<Long, SplitTrack> = emptyMap(), val readyTracks: Map<Long, SplitTrack> = emptyMap(),
    val savedTracks: List<SplitTrack> = emptyList(),
    val showPill: Boolean = true, val noticeId: Long = 0, val notice: String = ""
) {
    val ready: Int get() = readyCount
    val total: Int get() = queueIds.size
    val busy: Int get() = if (running && processingId != null) 1 else 0
}
object SplitModel {
    const val NAME = "UVR MDX-Net 9482"
    const val URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/source-separation-models/UVR_MDXNET_9482.onnx"
    const val SHA256 = "9d78f8566fa8198065214ab628be1de966a500c57786695aa4b13e2b27a7727d"
    const val BYTES = 29704738L
    const val SAMPLE_RATE = 44100
    const val STEM_FLAG = "xvox_split_stem_pair"
    const val MODEL_FILE = "uvr-9482.onnx"
    const val CACHE_LIMIT_BYTES = 768L * 1024 * 1024
}
