package com.ihsan.chat_attachment_camera_gallery.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.adapter.MessageAdapter
import com.ihsan.chat_attachment_camera_gallery.databinding.FragmentBottomAttachmentOptionBinding
import com.ihsan.chat_attachment_camera_gallery.model.BackState
import com.ihsan.chat_attachment_camera_gallery.model.Message
import com.ihsan.chat_attachment_camera_gallery.model.MessageType
import com.ihsan.chat_attachment_camera_gallery.utils.Constants
import com.ihsan.chat_attachment_camera_gallery.utils.MyApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class BottomAttachmentOptionFragment : Fragment() {
    private val personIdList = listOf("1001", "1002")

    companion object {
        private val conversation = ArrayList<Message>()
        private var senderId = ""
        private var receiverId = ""
    }

    private val TAG = "BottomAttachmentOptionFragment"
    private lateinit var binding: FragmentBottomAttachmentOptionBinding
    private lateinit var recyclerView: RecyclerView
    private val args: BottomAttachmentOptionFragmentArgs by navArgs()
    private var doubleBackPressedOnce = false

    // select launcher for image and video
    private lateinit var imageSelectLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoSelectLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentBottomAttachmentOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sendBtn = binding.sendButton
        val attachmentBtn = binding.attachButton

        recyclerView = binding.recyclerview
        //assigning sender receiver
        senderId = personIdList[0]
        receiverId = personIdList[1]

        //Argument added to the conversation
        if (args.fileUri != null && args.messageType != null) {
            //if last item match with argument
            if (conversation.isNotEmpty()) {
                if (args.fileUri != conversation.last().data) {
                    Toast.makeText(requireContext(), "added", Toast.LENGTH_SHORT).show()
                    addToConversation(args.messageType!!, args.fileUri!!)
                }
            } else {
                //for first time adding to conversation
                Toast.makeText(requireContext(), "added", Toast.LENGTH_SHORT).show()
                addToConversation(args.messageType!!, args.fileUri!!)
            }
        }

        if (conversation.isEmpty()) {
            loadLocalFiles()
        }

        if (savedInstanceState != null) {
            val savedRecyclerState = savedInstanceState.getParcelable<Parcelable>("recyclerViewState")
            recyclerView.layoutManager?.onRestoreInstanceState(savedRecyclerState)
            Log.d(TAG, "onViewCreated: savedRecyclerState")
            if (conversation.isNotEmpty()) {
                Log.d(TAG, "callAdapter: ${conversation.size}")
                recyclerView.adapter = MessageAdapter(conversation)
            }
        } else {
            //calling adapter to load conversation
            callAdapter()
        }

        imageSelectLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val imageUris = mutableListOf<Uri?>()

                    try {
                        // if multiple images are selected
                        if (data?.clipData != null) {
                            val count = data.clipData?.itemCount
                            for (i in 0 until count!!) {
                                imageUris.add(data.clipData?.getItemAt(i)!!.uri)
                            }
                            //saving selected file and calling adapter again for update
                            if (imageUris.size > 0) {
                                saveSelectedFiles(MessageType.IMAGE, imageUris)
                                callAdapter()
                            }
                        } else if (data?.data != null) {
                            // if single image is selected
                            imageUris.add(data.data!!)
                            val selectedImageUriString =
                                saveSingleSelectedFile(MessageType.IMAGE, data.data!!)

                            //Navigate Back to BottomAttachmentOptionFragment
                            val action =
                                BottomAttachmentOptionFragmentDirections.actionBottomAttachmentOptionFragmentToCameraPreviewFragment(
                                    selectedImageUriString, BackState.HOME, true
                                )
                            Navigation.findNavController(binding.root).navigate(action)
                            Log.d(TAG, "onViewCreated: ${data.data}")
                        }
                    } catch (e: java.lang.Exception) {
                        Log.d(TAG, "onActivityResult: $e")
                    }
                }
            }

        videoSelectLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val videoUris = mutableListOf<Uri?>()
                    try {
                        // if multiple images are selected
                        if (data?.clipData != null) {
                            val count = data.clipData?.itemCount
                            for (i in 0 until count!!) {
                                videoUris.add(data.clipData?.getItemAt(i)!!.uri)
                            }
                            //saving selected file, add to the conversation object and calling adapter again for update
                            if (videoUris.size > 0) {
                                saveSelectedFiles(MessageType.VIDEO, videoUris)
                                callAdapter()
                            }
                        } else if (data?.data != null) {
                            // if single image is selected
                            videoUris.add(data.data!!)
                            //saving video and get the new saved video Uri in string
                            val selectedVideoUriString =
                                saveSingleSelectedFile(MessageType.VIDEO, data.data!!)
                            val action =
                                BottomAttachmentOptionFragmentDirections.actionBottomAttachmentOptionFragmentToExoPlayerFragment(
                                    BackState.HOME, selectedVideoUriString
                                )
                            Navigation.findNavController(binding.root).navigate(action)
                            Log.d(TAG, "onViewCreated: ${data.data}")
                        }

                    } catch (e: java.lang.Exception) {
                        Log.d(TAG, "onActivityResult: $e")
                    }
                }
            }

        // handling back press event and enabling twice back to exit
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (doubleBackPressedOnce) {
                        // taking action for double back press event
                        requireActivity().finish()
                    } else {
                        doubleBackPressedOnce = true
                        Toast.makeText(
                            requireContext(),
                            "Press back again to exit",
                            Toast.LENGTH_SHORT
                        ).show()
                        //running looper to wait for second back press
                        Handler(Looper.getMainLooper()).postDelayed({
                            doubleBackPressedOnce = false
                        }, 2000)
                    }
                }
            })

        //bottom attachment click listener
        attachmentBtn.setOnClickListener {
            openAttachmentOption(view)
        }

        //text send click listener, attach text to the bottom
        sendBtn.setOnClickListener {
            if (!TextUtils.isEmpty(binding.textMessage.text)) {
                addToConversation(MessageType.TEXT, binding.textMessage.text.toString())
                callAdapter()
                binding.textMessage.text.clear()
            }
        }
    }

    private fun addToConversation(messageType: MessageType, message: String) {
        val textMessage = Message(
            messageType,
            true,
            senderId,
            receiverId,
            message,
            getTimeStamp(),
            false
        )
        conversation.add(textMessage)
        Log.d(TAG, "addToConversation $messageType: $message")
    }

    private fun callAdapter() {
        Log.d(TAG, "callAdapter: conversation size ${conversation.size}")

        if (conversation.isNotEmpty()) {
            Log.d(TAG, "callAdapter: ${conversation.size}")
            val adapter = MessageAdapter(conversation)
            recyclerView.adapter = adapter
            recyclerView.scrollToPosition(conversation.size - 1)
        } else {
            Log.d(TAG, "callAdapter: message empty")
        }
    }

    //attachment Alert Dialog menu
    private fun openAttachmentOption(view: View) {
        val options =
            arrayOf<CharSequence>(
                "Camera",
                "Choose Images",
                "Choose Videos",
                "Show Videos"
            )

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Option")

        //take action for selected item
        builder.setItems(options) { _, item ->
            when {
                options[item] == "Camera" -> {
                    Navigation.findNavController(view).navigate(
                        R.id.action_bottomAttachmentOptionFragment_to_cameraFragment
                    )
                }

                options[item] == "Choose Images" -> {
                    openGalleryForImages()
                }

                options[item] == "Choose Videos" -> {
                    openGalleryForVideos()
                }

                options[item] == "Show Videos" -> {
                    Navigation.findNavController(view).navigate(
                        R.id.action_bottomAttachmentOptionFragment_to_videoViewFragment
                    )
                }
            }
        }
        builder.show()
    }

    private fun openGalleryForImages() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        imageSelectLauncher.launch(intent)
    }

    private fun openGalleryForVideos() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "video/*"
        videoSelectLauncher.launch(intent)
    }

    // save gallery selected all files
    private fun saveSelectedFiles(type: MessageType, uris: MutableList<Uri?>): String {
        var count = 0
        var destinationUriString = ""
        val mediaDir = getMediaDirectory(type)

        uris.map { sourceUri ->
            if (sourceUri != null) {
                //making directory path of the file using directory
                val directoryPath = when (type) {
                    MessageType.IMAGE -> "IMAGE_" + getFileNameFormat() + ".jpg"
                    MessageType.VIDEO -> "VIDEO_" + getFileNameFormat() + ++count + ".mp4"
                    else -> "unknown"
                }
                // URI of the destination file with new name
                val destinationUri = Uri.fromFile(File(mediaDir, directoryPath))
                //copy file  source to destination
                copyFileFromUri(sourceUri, destinationUri)
                destinationUriString = destinationUri.toString()
                //when multiple images, images will be added directly to the conversation
                if (uris.size == 1) {
                    return destinationUriString
                }
                //adding selected image files to the conversation
                addToConversation(type, destinationUri.toString())
            }
        }
        Log.d(TAG, "saveSelectedFiles: destination: $destinationUriString")
        return destinationUriString
    }

    // save gallery selected single file
    private fun saveSingleSelectedFile(type: MessageType, uri: Uri): String {
        val mediaDir = getMediaDirectory(type)

        //making directory path of the file using directory
        val directoryPath = when (type) {
            MessageType.IMAGE -> "IMAGE_" + getFileNameFormat() + ".jpg"
            MessageType.VIDEO -> "VIDEO_" + getFileNameFormat() + ".mp4"
            else -> "unknown"
        }
        // URI of the destination file with new name
        val destinationUri = Uri.fromFile(File(mediaDir, directoryPath))
        //copy file  source to destination
        copyFileFromUri(uri, destinationUri)

        return destinationUri.toString()
    }

    private fun copyFileFromUri(sourceUri: Uri, destinationUri: Uri) {
        //copy file  source to destination
        MyApplication.instance.contentResolver.openInputStream(sourceUri)
            ?.use { inputStream ->
                MyApplication.instance.contentResolver.openOutputStream(destinationUri)
                    ?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
            }
    }

    private fun getMediaDirectory(type: MessageType): File {
        return when (type) {
            MessageType.IMAGE -> {
                Environment.getExternalStoragePublicDirectory("DCIM/${resources.getString(R.string.app_name)}/Pictures")
            }

            MessageType.VIDEO -> {
                Environment.getExternalStoragePublicDirectory("DCIM/${resources.getString(R.string.app_name)}/Videos")
            }

            else -> {
                Environment.getExternalStoragePublicDirectory("DCIM/${resources.getString(R.string.app_name)}")
            }
        }.let {
            //if doesn't exist then create
            if (!it.isDirectory) {
                it.mkdirs()
            }
            it
        }
    }

    private fun getFileNameFormat(): String {
        return SimpleDateFormat(
            Constants.FILE_NAME_FORMAT,
            Locale.getDefault()
        )
            .format(System.currentTimeMillis())
    }

    private fun getTimeStamp(): Long {
        return SimpleDateFormat(
            Constants.FILE_TIME_FORMAT,
            Locale.getDefault()
        ).format(System.currentTimeMillis()).toLong()
    }

    private fun loadLocalFiles() {
        val imageDirectory =
            Environment.getExternalStoragePublicDirectory("DCIM/${resources.getString(R.string.app_name)}/Pictures")
                .let {
                    //if doesn't exist then create
                    if (!it.isDirectory) {
                        it.mkdirs()
                    }
                    it
                }
        val videoDirectory =
            Environment.getExternalStoragePublicDirectory("DCIM/${resources.getString(R.string.app_name)}/Videos")
                .let {
                    //if doesn't exist then create
                    if (!it.isDirectory) {
                        it.mkdirs()
                    }
                    it
                }

        //loading all image file from the directory
        if (imageDirectory.exists() && imageDirectory.isDirectory) {
            val imageFiles = imageDirectory.listFiles() as Array<File>
            for (file in imageFiles) {
                if (file.isFile && file.extension == "jpg") {
                    val video = Message(
                        MessageType.IMAGE,
                        null,
                        "",
                        "",
                        file.toUri().toString(),
                        file.lastModified(),
                        false
                    )
                    conversation.add(video)
                }
            }
        }

        //loading all video file from local directory
        if (videoDirectory.exists() && videoDirectory.isDirectory) {
            val videoFiles = videoDirectory.listFiles() as Array<File>
            for (file in videoFiles) {
                if (file.isFile && file.extension == "mp4") {
                    val video = Message(
                        MessageType.VIDEO,
                        null,
                        "",
                        "",
                        file.toUri().toString(),
                        file.lastModified(),
                        false
                    )
                    conversation.add(video)
                    Log.d(TAG, "callAdapter: ${file.lastModified()}")
                }
            }
        }
        conversation.sortedBy {
            it.timestamp
        }
    }
}