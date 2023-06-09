package com.ihsan.chat_attachment_camera_gallery.ui

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.adapter.VideoPlayerRecyclerAdapter
import com.ihsan.chat_attachment_camera_gallery.databinding.FragmentVideoViewBinding
import com.ihsan.chat_attachment_camera_gallery.model.Message
import com.ihsan.chat_attachment_camera_gallery.model.MessageType
import com.ihsan.chat_attachment_camera_gallery.utils.customrecyclerview.VerticalSpacingItemDecorator
import com.ihsan.chat_attachment_camera_gallery.utils.customrecyclerview.VideoPlayerRecyclerView

class VideoViewFragment : Fragment() {
    private lateinit var binding: FragmentVideoViewBinding

    private lateinit var videoRecyclerView: VideoPlayerRecyclerView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentVideoViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //auto playing custom recyclerview (Not Used Right Now)
        videoRecyclerView = binding.videoRecyclerviewThumbnail
        videoRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
        videoRecyclerView.setHasFixedSize(true)
        initCustomVideoRecyclerView()

        recyclerView = binding.recyclerviewThumbnail
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.setHasFixedSize(true)

        val appDirectoryVideos = loadVideos()

        /*recyclerView.adapter = VideoViewPlayerAdapter(appDirectoryVideos)*/

        /*recyclerView.adapter = ExoPlayerAdapter(appDirectoryVideos)*/
    }

    private fun initCustomVideoRecyclerView() {
        val videoList = loadVideos()
        val itemDecor = VerticalSpacingItemDecorator(10)
        videoRecyclerView.addItemDecoration(itemDecor)
        videoRecyclerView.setMediaObjects(videoList)
        videoRecyclerView.adapter = VideoPlayerRecyclerAdapter(videoList, initGlide())
    }

    private fun initGlide(): RequestManager {
        val options = RequestOptions().placeholder(R.drawable.white_background)
            .error(R.drawable.white_background)
        return Glide.with(this).setDefaultRequestOptions(options)
    }

    private fun loadVideos(): ArrayList<Message> {
        val videoDirectory =
            Environment.getExternalStoragePublicDirectory("DCIM/${resources.getString(R.string.app_name)}/Videos")

        val videoUris = ArrayList<Message>()

        if (videoDirectory.exists() && videoDirectory.isDirectory) {
            val files = videoDirectory.listFiles()

            if (files != null) {
                for (file in files) {
                    if (file.isFile && file.extension == "mp4") {
                        val uri = file.toUri()
                        val video = Message(
                            MessageType.VIDEO,
                            null,
                            "",
                            "",
                            uri.toString(),
                            0,
                            false,
                        )
                        videoUris.add(video)
                    }
                }
            }
        }
        return videoUris
    }
    /*
        //for sdk level 29+
        private fun loadVideosQ(): ArrayList<Message> {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DATA
            )
            val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("%/DCIM/${resources.getString(R.string.app_name)}/Videos/%")
            val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"
            val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            val cursor = requireContext().contentResolver.query(
                queryUri, projection, selection, selectionArgs, sortOrder
            )

            val videoList = mutableListOf<Message>()

            if (cursor != null) {
                Log.d(TAG, "loadVideos: ${cursor.position}")
                while (cursor.moveToNext()) {
                    //val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val displayName =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                    val dateTaken =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))

                    val data =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))

                    val video = Message(
                        MessageType.VIDEO,
                        null,
                        "",
                        "",
                        displayName,
                        "",
                        data,
                        dateTaken,
                        true
                    )
                    Log.d(TAG, "loadVideos2: $video")
                    videoList.add(video)
                }
                cursor.close()
            }
            Log.d(TAG, "loadVideos: ${videoList.size}")
            return videoList as ArrayList<Message>
        }*/
}