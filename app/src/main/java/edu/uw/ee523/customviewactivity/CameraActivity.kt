package edu.uw.ee523.customviewactivity


import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import edu.uw.ee523.customviewactivity.BitmapOps.Companion.bitmapBlur
import edu.uw.ee523.customviewactivity.databinding.ActivityCameraBinding

import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This class uses the CameraX API referred to in class.
 */
class CameraActivity : AppCompatActivity() {
    private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    val MY_TAG = "CountActivity"

    lateinit var binding: ActivityCameraBinding
    val REQUEST_CODE_FOR_PERMISSIONS = 1902

    private val BLUR_AMOUNT = 50

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera)
        binding.buttonTakePicture.setOnClickListener { takePhoto() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Adrienne's note: This is the same as the permissions we saw
        // previously-- the way the codelab sets up the permissions is really nicer though!
        //
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher. You can use either a val, as shown in this snippet,
        // or a lateinit var in your onAttach() or onCreate() method.
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    Log.i(MY_TAG, "permission is granted ")

                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Log.i(MY_TAG, "permission is NOT granted ")
                }
            }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            checkPermissions(requestPermissionLauncher)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted(): Boolean {
        val canUseCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        val canUseStorage = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return canUseCamera == PackageManager.PERMISSION_GRANTED &&
                canUseStorage == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(MY_TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Adrienne's note: Unlike the codelab, here we use the ImageCapturedCallback,
        // instead of the ImageSavedCallback.
        // This allows us to keep the image in memory rather than storing/retrieving from storage.
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(MY_TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    binding.imageView.setImageBitmap(imageProxyToBlurredBitmap(image))
                    binding.imageView.rotation = image.getImageInfo().getRotationDegrees().toFloat()
                    image.close()
                }
            }
        )
    }

    /**
     * Helper method to turn an ImageProxy into a Bitmap
     * Using the blur function to show the blur
     */
    private fun imageProxyToBlurredBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        return bitmapBlur(bitmap, 1.0f, BLUR_AMOUNT)
    }




    fun checkPermissions(requestPermissionLauncher: ActivityResultLauncher<String>) {

        val canUseCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        val canUseStorage = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (canUseCamera != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Log.i(MY_TAG, "I should tell you why I want to use the camera")
            }
        }

        if (canUseStorage != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.i(MY_TAG, "I should tell you why I want to use the external storage")
            }
        }

        if (canUseCamera == PackageManager.PERMISSION_DENIED &&
            canUseStorage == PackageManager.PERMISSION_DENIED
        ) {
            Log.i(MY_TAG, "Don't have permission for either so going to ask for both")
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_CODE_FOR_PERMISSIONS
            )

        } else {
            if (canUseCamera == PackageManager.PERMISSION_DENIED) {
                Log.i(MY_TAG, "Going to ask for permission for camera only")
                // Only request a single permission
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
            if (canUseStorage == PackageManager.PERMISSION_DENIED) {
                Log.i(MY_TAG, "Going to ask for permission for storage only")
//                Log.i(MY_TAG,"Don't have permission so going to ask for it")
                requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }




}


