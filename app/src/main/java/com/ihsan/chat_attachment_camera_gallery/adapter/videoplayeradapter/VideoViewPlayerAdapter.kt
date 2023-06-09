package com.ihsan.chat_attachment_camera_gallery.adapter.videoplayeradapter

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.model.BackState
import com.ihsan.chat_attachment_camera_gallery.model.Message
import com.ihsan.chat_attachment_camera_gallery.ui.VideoViewFragmentDirections
import com.ihsan.chat_attachment_camera_gallery.utils.MyApplication

class VideoViewPlayerAdapter(private val videoList: List<Message>) :
    RecyclerView.Adapter<VideoViewPlayerAdapter.ViewHolder>() {

    class ViewHolder(private val binding: View) : RecyclerView.ViewHolder(binding) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val videoView: VideoView = itemView.findViewById(R.id.videoPlayer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val videoView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_basic_video_list_item, parent, false)
        return ViewHolder(videoView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videoList[position]
        val videoView = holder.videoView
        val imageView = holder.imageView

        videoView.visibility = ViewGroup.GONE

        Log.d("teamAdapter", "BindViewHolder: ${videoList.size}")
        Log.d("teamAdapter", "BindViewHolder: ${video.data}")

        //load thumbnail for the video
        Glide.with(MyApplication.instance)
            .asBitmap()
            .load(video.data)
            .placeholder(com.google.android.exoplayer2.R.drawable.exo_ic_pause_circle_filled) // Optional placeholder image
            .centerCrop()
            .error(R.drawable.ic_cancel) // Optional error image
            .into(holder.imageView)

        //imageView.foreground=ContextCompat.getDrawable(MyApplication.instance,R.drawable.ic_play_circle_outline)

        holder.itemView.setOnClickListener {

            if (videoView.isPlaying) {
                holder.videoView.setVideoURI(null)
                holder.imageView.visibility = ViewGroup.VISIBLE
                holder.videoView.visibility = ViewGroup.GONE
            } else {
                imageView.visibility = ViewGroup.GONE
                videoView.visibility = ViewGroup.VISIBLE
                val videoUri = Uri.parse(video.data)
                videoView.setVideoURI(videoUri)
                videoView.setOnPreparedListener { mp: MediaPlayer ->
                    videoView.start()
                }
                videoView.setOnCompletionListener {
                    videoView.setVideoURI(null)
                    imageView.visibility = ViewGroup.VISIBLE
                    videoView.visibility = ViewGroup.GONE
                }
            }
        }

        holder.itemView.rootView.setOnLongClickListener {
            val action =
                VideoViewFragmentDirections.actionVideoViewFragmentToExoPlayerFragment(
                    BackState.VIDEO,
                    video.data
                )
            Navigation.findNavController(holder.itemView).navigate(action)
            return@setOnLongClickListener true
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        //Log.d(TAG, "bindingAdapterPosition: ${holder.bindingAdapterPosition}")
        holder.videoView.setVideoURI(null)
        holder.imageView.visibility = ViewGroup.VISIBLE
        holder.videoView.visibility = ViewGroup.GONE
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.videoView.stopPlayback()
        holder.videoView.setVideoURI(null)
        holder.imageView.visibility = ViewGroup.VISIBLE
        holder.videoView.visibility = ViewGroup.GONE
    }

    override fun getItemCount(): Int {
        return videoList.size
    }
}