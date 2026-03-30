package com.ismartcoding.plain.ai

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.preferences.AiImageSearchEnabledPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class SemanticSearchResult(val imageId: String, val score: Float)

object ImageSearchManager {
    private const val MODEL_DIR_NAME = "ai_models"
    private const val IMAGE_MODEL = "mobileclip_s2_image.tflite"
    private const val TEXT_MODEL = "mobileclip_s2_text.tflite"
    private const val TOKENIZER = "tokenizer.json"
    private const val MIN_SCORE = 0.15f

    private const val HF_BASE =
        "https://huggingface.co/plainhub/mobileclip-s2-tflite/resolve/main"
    private val MODEL_FILES = listOf(
        ModelFile("$HF_BASE/$IMAGE_MODEL", IMAGE_MODEL, 144_120_668L),
        ModelFile("$HF_BASE/$TEXT_MODEL", TEXT_MODEL, 253_874_828L),
        ModelFile("$HF_BASE/$TOKENIZER", TOKENIZER, 1_708_304L),
    )

    private val _status = MutableStateFlow(ImageSearchStatus.UNAVAILABLE)
    val status = _status.asStateFlow()
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()
    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()
    @Volatile private var downloadCancelled = false

    private val modelsDir: File get() = File(MainApp.instance.filesDir, MODEL_DIR_NAME)

    fun getModelDir(): String = modelsDir.absolutePath

    fun isModelReady(): Boolean = _status.value == ImageSearchStatus.READY

    fun isModelAvailable(): Boolean {
        val dir = modelsDir
        return File(dir, IMAGE_MODEL).exists() &&
            File(dir, TEXT_MODEL).exists() &&
            File(dir, TOKENIZER).exists()
    }

    fun totalModelSize(): Long = MODEL_FILES.sumOf { it.size }

    suspend fun restoreIfEnabled() = withContext(Dispatchers.IO) {
        val enabled = AiImageSearchEnabledPreference.getAsync(MainApp.instance)
        if (enabled && isModelAvailable()) {
            loadModels()
        }
    }

    suspend fun enable() = withContext(Dispatchers.IO) {
        if (_status.value == ImageSearchStatus.DOWNLOADING ||
            _status.value == ImageSearchStatus.LOADING
        ) return@withContext
        if (!isModelAvailable()) {
            download()
            if (!isModelAvailable()) return@withContext
        }
        loadModels()
        AiImageSearchEnabledPreference.putAsync(MainApp.instance, true)
    }

    suspend fun disable() = withContext(Dispatchers.IO) {
        ImageEmbedHelper.close()
        TextEmbedHelper.close()
        DelegateHelper.closeAll()
        modelsDir.deleteRecursively()
        AppDatabase.instance.imageEmbeddingDao().deleteAll()
        _status.value = ImageSearchStatus.UNAVAILABLE
        AiImageSearchEnabledPreference.putAsync(MainApp.instance, false)
        emitStatus()
    }

    fun cancelDownload() {
        downloadCancelled = true
        _status.value = ImageSearchStatus.UNAVAILABLE
        _downloadProgress.value = 0
        modelsDir.deleteRecursively()
        emitStatus()
    }

    suspend fun search(query: String, limit: Int = 50): List<SemanticSearchResult> =
        withContext(Dispatchers.IO) {
            val textEmb = TextEmbedHelper.embed(query) ?: return@withContext emptyList()
            val dao = AppDatabase.instance.imageEmbeddingDao()
            val all = dao.getAll()
            all.mapNotNull { vec ->
                val imgEmb = bytesToFloats(vec.embedding)
                if (hasInvalidValues(imgEmb)) return@mapNotNull null
                val score = dotProduct(textEmb, imgEmb)
                if (score >= MIN_SCORE) SemanticSearchResult(vec.id, score) else null
            }.sortedByDescending { it.score }.take(limit)
        }

    private suspend fun download() {
        _status.value = ImageSearchStatus.DOWNLOADING
        _downloadProgress.value = 0
        downloadCancelled = false
        emitStatus()
        val dir = modelsDir
        dir.mkdirs()
        val totalSize = MODEL_FILES.sumOf { it.size }
        var downloaded = 0L
        try {
            for (f in MODEL_FILES) {
                if (downloadCancelled) return
                downloaded = downloadFile(f.url, File(dir, f.filename), downloaded, totalSize)
            }
            if (downloadCancelled) return
            _downloadProgress.value = 100
            emitStatus()
        } catch (e: Exception) {
            LogCat.e("Model download failed", e)
            dir.deleteRecursively()
            _status.value = ImageSearchStatus.ERROR
            _errorMessage.value = e.message ?: "Download failed"
            emitStatus()
        }
    }

    private fun loadModels() {
        try {
            _status.value = ImageSearchStatus.LOADING
            emitStatus()
            val dir = modelsDir
            ImageEmbedHelper.init(File(dir, IMAGE_MODEL))
            TextEmbedHelper.init(File(dir, TEXT_MODEL), File(dir, TOKENIZER))
            _status.value = ImageSearchStatus.READY
            _errorMessage.value = ""
            emitStatus()
            LogCat.d("MobileCLIP-S2 loaded successfully")
        } catch (e: Exception) {
            LogCat.e("Model load failed", e)
            _status.value = ImageSearchStatus.ERROR
            _errorMessage.value = e.message ?: "Load failed"
            emitStatus()
        }
    }

    private fun downloadFile(
        url: String, dest: File, startBytes: Long, totalSize: Long,
    ): Long {
        var downloaded = startBytes
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        conn.inputStream.use { input ->
            FileOutputStream(dest).use { output ->
                val buf = ByteArray(8192)
                var len: Int
                while (input.read(buf).also { len = it } != -1) {
                    if (downloadCancelled) { dest.delete(); return downloaded }
                    output.write(buf, 0, len)
                    downloaded += len
                    _downloadProgress.value = (downloaded * 100 / totalSize).toInt().coerceIn(0, 99)
                }
            }
        }
        return downloaded
    }

    private fun emitStatus() {
        sendEvent(ImageSearchStatusChangedEvent(_status.value, _downloadProgress.value, _errorMessage.value))
    }

    private data class ModelFile(val url: String, val filename: String, val size: Long)
}
