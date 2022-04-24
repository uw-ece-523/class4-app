package edu.uw.ee523.customviewactivity


import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import edu.uw.ee523.customviewactivity.databinding.ActivityCameraBinding

import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {
    private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    val MY_TAG = "CountActivity"

    lateinit var binding: ActivityCameraBinding
    lateinit var imageUri: Uri
    val REQUEST_IMAGE_CAPTURE = 42
    val REQUEST_CHOOSE_IMAGE = 47
    val REQUEST_CODE_FOR_PERMISSIONS = 1902

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera)


//        var viewBinding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(viewBinding.root)

//        var view = binding.imageView
        binding.buttonTakePicture.setOnClickListener { takePhoto() }

/*
        binding.imageView.setOnTouchListener { v, event ->
            val action = event.action
            when(action){
                MotionEvent.ACTION_DOWN -> {
                    pDownX= event.x.toInt()
                    pDownY= event.y.toInt()
                }

                MotionEvent.ACTION_MOVE -> { }

                MotionEvent.ACTION_UP -> {
                    pUpX= event.x.toInt()
                    pUpY= event.y.toInt()
                }

                MotionEvent.ACTION_CANCEL -> {

                }

                else ->{

                }
            }
            true
        }*/

        // Set up the listeners for take photo and video capture buttons
//        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()

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

        if (allPermissionsGranted()){
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
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
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
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

//        // Set up image capture listener, which is triggered after photo has
//        // been taken
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(MY_TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                override fun
//                        onImageSaved(output: ImageCapture.OutputFileResults){
//                    val msg = "Photo capture succeeded: ${output.savedUri}"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                    Log.d(MY_TAG, msg)
//                }
//            }
//        )

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(MY_TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onCaptureSuccess(image: ImageProxy, ) {
                    super.onCaptureSuccess(image)

                    binding.imageView.setImageBitmap(imageProxyToBitmap(image))
                    binding.imageView.rotation = image.getImageInfo().getRotationDegrees().toFloat()
                    image.close()
                }

            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun gotImage(){
        var bitmapFromCamera: Bitmap

//        // Get Canvas/Draw directly on the Bitmap
//        bitmapFromCamera.drawOval()..
//
//        var view = findViewById<CustomImageView>(R.id.customImageView)
//        view.setImageBitmap(<bitmap from camera>)
    }

    fun startCameraIntentForResult(view: View) {
        // Clean up last time's image
//        imageUri = null
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

//    override fun onActivityResult(code: Int, resultCode: Int, data: Intent) {
//        super.onActivityResult(code, resultCode, data)
//        if (resultCode == Activity.RESULT_OK) {
//            if (code == REQUEST_IMAGE_CAPTURE){
//
//            }
//        }
//
////        super.onActivityResult(requestCode, resultCode, data)
//
//    }


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
            canUseStorage == PackageManager.PERMISSION_DENIED) {
            Log.i(MY_TAG,"Don't have permission for either so going to ask for both")
            requestPermissions(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_FOR_PERMISSIONS)

        } else {
            if (canUseCamera == PackageManager.PERMISSION_DENIED) {
                Log.i(MY_TAG,"Going to ask for permission for camera only")
                // Only request a single permission
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
            if (canUseStorage == PackageManager.PERMISSION_DENIED) {
                Log.i(MY_TAG,"Going to ask for permission for storage only")
//                Log.i(MY_TAG,"Don't have permission so going to ask for it")
                requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }


}


