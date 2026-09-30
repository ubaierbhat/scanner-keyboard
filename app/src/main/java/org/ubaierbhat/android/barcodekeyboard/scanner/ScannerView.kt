package org.ubaierbhat.android.barcodekeyboard.scanner

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.ubaierbhat.android.barcodekeyboard.R
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyView
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val previewView: PreviewView
    private val errorView: TextView
    private val hintView: TextView
    private val closeKey: KeyView
    private val torchKey: KeyView

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var lifecycleOwner: ScannerLifecycleOwner? = null
    private var analysisExecutor: ExecutorService? = null
    private var barcodeAnalyzer: BarcodeAnalyzer? = null
    private var running = false
    private var session = 0
    private var torchOn = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private var onClose: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onBarcodeResult: ((String) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.scanner_view, this)
        previewView = findViewById(R.id.scanner_preview)
        errorView = findViewById(R.id.scanner_error)
        hintView = findViewById(R.id.scanner_hint)
        closeKey = findViewById(R.id.scanner_close_key)
        torchKey = findViewById(R.id.scanner_torch_key)
        closeKey.onPress = { onClose?.invoke() }
        torchKey.onPress = { toggleTorch() }
    }

    fun setCallbacks(
        onClose: () -> Unit,
        onError: (String) -> Unit,
        onBarcodeResult: (String) -> Unit,
    ) {
        this.onClose = onClose
        this.onError = onError
        this.onBarcodeResult = onBarcodeResult
    }

    fun start(context: Context) {
        if (running) {
            return
        }
        running = true
        session++
        val currentSession = session
        showPreviewState()
        analysisExecutor = Executors.newSingleThreadExecutor()
        barcodeAnalyzer = BarcodeAnalyzer(ScanThrottle()) { text ->
            mainHandler.post { onBarcodeResult?.invoke(text) }
        }
        val appContext = context.applicationContext
        val providerFuture = ProcessCameraProvider.getInstance(appContext)
        providerFuture.addListener(
            {
                if (!running || currentSession != session) {
                    return@addListener
                }
                try {
                    val provider = providerFuture.get()
                    cameraProvider = provider
                    bindPreview(provider)
                } catch (error: ExecutionException) {
                    Log.e(TAG, "camera provider unavailable", error)
                    fail()
                } catch (error: InterruptedException) {
                    Log.e(TAG, "camera provider wait interrupted", error)
                    Thread.currentThread().interrupt()
                    fail()
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun stop() {
        if (!running) {
            return
        }
        running = false
        session++
        torchOn = false
        torchKey.isKeyValueChecked = false
        camera?.cameraControl?.enableTorch(false)
        camera = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        barcodeAnalyzer?.close()
        barcodeAnalyzer = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
        showPreviewState()
    }

    private fun bindPreview(provider: ProcessCameraProvider) {
        val owner = ScannerLifecycleOwner()
        owner.start()
        lifecycleOwner = owner
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val executor = analysisExecutor ?: return
        val analyzer = barcodeAnalyzer ?: return
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor, analyzer)
        try {
            camera = provider.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "camera bind failed", error)
            fail()
        }
    }

    private fun toggleTorch() {
        val camera = camera ?: return
        torchOn = !torchOn
        torchKey.isKeyValueChecked = torchOn
        camera.cameraControl.enableTorch(torchOn)
    }

    private fun fail() {
        errorView.visibility = VISIBLE
        previewView.visibility = GONE
        hintView.visibility = GONE
        onError?.invoke(context.getString(R.string.scanner_camera_error))
    }

    private fun showPreviewState() {
        previewView.visibility = VISIBLE
        hintView.visibility = VISIBLE
        errorView.visibility = GONE
    }

    private companion object {
        private const val TAG = "ScannerView"
    }
}
