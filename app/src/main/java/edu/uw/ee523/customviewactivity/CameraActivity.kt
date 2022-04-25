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

        return bitmapBlur(bitmap, 1.0f, 100)
    }


    /**
     * The idea of a blur is that for every pixel in an image,
     * we average the value of the current pixel with the pixels
     * around it. The larger the radius, the more the blur.
     *
     */
    fun bitmapBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        var sentBitmap = sentBitmap
        val width = Math.round(sentBitmap.width * scale)
        val height = Math.round(sentBitmap.height * scale)
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)

        val bitmap = sentBitmap.copy(sentBitmap.config, true)
        if (radius < 1) {
            return null
        }
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
//        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val width_max = w - 1
        val height_max = h - 1
        val totalNumPixels = w * h
        val div = radius + radius + 1

        // Create an array for each of red, green and blue
        val r = IntArray(totalNumPixels)
        val g = IntArray(totalNumPixels)
        val b = IntArray(totalNumPixels)

        // Keep track of the sums
        var red_sum: Int
        var green_sum: Int
        var blue_sum: Int
        var x: Int
        var y: Int

        var i: Int
        var curPixel: Int

        var yp: Int
        var yi: Int
        var yw: Int

        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1 // shr = shift right
        divsum *= divsum

        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        yw = yi

        // Use a stack to keep track of pixels during the process
        val stack = Array(div) { IntArray(3) } // A stack of 3-element ints (R, G, B)
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int

        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        // First, handle y
        y = 0
        while (y < h) {
            blue_sum = 0
            green_sum = 0
            red_sum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0

            // Look at pixels in the radius
            i = -radius
            while (i <= radius) {
                curPixel = pix[yi + Math.min(width_max, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = curPixel and 0xff0000 shr 16 // Get the R val
                sir[1] = curPixel and 0x00ff00 shr 8  // Get the G val
                sir[2] = curPixel and 0x0000ff        // Get the B val
                rbs = r1 - Math.abs(i)  // The closer to the edge of the radius, the less it contributes
                red_sum += sir[0] * rbs
                green_sum += sir[1] * rbs
                blue_sum += sir[2] * rbs
                if (i > 0) { // "going in" vs "going out" of the pixel
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[red_sum]
                g[yi] = dv[green_sum]
                b[yi] = dv[blue_sum]
                red_sum -= routsum
                green_sum -= goutsum
                blue_sum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, width_max)
                }
                curPixel = pix[yw + vmin[x]]
                sir[0] = curPixel and 0xff0000 shr 16
                sir[1] = curPixel and 0x00ff00 shr 8
                sir[2] = curPixel and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                red_sum += rinsum
                green_sum += ginsum
                blue_sum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            blue_sum = 0
            green_sum = 0
            red_sum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0

            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                red_sum += r[yi] * rbs
                green_sum += g[yi] * rbs
                blue_sum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < height_max) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] =
                    -0x1000000 and pix[yi] or (dv[red_sum] shl 16) or (dv[green_sum] shl 8) or dv[blue_sum]
                red_sum -= routsum
                green_sum -= goutsum
                blue_sum -= boutsum
                stackstart = stackpointer - radius + div

                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, height_max) * w
                }
                curPixel = x + vmin[y]
                sir[0] = r[curPixel]
                sir[1] = g[curPixel]
                sir[2] = b[curPixel]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                red_sum += rinsum
                green_sum += ginsum
                blue_sum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
//        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
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


