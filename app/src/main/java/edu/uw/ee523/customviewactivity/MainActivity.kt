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
import edu.uw.ee523.customviewactivity.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding:ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.buttonTakePicture.setOnClickListener { startCameraActivity() }

        binding.buttonStartSensors.setOnClickListener { startSensorActivity() }
    }

    fun startCameraActivity() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q){
            // Use old-school camera approach
            val intent = Intent(this, CameraPreQuinceActivity::class.java)
            startActivity(intent)
        } else{
            // Use CameraX
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

    }

    fun startSensorActivity() {
        val intent = Intent(this, SensorClassActivity::class.java)
        startActivity(intent)
    }



}


