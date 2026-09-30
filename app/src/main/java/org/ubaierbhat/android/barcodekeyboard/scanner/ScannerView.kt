package org.ubaierbhat.android.barcodekeyboard.scanner

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.ubaierbhat.android.barcodekeyboard.R
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyView
import java.util.concurrent.ExecutionException

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
    private var running = false
    private var torchOn = false

    private var onClose: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

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

    fun setCallbacks(onClose: () -> Unit, onError: (String) -> Unit) {
        this.onClose = onClose
        this.onError = onError
    }

    fun start(context: Context) {
        if (running) {
            return
        }
        running = true
        showPreviewState()
        val appContext = context.applicationContext
        val providerFuture = ProcessCameraProvider.getInstance(appContext)
        providerFuture.addListener(
            {
                if (!running) {
                    return@addListener
                }
                try {
                    val provider = providerFuture.get()
                    cameraProvider = provider
                    bindPreview(provider)
                } catch (error: ExecutionException) {
                    fail()
                } catch (error: InterruptedException) {
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
        torchOn = false
        torchKey.isKeyValueChecked = false
        camera?.cameraControl?.enableTorch(false)
        camera = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        showPreviewState()
    }

    private fun bindPreview(provider: ProcessCameraProvider) {
        val owner = ScannerLifecycleOwner()
        owner.start()
        lifecycleOwner = owner
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        try {
            camera = provider.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
            )
        } catch (error: IllegalArgumentException) {
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
}
