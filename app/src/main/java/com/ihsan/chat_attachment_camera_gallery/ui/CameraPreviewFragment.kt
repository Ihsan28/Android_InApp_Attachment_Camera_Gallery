package com.ihsan.chat_attachment_camera_gallery.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.databinding.FragmentCameraPreviewBinding
import com.ihsan.chat_attachment_camera_gallery.model.BackState
import com.ihsan.chat_attachment_camera_gallery.model.MessageType
import com.ihsan.chat_attachment_camera_gallery.utils.MyApplication
import java.io.File

class CameraPreviewFragment : Fragment() {
    private val TAG = "CameraPreviewFragment"
    private lateinit var binding: FragmentCameraPreviewBinding
    private val args: CameraPreviewFragmentArgs by navArgs()
    private var photoFile: File? = null
    private lateinit var photoUri: Uri
    private lateinit var cropImageLauncher: ActivityResultLauncher<CropImageContractOptions>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.hide()
        // Inflate the layout for this fragment
        binding = FragmentCameraPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cropImageBtn=binding.cropImage
        val saveImageBtn=binding.saveImage
        val discardImageBtn=binding.discardImage
        val backTo=args.backTo
        (activity as AppCompatActivity).supportActionBar?.hide()

        photoUri = args.photoUri.toUri()
        photoFile = photoUri.toFile()

        Log.d(TAG, "onViewCreated photoUri: $photoUri")
        Log.d(TAG, "onViewCreated photoFile: $photoFile")
        if (!args.modifyImage){
            cropImageBtn.visibility=ViewGroup.GONE
            saveImageBtn.visibility=ViewGroup.GONE
            discardImageBtn.visibility=ViewGroup.GONE
        }
        //load image to camera preview
        Glide.with(this)
            .load(photoUri)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.localImg)

        //Crop Image Activity result
        cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                // Assign crop image uri
                val croppedImageUri = result.uriContent
                Log.d(TAG, "onViewCreated croppedImageUri:$croppedImageUri ")
                //save new cropped image to local photoUri and delete croppedImageUri from cache
                saveCropImage(croppedImageUri, photoUri)
                //updating Camera preview by loading cropped image
                Glide.with(this)
                    .load(photoUri)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.localImg)
            } else {
                Log.e(TAG, "exception on image crop:${result.error}")
            }
        }

        cropImageBtn.setOnClickListener {
            startCrop(photoUri)
        }

        saveImageBtn.setOnClickListener {
            //Navigate Back to BottomAttachmentOptionFragment
            navigateBackToHomeWithArgument(it)
        }

        discardImageBtn.setOnClickListener {
            photoFile?.delete()
            when(backTo){
                BackState.HOME->navigateBackToHome(it)
                BackState.CAMERA->Navigation.findNavController(it)
                    .navigate(R.id.action_cameraPreviewFragment_to_cameraFragment)
                else->{}
            }
        }

        // handling back press event
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when(backTo){
                        BackState.HOME->navigateBackToHome(view)
                        BackState.CAMERA->Navigation.findNavController(view)
                            .navigate(R.id.action_exoPlayerFragment_to_cameraFragment)
                        else->{}
                    }
                }
            })
    }

    private fun navigateBackToHome(view:View){
        (activity as AppCompatActivity).supportActionBar?.show()
        Navigation.findNavController(view)
            .navigate(R.id.action_cameraPreviewFragment_to_bottomAttachmentOptionFragment)
    }
    private fun navigateBackToHomeWithArgument(view: View) {
        (activity as AppCompatActivity).supportActionBar?.show()
        val action =
            CameraPreviewFragmentDirections.actionCameraPreviewFragmentToBottomAttachmentOptionFragment(
                MessageType.IMAGE,
                photoUri.toString()
            )
        Navigation.findNavController(view).navigate(action)
    }

    private fun startCrop(imageUri: Uri) {
        // start cropping activity for pre-acquired image saved on the device
        cropImageLauncher.launch(options(uri = imageUri) {
            setGuidelines(CropImageView.Guidelines.ON)
            setOutputCompressFormat(Bitmap.CompressFormat.PNG)
        })
    }

    private fun saveCropImage(croppedImageUri: Uri?, destinationUri: Uri) {
        if (croppedImageUri != null) {
            Log.d(TAG, "saveCropImage-croppedImageUri: $croppedImageUri")
            //copy file  source to destination
            MyApplication.instance.contentResolver
                .openInputStream(croppedImageUri)
                ?.use { inputStream ->
                    MyApplication.instance.contentResolver.openOutputStream(destinationUri) // photoUri as destination Uri
                        ?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                }
        }
    }
}