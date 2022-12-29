package com.affinidi.tfdemoone

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ImageClassifierHelper.ClassifierListener {


    private lateinit var cameraExecutor: ExecutorService
    private var mCameraProvider: ProcessCameraProvider? = null

    private lateinit var viewFinder: PreviewView

    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private lateinit var bitmapBuffer: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewFinder = findViewById(R.id.viewFinder)


        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            imageClassifierListener = this
        )

        if (allPermissionsGranted()) {
            setUpCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun setUpCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        }



//        cameraExecutor = Executors.newSingleThreadExecutor()
//
//
//        analysisUseCase.setAnalyzer(
//            // newSingleThreadExecutor() will let us perform analysis on a single worker thread
//            Executors.newSingleThreadExecutor()
//        ) { imageProxy ->
//            processImageProxy(imageProxy)
//        }
    }

//    @SuppressLint("UnsafeOptInUsageError")
//    private fun processImageProxy(
//        imageProxy: ImageProxy
//    ) {
//        imageProxy.image?.let { image ->
//            if (!::bitmapBuffer.isInitialized) {
//                // The image rotation and RGB image buffer are initialized only once
//                // the analyzer has started running
//                bitmapBuffer = Bitmap.createBitmap(
//                    image.width,
//                    image.height,
//                    Bitmap.Config.ARGB_8888
//                )
//            }
//
//            detectObjects(imageProxy)
//            mCameraProvider?.unbindAll()
//        }
//
//    }

    private fun detectObjects(image: ImageProxy) {

        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees

        // Pass Bitmap and rotation to the object detector helper for processing and detection
        imageClassifierHelper.classify(bitmapBuffer, imageRotation)
//        imageClassifierHelper.

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            mCameraProvider = cameraProvider
            // Preview
            val surfacePreview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(viewFinder.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val imageAnalyzer =
                ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(viewFinder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    // The analyzer can then be assigned to the instance
                    .also {

                        it.setAnalyzer(cameraExecutor) { image ->

                            if (!::bitmapBuffer.isInitialized) {

                                // The image rotation and RGB image buffer are initialized only once
                                // the analyzer has started running
                                bitmapBuffer = Bitmap.createBitmap(
                                    image.width,
                                    image.height,
                                    Bitmap.Config.ARGB_8888
                                )
                            }

                            detectObjects(image)
                        }
                    }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, surfacePreview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Toast.makeText(this, exc.message, Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        imageClassifierHelper.clearImageClassifier()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA
            ).toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {

            if (allPermissionsGranted()) {

                setUpCamera()
            } else {

                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

//    override fun onInitialized() {
//        if (allPermissionsGranted()) {
//            setUpCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
//            )
//        }
//        cameraExecutor = Executors.newSingleThreadExecutor()
//    }

    override fun onError(error: String) {
        runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_SHORT).show() }

    }

    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        runOnUiThread {
            Log.i(
                "resultssss",
                "${results?.get(0)?.categories.toString()} ${results?.toString()}"
            )

//            [<Category "1" (displayName= score=0.99609375 index=1)>] [Classifications{categories=[<Category "1" (displayName= score=0.99609375 index=1)>], headIndex=0}]
        }
    }


}