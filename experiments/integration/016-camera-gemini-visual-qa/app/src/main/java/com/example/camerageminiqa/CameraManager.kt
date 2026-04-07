package com.example.camerageminiqa

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * グラスカメラのキャプチャ管理。
 * ProjectedDeviceContextでグラスの外向きカメラにアクセスし、
 * 640x480解像度でキャプチャ→Base64エンコードしてGeminiに送信可能な形式にする。
 */
@OptIn(ExperimentalProjectedApi::class)
class CameraManager(private val activity: ComponentActivity) {

    companion object {
        private const val TAG = "CameraManager"
        private const val CAPTURE_WIDTH = 640
        private const val CAPTURE_HEIGHT = 480
        private const val JPEG_QUALITY = 80
    }

    /** キャプチャ結果 */
    sealed class CaptureResult {
        data class Success(val base64Image: String, val mimeType: String = "image/jpeg") : CaptureResult()
        data class Failure(val message: String) : CaptureResult()
    }

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isInitialized = false

    /**
     * カメラを初期化してバインド。
     * ProjectedDeviceContextを使ってグラスの外向きカメラにアクセスする。
     */
    fun initialize(onResult: (Boolean) -> Unit) {
        try {
            val projectedContext = ProjectedContext.createProjectedDeviceContext(activity)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(projectedContext)

            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    if (!provider.hasCamera(cameraSelector)) {
                        Log.e(TAG, "No back camera available on glasses")
                        onResult(false)
                        return@addListener
                    }

                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(CAPTURE_WIDTH, CAPTURE_HEIGHT),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                            )
                        ).build()

                    imageCapture = ImageCapture.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()

                    provider.unbindAll()
                    provider.bindToLifecycle(activity, cameraSelector, imageCapture!!)
                    isInitialized = true
                    onResult(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bind camera", e)
                    onResult(false)
                }
            }, ContextCompat.getMainExecutor(activity))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to create projected device context", e)
            onResult(false)
        }
    }

    /**
     * 画像をキャプチャしてBase64エンコードされた文字列として返す。
     * Gemini LiveセッションにInlineDataとして送信可能。
     */
    fun captureImage(onResult: (CaptureResult) -> Unit) {
        val capture = imageCapture
        if (capture == null || !isInitialized) {
            onResult(CaptureResult.Failure("Camera not initialized"))
            return
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        val base64 = imageProxyToBase64(imageProxy)
                        imageProxy.close()
                        onResult(CaptureResult.Success(base64))
                    } catch (e: Exception) {
                        imageProxy.close()
                        Log.e(TAG, "Failed to process captured image", e)
                        onResult(CaptureResult.Failure("Image processing failed: ${e.message}"))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exception)
                    onResult(CaptureResult.Failure("Capture failed: ${exception.message}"))
                }
            }
        )
    }

    /**
     * ImageProxyをBase64エンコードされたJPEG文字列に変換。
     */
    private fun imageProxyToBase64(imageProxy: ImageProxy): String {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // YUV→Bitmap→JPEG→Base64変換
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: run {
                // JPEGでない場合はバイトデータをそのままBase64化
                return Base64.encodeToString(bytes, Base64.NO_WRAP)
            }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        bitmap.recycle()

        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * リソースを解放。onDestroyで必ず呼ぶこと。
     */
    fun release() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing camera", e)
        }
        imageCapture = null
        cameraProvider = null
        isInitialized = false
    }
}
