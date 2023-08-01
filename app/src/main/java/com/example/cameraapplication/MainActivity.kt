package com.example.cameraapplication

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapplication.databinding.ActivityMainBinding
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    companion object{
        private lateinit var viewModel: SharedViewModel
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ).apply {
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
  //  private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater,null,false)
        setContentView(binding.root)
        initData()
       // cameraExecutor = Executors.newSingleThreadExecutor()
      //  requestPermission()


    }

    private fun initData() {
        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        if(!allPermissionsGranted()){
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }else{
            viewModel.setPermission(allPermissionsGranted())
        }
    }

    /*private fun requestPermission() {
        requestCameraPermissionIfMessing{ granted ->
            if(granted){
              //  startCamera()
            }else{
                Toast.makeText(this, "Please Allow the Permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestCameraPermissionIfMessing(onResult: (Boolean) -> Unit) {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED){
            onResult(true)
        }else{
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                onResult(it)
            }.launch(android.Manifest.permission.CAMERA)
        }
    }*/

    /*private fun startCamera() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this)
        processCameraProvider.addListener({
            try {
                val cameraProvider = processCameraProvider.get()
                val previewUseCase = buildPreviewUseCase()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,previewUseCase)
            }catch (e:Exception){
                Toast.makeText(this, "Error starting the camera", Toast.LENGTH_SHORT).show()
            }
        },ContextCompat.getMainExecutor(this))
    }

    private fun buildPreviewUseCase(): Preview {
        return Preview.Builder().build().also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }
    }*/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            viewModel.setPermission(allPermissionsGranted())
        }
    }

    fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(this,it) == PackageManager.PERMISSION_GRANTED
    }


}