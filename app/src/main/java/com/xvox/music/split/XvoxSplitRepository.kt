package com.xvox.music.split

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AtomicFile
import androidx.core.content.ContextCompat
import com.xvox.music.core.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/** Small persistent catalogue. Audio and model bytes never enter DataStore or media-item bundles. */
object XvoxSplitRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generation = AtomicLong()
    private val _state = MutableStateFlow(SplitState())
    val state = _state.asStateFlow()
    private var app: Context? = null
    private val skips = ArrayDeque<Long>()
    private var lastManualId: Long? = null
    private var persistJob: Job? = null
    var allowMeteredDownload: Boolean = false
        private set

    @Synchronized fun initialize(context: Context) {
        if (app != null) return
        app = context.applicationContext
        scope.launch {
            val entries = runCatching {
                val array = JSONArray(AtomicFile(File(directory(), "index.json")).openRead().bufferedReader().use { it.readText() })
                List(array.length()) { i ->
                    val j = array.getJSONObject(i)
                    val name = j.optString("file").takeIf { it.matches(Regex("[a-zA-Z0-9_.-]+")) }
                    val ready = name != null && File(directory(), name).isFile
                    SplitTrack(j.getLong("id"), j.optString("title"), j.optString("artist"), j.getString("source"),
                        j.optString("art").takeIf { it.isNotBlank() }, j.optLong("duration"), j.optLong("size"),
                        if (ready) SplitStatus.READY else SplitStatus.CANCELLED, if (ready) 1f else 0f, if (ready) name else null,
                        ready && j.optBoolean("saved"))
                }
            }.getOrDefault(emptyList())
            val retained = entries.mapNotNull { it.fileName }.toSet()
            directory().listFiles()?.forEach { file ->
                if (file.extension in setOf("part", "f32") || (file.name.startsWith("pair_") && file.extension == "wav" && file.name !in retained)) file.delete()
            }
            _state.update { recompute(it.copy(initialized = true, tracks = entries, modelReady = File(directory(), SplitModel.MODEL_FILE).length() == SplitModel.BYTES)) }
        }
    }
    fun directory(): File = File(checkNotNull(app).filesDir, "xvox_split").apply { mkdirs() }
    fun modelFile(): File = File(directory(), SplitModel.MODEL_FILE)
    fun currentGeneration(): Long = generation.get()
    fun valid(run: Long, id: Long? = null): Boolean = generation.get() == run && _state.value.running &&
        (id == null || _state.value.byId[id]?.status !in setOf(SplitStatus.CANCELLED, SplitStatus.FAILED))

    fun start(context: Context, songs: List<Song>, currentId: Long?, allowMobile: Boolean) {
        initialize(context)
        if (!_state.value.initialized) { notice("XvoxSplit storage is loading. Try again in a moment."); return }
        if (songs.isEmpty()) { notice("Play or select a queue before setting up XvoxSplit"); return }
        allowMeteredDownload = allowMobile
        val index = songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        val ordered = (songs.drop(index) + songs.take(index)).distinctBy { it.id }
        val ids = ordered.map { it.id }
        val run = generation.incrementAndGet()
        synchronized(skips) { skips.clear(); lastManualId = currentId }
        _state.update { old ->
            val existing = old.tracks.associateBy { it.id }
            val fresh = ordered.map { song ->
                val item = existing[song.id]
                if (item?.status == SplitStatus.READY && readyFile(item) != null && (item.saved ||
                    (item.source == song.contentUri.toString() && (song.sizeBytes == 0L || item.sizeBytes == 0L || item.sizeBytes == song.sizeBytes)))) item
                else SplitTrack.from(song)
            }
            recompute(old.copy(requested = true, running = true, autoPaused = false, queueIds = ids, currentId = currentId,
                gateIds = ids.take(2), normalIds = emptySet(), tracks = fresh + old.tracks.filter { it.id !in ids && it.saved }, stage = "Preparing first ${minOf(2, ids.size)} tracks"))
        }
        persist()
        // Prefer the foreground service; if Android refuses it, run the identical pipeline inside
        // the app instead of failing outright. XvoxSplit no longer depends on the service starting.
        val started = runCatching {
            ContextCompat.startForegroundService(context, Intent(context, XvoxSplitService::class.java).setAction(XvoxSplitService.START).putExtra("run", run))
        }.isSuccess
        if (!started) {
            notice("Running XvoxSplit inside XVOX; keep the app open")
            XvoxSplitPipeline.runInProcess(context, run)
        }
    }

    /** Fetch the separation model on its own, with no service and no queue involved. */
    fun downloadModel(context: Context, allowMobile: Boolean) {
        initialize(context)
        allowMeteredDownload = allowMobile
        XvoxSplitPipeline.downloadModel(context, allowMobile)
    }
    fun stop(message: String = "XvoxSplit off; prepared versions are kept") {
        generation.incrementAndGet()
        _state.update { old -> recompute(old.copy(requested = false, running = false, active = false, processingId = null, stage = "Paused",
            tracks = old.tracks.map { if (it.status in setOf(SplitStatus.QUEUED, SplitStatus.DECODING, SplitStatus.SEPARATING)) it.copy(status = SplitStatus.CANCELLED) else it })) }
        XvoxSplitPipeline.cancelInProcess()
        app?.let { runCatching { it.stopService(Intent(it, XvoxSplitService::class.java)) } }
        notice(message); persist()
    }
    fun onTrackChanged(id: Long?, manual: Boolean) {
        if (id == null) return
        val before = _state.value
        if (!before.requested && !before.running) return
        if (!manual && before.currentId == id) return
        if (manual) synchronized(skips) {
            if (lastManualId != id) {
                val now = android.os.SystemClock.elapsedRealtime(); lastManualId = id
                while (skips.isNotEmpty() && now - skips.first() > 4000) skips.removeFirst()
                skips.addLast(now)
                if (skips.size >= 3) {
                    stop("XvoxSplit paused after rapid track changes. Use Normal or enable it again when settled.")
                    _state.update { it.copy(autoPaused = true) }
                    return
                }
            }
        }
        _state.update { old ->
            val index = old.queueIds.indexOf(id)
            if (index < 0) old.copy(currentId = id) else {
                val order = old.queueIds.drop(index) + old.queueIds.take(index)
                recompute(old.copy(currentId = id, queueIds = order))
            }
        }
    }
    fun updateQueue(songs: List<Song>, currentId: Long?) {
        val before = _state.value
        if ((!before.requested && !before.running) || songs.isEmpty()) return
        val at = songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        val ordered = (songs.drop(at) + songs.take(at)).distinctBy { it.id }
        val ids = ordered.map { it.id }
        if (ids == before.queueIds) return
        _state.update { old ->
            val known = old.tracks.mapTo(HashSet()) { it.id }
            val added = ordered.filter { it.id !in known }.map { SplitTrack.from(it).let { item ->
                if (old.running) item else item.copy(status = SplitStatus.CANCELLED, error = "Start preparation to process this track")
            } }
            recompute(old.copy(queueIds = ids, tracks = old.tracks + added, currentId = currentId,
                gateIds = if (!old.active && old.running) ids.take(2) else old.gateIds))
        }
    }
    fun nextTask(): SplitTrack? {
        val s = _state.value
        return s.queueIds.asSequence().mapNotNull { id -> s.byId[id]?.takeIf { it.status == SplitStatus.QUEUED } }.firstOrNull()
    }
    fun update(id: Long, status: SplitStatus, progress: Float = 0f, error: String? = null, file: File? = null) {
        val wasActive = _state.value.active
        val existing = _state.value.byId[id]
        if (existing?.status == status && status in setOf(SplitStatus.DECODING, SplitStatus.SEPARATING)) {
            _state.update { it.copy(processingId = id, processingProgress = progress.coerceIn(0f, 1f)) }
            return
        }
        _state.update { old -> recompute(old.copy(processingId = if (status in setOf(SplitStatus.DECODING, SplitStatus.SEPARATING)) id else null,
            processingProgress = progress, tracks = old.tracks.map {
            if (it.id == id && !(it.status == SplitStatus.CANCELLED && status in setOf(SplitStatus.DECODING, SplitStatus.SEPARATING, SplitStatus.READY))) it.copy(status = status, progress = progress.coerceIn(0f, 1f), error = error, fileName = file?.name ?: it.fileName) else it
        })) }
        if (!wasActive && _state.value.active) notice("XvoxSplit is ready for prepared tracks")
        if (status in setOf(SplitStatus.READY, SplitStatus.FAILED, SplitStatus.CANCELLED)) persist()
    }
    fun updateFor(run: Long, id: Long, status: SplitStatus, progress: Float = 0f, error: String? = null, file: File? = null) {
        if (generation.get() == run) update(id, status, progress, error, file)
    }
    fun modelProgress(value: Float, text: String) { _state.update { it.copy(modelProgress = value, stage = text) } }
    fun modelNeedsDownload() { _state.update { it.copy(modelReady = false, modelProgress = 0f, stage = "Model download required") } }
    fun modelReady() { _state.update { it.copy(modelReady = true, modelProgress = 1f, stage = "Separating queue") } }
    fun finished() { _state.update { recompute(it.copy(running = false, stage = "Queue preparation complete")) }; persist() }
    fun cancel(id: Long) {
        _state.update { old -> recompute(old.copy(gateIds = old.gateIds.filterNot { it == id }, normalIds = old.normalIds + id,
            tracks = old.tracks.map { if (it.id == id && it.status != SplitStatus.READY) it.copy(status = SplitStatus.CANCELLED) else it })) }
        notice("Preparation cancelled for this track"); persist()
    }
    fun useNormal(id: Long) {
        val item = _state.value.tracks.firstOrNull { it.id == id }
        scope.launch {
            val available = item == null || runCatching { app!!.contentResolver.openAssetFileDescriptor(Uri.parse(item.source), "r")?.use { true } ?: false }.getOrDefault(false)
            if (available) { _state.update { it.copy(normalIds = it.normalIds + id) }; notice("Normal playback selected") }
            else notice("The original file is unavailable; prepared audio was kept")
        }
    }
    fun useSplit(id: Long, resetRapidGuard: Boolean = false) {
        if (_state.value.autoPaused && !resetRapidGuard) { notice("XvoxSplit is paused after rapid skips. Enable it again from its task menu."); return }
        if (_state.value.tracks.firstOrNull { it.id == id }?.let(::readyFile) == null) { notice("This track is not ready yet"); return }
        if (resetRapidGuard || !_state.value.requested) synchronized(skips) { skips.clear(); lastManualId = id }
        _state.update { recompute(it.copy(requested = true, autoPaused = false, gateIds = listOf(id), normalIds = it.normalIds - id)) }
        notice("XvoxSplit selected")
    }
    fun save(id: Long, value: Boolean = true) {
        _state.update { recompute(it.copy(tracks = it.tracks.map { item -> if (item.id == id && item.status == SplitStatus.READY) item.copy(saved = value) else item })) }
        persist(); notice(if (value) "Added to XvoxSplit" else "Removed from saved XvoxSplit")
    }
    fun removePrepared(id: Long) {
        useNormal(id)
        val item = _state.value.tracks.firstOrNull { it.id == id } ?: return
        // Don't unlink an actively read file; a later maintenance pass can reclaim it safely.
        _state.update { recompute(it.copy(tracks = it.tracks.filterNot { row -> row.id == id })) }; persist()
        scope.launch { delay(1500); readyFile(item)?.delete() }
    }
    fun readyFile(item: SplitTrack): File? = item.fileName?.let { File(directory(), it) }?.takeIf { it.isFile && it.length() > 44 }
    fun resolve(song: Song): Uri? {
        val s = _state.value
        if (!s.active || song.id in s.normalIds) return null
        val item = s.readyTracks[song.id] ?: return null
        if (item.source != song.contentUri.toString() || (song.sizeBytes > 0 && item.sizeBytes > 0 && song.sizeBytes != item.sizeBytes)) return null
        return readyFile(item)?.let(Uri::fromFile)
    }
    fun preparedFor(id: Long): Boolean = _state.value.readyTracks[id]?.let(::readyFile) != null
    fun notice(message: String) { _state.update { it.copy(noticeId = it.noticeId + 1, notice = message) } }
    fun showPill(show: Boolean) { _state.update { it.copy(showPill = show) } }
    fun evictCache() {
        val s = _state.value
        val protected = (s.queueIds.take(2) + s.gateIds + listOfNotNull(s.currentId)).toSet()
        val cached = s.tracks.filter { it.status == SplitStatus.READY && !it.saved }
        var bytes = cached.sumOf { readyFile(it)?.length() ?: 0 }
        for (item in cached) {
            if (bytes <= SplitModel.CACHE_LIMIT_BYTES) break
            if (item.id in protected) continue
            val file = readyFile(item) ?: continue
            bytes -= file.length(); file.delete()
            _state.update { old -> recompute(old.copy(tracks = old.tracks.map { if (it.id == item.id) it.copy(status = SplitStatus.CANCELLED, fileName = null, error = "Cache reclaimed; prepare again if needed") else it })) }
        }
    }
    private fun recompute(s: SplitState): SplitState {
        val byId = s.tracks.associateBy { it.id }
        val ready = byId.filterValues { it.status == SplitStatus.READY }
        val gates = s.gateIds.filter { id -> byId[id]?.status !in setOf(null, SplitStatus.CANCELLED, SplitStatus.FAILED) }
        val active = s.requested && !s.autoPaused && gates.isNotEmpty() && gates.all { it in ready }
        return s.copy(active = active, byId = byId, readyTracks = ready, savedTracks = ready.values.filter { it.saved },
            readyCount = s.queueIds.count { it in ready }, processingId = s.processingId?.takeIf { byId[it]?.status in setOf(SplitStatus.DECODING, SplitStatus.SEPARATING) })
    }
    @Synchronized private fun persist() {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(80)
            val array = JSONArray()
            _state.value.tracks.forEach { item -> array.put(JSONObject().put("id", item.id).put("title", item.title).put("artist", item.artist)
                .put("source", item.source).put("art", item.artwork.orEmpty()).put("duration", item.duration).put("size", item.sizeBytes)
                .put("saved", item.saved).put("file", item.fileName.orEmpty())) }
            val atomic = AtomicFile(File(directory(), "index.json")); var stream: java.io.FileOutputStream? = null
            try { stream = atomic.startWrite(); stream.write(array.toString().toByteArray()); atomic.finishWrite(stream) }
            catch (_: Exception) { if (stream != null) atomic.failWrite(stream) }
        }
    }
}
