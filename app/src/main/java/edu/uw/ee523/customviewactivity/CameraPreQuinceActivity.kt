package edu.uw.ee523.customviewactivity

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.mlkit.vision.common.InputImage
import edu.uw.ee523.customviewactivity.BitmapOps.Companion.bitmapBlur
import edu.uw.ee523.customviewactivity.BitmapOps.Companion.doFaceDetection
import edu.uw.ee523.customviewactivity.databinding.ActivityCameraPreQuinceBinding
import java.io.IOException
import java.util.ArrayList

class CameraPreQuinceActivity : AppCompatActivity() {

    val MY_TAG = "CameraPreQuinceActivity"
    private var isLandscape: Boolean = false
    private var imageUri: Uri? = null

    private lateinit var binding:ActivityCameraPreQuinceBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera_pre_quince)
        setTitle("PreQuince Camera")
        setContentView(R.layout.activity_camera_pre_quince)

//        binding.buttonTakePicturePq.setOnClickListener {
//            Log.e(MY_TAG, "Going to call to start camera intent")
//            startCameraIntentForResult()
//        }

        getRuntimePermissions()
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        savedInstanceState?.let {
            imageUri = it.getParcelable(KEY_IMAGE_URI)
        }
    }


    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }


    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putParcelable(KEY_IMAGE_URI, imageUri)
        }
    }

    fun startCameraIntentForResult(view: View) {
//        Log.INFO("CameraPreQuince"
        Log.e(MY_TAG, "Starting camera intent")
//        var previewPane = binding.previewPane
        var previewPane = findViewById<ImageView>(R.id.previewPane)
        // Clean up last time's image
        imageUri = null
        previewPane?.setImageBitmap(null)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    fun startChooseImageIntentForResult(view: View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            tryReloadAndDetectInImage()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data!!.data
            tryReloadAndDetectInImage()
        }
    }

    private fun tryReloadAndDetectInImage() {
        var previewPane = findViewById<ImageView>(R.id.previewPane)
        var imageView = findViewById<ImageView>(R.id.imageView)
        try {
            if (imageUri == null) {
                return
            }
            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri!!)
                ImageDecoder.decodeBitmap(source)
            }
            previewPane?.setImageBitmap(imageBitmap) //previewPane is the ImageView from the layout
            doFaceDetection(InputImage.fromBitmap(imageBitmap, 0))
            imageView?.setImageBitmap(bitmapBlur(imageBitmap, 0.25f, 50))
        } catch (e: IOException) {
        }
    }
    companion object {
        private const val KEY_IMAGE_URI = "edu.uw.eep523.takepicture.KEY_IMAGE_URI"
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE = 1002
        private const val PERMISSION_REQUESTS = 1
    }
}