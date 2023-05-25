package com.ihsan.chat_attachment_camera_gallery.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context.WINDOW_SERVICE
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.databinding.FragmentCameraBinding
import com.ihsan.chat_attachment_camera_gallery.model.BackState
import com.ihsan.chat_attachment_camera_gallery.utils.Constants
import com.ihsan.chat_attachment_camera_gallery.utils.MyApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs


class CameraFragment : Fragment() {
    private val TAG = "CameraFragment"
    private lateinit var binding: FragmentCameraBinding

    //image capture properties
    private var imageCapture: ImageCapture? = null
    private var imageName = ""

    //camera properties
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraAspectRatio: Int = AspectRatio.RATIO_DEFAULT
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var captureLoadingImageView: ImageView

    //video recording properties
    private var recorder: Recorder? = null
    private var recording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private lateinit var videoUri: Uri
    private lateinit var chronometer: Chronometer
    private lateinit var recordingIndicator: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.hide()
        // Inflate the layout for this fragment
        binding = FragmentCameraBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnCapture = binding.capture
        val btnRecordVideo = binding.recordVideo
        val btnBack = binding.backBtn
        val flipCamera = binding.switchBtn

        cameraAspectRatio = getCameraAspectRatio()
        chronometer = binding.chronometer
        recordingIndicator = binding.recordingIndicator
        captureLoadingImageView = binding.loadingImageView

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        chronometer.stop()
        //setting the camera screen size following camera aspect ratio
        setCameraScreenSize()
        //starting Camera
        startCamera()

        //photo click listener
        btnCapture.setOnClickListener {
            if (flipCamera.visibility == ViewGroup.VISIBLE) {
                animateFlash()
                capturePhoto()
                buttonVisibilityGone()
            }
        }

        //video record start stop click listener
        btnRecordVideo.setOnClickListener {
            if (flipCamera.visibility == ViewGroup.VISIBLE) {
                //start recording
                startStopVideoRecording(it)
                btnRecordVideo.setImageResource(R.drawable.ic_stop)

                flipCamera.visibility = ViewGroup.GONE
                btnCapture.visibility = ViewGroup.GONE
                btnBack.visibility = ViewGroup.GONE
                btnRecordVideo.size = FloatingActionButton.SIZE_NORMAL
            } else {
                //stop recording
                buttonVisibilityGone()
                startStopVideoRecording(it)
                btnRecordVideo.setImageResource(R.drawable.ic_round_videocam)
            }
        }

