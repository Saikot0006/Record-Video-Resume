package com.example.cameraapplication

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapplication.databinding.FragmentVideoCaptureBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class VideoCaptureFragment : Fragment() {

    private lateinit var binding : FragmentVideoCaptureBinding
    companion object{
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
    private lateinit var sharedViewModel: SharedViewModel
    private var videoCapture : VideoCapture<Recorder>? = null
    private var recording : Recording? = null
    private lateinit var cameraExecutor : ExecutorService
    private lateinit var cameraSelector : CameraSelector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVideoCaptureBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun onClick() {
        binding.cameraChangeButton.setOnClickListener {
            Log.d("CameraSelector", "onClick: CameraSelector")
            if(cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA){
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                startCamera()
            }else{
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                startCamera()
            }
        }
        binding.videoCaptureButton.setOnClickListener { captureVideo() }
    }

    private fun initView() {
        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        sharedViewModel.isPermissionGranted.observe(viewLifecycleOwner){
            if(it) startCamera()
        }
        onClick()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try{
                val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
                val preview = buildPreviewUseCase()
                // Video
                val recorder = recordAudio()
                videoCapture = VideoCapture.withOutput(recorder)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector,preview,videoCapture)

            }catch (e:Exception){}

        },ContextCompat.getMainExecutor(requireContext()))
    }

    private fun buildPreviewUseCase(): Preview {
        return Preview.Builder().build().also { it.setSurfaceProvider(binding.videoPreviewView.surfaceProvider) }
    }

    private fun recordAudio() : Recorder{
        return Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            )
            .build()
    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return
        binding.videoCaptureButton.isEnabled = false

        var curRecording = recording
        if(curRecording!=null){
            curRecording.stop()
            recording = null
            return
        }

        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + "_video.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val customDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                customDirectory?.let { dir ->
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    put(MediaStore.MediaColumns.DATA, File(dir, name).toString())
                }
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireActivity().contentResolver,MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(requireContext(),mediaStoreOutputOptions)
            .apply {
                if(PermissionChecker.checkSelfPermission(
                        requireContext(),android.Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED){
                    withAudioEnabled()
                }
            }.start(ContextCompat.getMainExecutor(requireContext())){recordEvent ->

                when(recordEvent){
                    is VideoRecordEvent.Start -> {
                        Log.d("hello", "captureVideo: hello")
                        binding.videoCaptureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if(!recordEvent.hasError()){
                            val msg = "Video capture succeeded ${recordEvent.outputResults.outputUri}"
                            Log.d("msg", "captureVideo: $msg")
                            Log.d("msg", "captureVideo: $name")
                        }else{
                            recording?.close()
                            recording = null
                        }
                        binding.videoCaptureButton.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }

                val start = recordEvent.recordingStats
                val size = start.numBytesRecorded / 100
                val time = TimeUnit.NANOSECONDS.toSeconds(start.recordedDurationNanos)
                binding.videoTime.text = time.toString()
            }


    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}