        // handling back press event and enabling twice back to exit
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBackHome(view)
                }
            })

        //back button click listener
        btnBack.setOnClickListener {
            navigateBackHome(view)
        }

        //flip camera click listener
        flipCamera.setOnClickListener {
            //change the cameraSelector
            flipCamera()
            //restart the camera
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recording?.stop()
        recording?.close()
    }

    private fun navigateBackHome(view: View) {
        (activity as AppCompatActivity).supportActionBar?.show()
        //navigate back to bottomAttachmentOptionFragment
        findNavController(view).navigate(
            R.id.action_cameraFragment_to_bottomAttachmentOptionFragment
        )
    }

    private fun navigateToExoPlayerFragment(view: View, uri: String) {
        val action =
            CameraFragmentDirections.actionCameraFragmentToExoPlayerFragment(BackState.CAMERA, uri)
        findNavController(view).navigate(action)
    }

    private fun buttonVisibilityGone() {
        binding.backBtn.visibility = ViewGroup.GONE
        binding.switchBtn.visibility = ViewGroup.GONE
        binding.capture.visibility = ViewGroup.GONE
        binding.recordVideo.visibility = ViewGroup.GONE
    }

    private fun flipCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun setCameraScreenSize() {
        val currentOrientation = resources.configuration.orientation
        val layoutParams = binding.viewFinder.layoutParams
        val layoutParamsParent = binding.viewFinderFrame.layoutParams
        val display = getDisplayMetrics()
        val width = display.widthPixels.toFloat()
        val height = display.heightPixels.toFloat()
        val ratio = if (cameraAspectRatio == AspectRatio.RATIO_16_9) {
            16f / 9f
        } else {
            4f / 3f
        }
        Toast.makeText(
            MyApplication.instance,
            "Screen H:$height, W:$width, CSR:${(height / width)}",
            Toast.LENGTH_SHORT
        ).show()
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            layoutParams.height = (width * ratio).toInt()
            layoutParamsParent.height = (width * ratio).toInt()
        } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams.width = (height * ratio).toInt()
            layoutParamsParent.width = (height * ratio).toInt()
        }

        binding.viewFinder.layoutParams = layoutParams
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val windowManager = requireContext().getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds

            // Set the width and height in displayMetrics
            displayMetrics.widthPixels = bounds.width()
            displayMetrics.heightPixels = bounds.height()
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            displayMetrics.widthPixels = windowManager.defaultDisplay.width
            displayMetrics.heightPixels = windowManager.defaultDisplay.height
        }
        return displayMetrics
    }

    private fun getCameraAspectRatio(): Int {
        val displayMetrics = getDisplayMetrics()
        val widthPixels = displayMetrics.widthPixels.toFloat()
        val heightPixels = displayMetrics.heightPixels.toFloat()

        // Calculate the aspect ratio
        val aspectRatio = heightPixels / widthPixels

        // Compare aspect ratio to standard ratios
        return if (
            isApproximatelyEqual(aspectRatio, 16f / 9f) ||
            isApproximatelyEqual(aspectRatio, 9f / 16f) ||
            isApproximatelyEqual(aspectRatio, 21f / 9f) ||
            isApproximatelyEqual(aspectRatio, 9f / 21f)
        ) {
            AspectRatio.RATIO_16_9
        } else if (
            isApproximatelyEqual(aspectRatio, 4f / 3f) ||
            isApproximatelyEqual(aspectRatio, 3f / 4f)
        ) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_DEFAULT
        }
    }

    private fun isApproximatelyEqual(a: Float, b: Float, tolerance: Float = 0.18f): Boolean {
        return abs(a - b) <= tolerance
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                //Get Camera Provider from camera provider future
                val cameraProvider = cameraProviderFuture.get()
                val preview =
                    Preview.Builder().setTargetAspectRatio(cameraAspectRatio).build().also { prev ->
                        prev.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                //assigning image capture builder
                imageCapture =
                    ImageCapture.Builder().setTargetAspectRatio(cameraAspectRatio).build()

                //video quality selector
                val qualitySelector = QualitySelector.from(
                    Quality.FHD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
                //video recorder builder
                recorder = Recorder.Builder().setAspectRatio(cameraAspectRatio)
                    .setQualitySelector(qualitySelector).build()
                //assign value to videoCapture include recorder
                videoCapture = VideoCapture.withOutput(recorder!!)

                //Binding camera
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture, videoCapture
                    )
                } catch (e: java.lang.Exception) {
                    Log.d(TAG, "startCamera Fail: $e")
                }
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        val outputOptions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            //for sdk level 29+
            getImageCaptureOutputOptionsQ()
        } else {
            //for sdk level 28 and below
            getImageCaptureOutputOptionsP()
        }

        //capturing image
        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "onImageSaved: $imageName")
                    val directory =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    val photoFile = File(
                        directory, "${resources.getString(R.string.app_name)}/Pictures/${imageName}"
                    )
                    //Navigate to Preview and Crop
                    val action =
                        CameraFragmentDirections.actionCameraFragmentToCameraPreviewFragment(
                            photoFile.toUri().toString(), BackState.CAMERA, true
                        )
                    findNavController(binding.root).navigate(action)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "onError: $exception")
                    Toast.makeText(
                        MyApplication.instance, "Video not saved $exception", Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun startStopVideoRecording(view: View) {
        val videoCapture = this.videoCapture ?: return

        //stop recording if it's running else it will start after this
        if (recording != null) {
            animateFlash()
            //Stop recording here
            stopRecordingTask()
            resumeOrientationRotation()
            return
        }

        //To hold current orientation while recording on going
        fixedCurrentOrientation()

        recording =
            videoCapture.output.prepareRecording(requireContext(), getVideoOutputOptions()).apply {
                // Enable Audio for recording and checking permission before enable
                if (PermissionChecker.checkSelfPermission(
                        requireContext(), Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }.start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        startRecordingTask()
                    }

                    is VideoRecordEvent.Pause -> {
                        captureLoadingImageView.visibility = ViewGroup.VISIBLE
                    }

                    is VideoRecordEvent.Status -> {

                    }

                    is VideoRecordEvent.Finalize -> {
                        binding.recordVideo.setImageResource(R.drawable.ic_round_videocam)
                        if (!recordEvent.hasError()) {
                            Toast.makeText(requireContext(), "saved", Toast.LENGTH_SHORT).show()
                            Log.d(
                                TAG,
                                "Video record succeeded: ${recordEvent.outputResults.outputUri}"
                            )

                            navigateToExoPlayerFragment(
                                view, recordEvent.outputResults.outputUri.toString()
                            )
                        } else {
                            stopRecordingTask()
                            resumeOrientationRotation()

                            Toast.makeText(MyApplication.instance, "not saved", Toast.LENGTH_SHORT)
                                .show()
                            Log.e(TAG, "Video record ends with error: ${recordEvent.error}")
                        }
                    }
                }
            }
    }

    private fun startRecordingTask() {
        binding.recordVideo.setImageResource(R.drawable.ic_stop)
        recordingIndicator.visibility = ViewGroup.VISIBLE
        //start time count
        chronometer.visibility = ViewGroup.VISIBLE
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
        //load video recoding indicator
        Glide.with(MyApplication.instance).asGif().load(R.drawable.blinking_red_dot).centerCrop()
            .into(recordingIndicator)
    }

    private fun stopRecordingTask() {
        chronometer.stop()
        chronometer.visibility = ViewGroup.GONE
        recordingIndicator.visibility = ViewGroup.GONE
        //stop recording
        recording!!.stop()
        recording = null
    }

    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 10)

        //show loading animation
        Glide.with(MyApplication.instance).load(R.drawable.progress_animation).fitCenter()
            .error(R.drawable.ic_cancel) // Optional error image
            .into(captureLoadingImageView)
        captureLoadingImageView.visibility = ViewGroup.VISIBLE
    }

    private fun fixedCurrentOrientation() {
        val currentOrientation = resources.configuration.orientation
        // fix the screen rotation temporarily
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun resumeOrientationRotation() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /*OUTPUT DIRECTORIES FOR CAPTURE AND RECORDING*/

    //for sdk level 29+
    private fun getImageCaptureOutputOptionsQ(): ImageCapture.OutputFileOptions {
        imageName = "IMAGE_${
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT, Locale.getDefault()
            ).format(System.currentTimeMillis())
        }.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "DCIM/${resources.getString(R.string.app_name)}/Pictures"
                )
            }
        }

        val resolver = requireContext().contentResolver
        val imageCollectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        return ImageCapture.OutputFileOptions.Builder(resolver, imageCollectionUri, contentValues)
            .build()
    }

    //for sdk level 28 and below
    private fun getImageCaptureOutputOptionsP(): ImageCapture.OutputFileOptions {
        imageName = "IMAGE_${
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT, Locale.getDefault()
            ).format(System.currentTimeMillis())
        }.jpg"

        //add image path to app directory
        val imageDirectory = getAppOutputDirectoryP().let {
            File(it, "Pictures").apply {
                mkdirs()
            }
        }

        val photoFile = File(imageDirectory, imageName)

        return ImageCapture.OutputFileOptions.Builder(photoFile).build()
    }

    //for sdk level 28 and below for camera camera capture
    private fun getAppOutputDirectoryP(): File {
        //getting DCIM folder directory to path
        val dcimPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath

        //adding additional directory and creating if doesn't exist
        return dcimPath.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
    }

    private fun getVideoOutputOptions(): FileOutputOptions {
        val videoName = "VIDEO_" + SimpleDateFormat(
            Constants.FILE_NAME_FORMAT, Locale.getDefault()
        ).format(System.currentTimeMillis()) + ".mp4"

        val videoDirectory = getAppOutputDirectoryP().let {
            File(it, "Videos").apply {
                mkdirs()
            }
        }
        val videoFile = File(videoDirectory, videoName)

        //assigning videoUri
        videoUri = videoFile.toUri()

        // Create the output options for the video capture
        return FileOutputOptions.Builder(videoFile).build()
    }

    //store into application Cache
    /*private fun getOutputOptionsCache(): ImageCapture.OutputFileOptions {
        //path way
        val mediaDir = context?.externalMediaDirs?.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        //Assign value to photoFile
        val photoFile = File(
            mediaDir,
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        //preparing output option for captured photo
        return ImageCapture.OutputFileOptions.Builder(photoFile).build()
    }*//*

        private fun createVideoThumbnail(videoUri: Uri): Bitmap? {
            val retriever = MediaMetadataRetriever()
            if (!videoUri.toFile().exists()) {
                Toast.makeText(requireContext(), "file not exist", Toast.LENGTH_SHORT).show()
                return null
            }
            try {
                retriever.setDataSource(context, videoUri)
                val frame = retriever.getFrameAtTime(1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                return frame?.let { Bitmap.createScaledBitmap(it, 720, 1020, false) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create thumbnail for video $videoUri: ${e.message}")
            } finally {
                retriever.release()
            }
            return null
        }

        private fun insertThumbnailToMediaStore(resolver: ContentResolver, thumbnail: Bitmap?): Uri? {
            // create a new thumbnail image file in the media store.
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME, "thumbnail_${System.currentTimeMillis()}.jpeg"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }

            val thumbnailUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            // write the thumbnail bitmap to the output stream.
            if (thumbnail != null) {
                resolver.openOutputStream(thumbnailUri ?: return null).use {
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
            }
            return thumbnailUri
        }

        @SuppressLint("Range")
        private fun getVideoContentUri(videoUri: Uri): Uri? {
            val filePath = videoUri.path?.let { File(it).absolutePath }
            val cursor = requireContext().contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Video.Media._ID),
                MediaStore.Video.Media.DATA + "=? ",
                arrayOf(filePath),
                null
            )
            cursor?.let {
                if (it.moveToFirst()) {
                    val id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                    val baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    return Uri.withAppendedPath(baseUri, "" + id)
                }
                cursor.close()
            }
            return null

            /*val externalUri=MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val cameraUri = Uri.withAppendedPath(externalUri, "DCIM/Camera")
            return Uri.withAppendedPath(externalUri, "DCIM/Screenshots")*/
        }

        private fun saveVideoThumbnailToMediaStore(videoUri: Uri, context: Context): Uri? {
            Log.d(TAG, "saveVideoThumbnailToMediaStore: $videoUri")
            //creating thumbnail
            val resolver = context.contentResolver
            val thumbnail = createVideoThumbnail(videoUri)
            val thumbnailUri = insertThumbnailToMediaStore(resolver, thumbnail)

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.MINI_THUMB_MAGIC, thumbnailUri.toString())
            }

            getVideoContentUri(videoUri)?.let {
                resolver.update(it, values, null, null)
                Toast.makeText(requireContext(), "Updated $it", Toast.LENGTH_SHORT).show()
            }

            return thumbnailUri
        }*/
}