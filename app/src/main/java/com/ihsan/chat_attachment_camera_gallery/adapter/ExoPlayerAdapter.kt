package com.ihsan.chat_attachment_camera_gallery.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.model.BackState
import com.ihsan.chat_attachment_camera_gallery.model.Message
import com.ihsan.chat_attachment_camera_gallery.ui.VideoViewFragmentDirections
import com.ihsan.chat_attachment_camera_gallery.utils.MyApplication

private const val TAG = "ExoPlayerAdapter"
class ExoPlayerAdapter(private val videoList: List<Message>) :
    RecyclerView.Adapter<ExoPlayerAdapter.ThumbnailViewHolder>() {
    //private val player: ExoPlayer by lazy { ExoPlayer.Builder(MyApplication.instance).build() }

    class ThumbnailViewHolder(private val binding: View) : RecyclerView.ViewHolder(binding) {
        val videoThumbnail: ImageView = itemView.findViewById(R.id.local_img)
        val videoViewContainer: RelativeLayout = itemView.findViewById(R.id.video_player_container)
        val videoView: StyledPlayerView = itemView.findViewById(R.id.exo_player)

        //val player: ExoPlayer = ExoPlayer.Builder(MyApplication.instance).build()

        private var playbackPosition: Long = 0

        fun bind(videoUrl: String) {
            val player: ExoPlayer = ExoPlayer.Builder(MyApplication.instance).build()
            val mediaItem = MediaItem.fromUri(videoUrl)
            //player.setMediaItem(mediaItem)
            val mediaSource =
                ProgressiveMediaSource.Factory(DefaultDataSource.Factory(MyApplication.instance))
                    .createMediaSource(mediaItem)
            /*if (playbackPosition != 0L) {
                player.seekTo(playbackPosition)
            }*/

            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()

            videoView.player = player
        }

        fun release() {
            //playbackPosition = player.currentPosition
            //player.pause()
            videoView.player = null
            videoViewContainer.visibility = ViewGroup.GONE

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val root =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_thumbnail, parent, false)
        Log.d("teamAdapter", "onCreateViewHolder: ${videoList.size}")
        return ThumbnailViewHolder(root)
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    @SuppressLint("SetTextI18n", "ResourceAsColor")
    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        val video = videoList[position]
        Log.d("teamAdapter", "BindViewHolder: ${videoList.size}")
        Glide.with(MyApplication.instance)
            .asGif()
            .load(video.data)
            .placeholder(com.google.android.exoplayer2.R.drawable.exo_ic_pause_circle_filled) // Optional placeholder image
            .error(R.drawable.ic_cancel) // Optional error image
            .into(holder.videoThumbnail)

        holder.itemView.rootView.setOnClickListener {
            //mediaPlayer(holder.videoViewContainer)
            //player.release()
            holder.bind(video.data)
            holder.videoViewContainer.visibility = ViewGroup.VISIBLE
        }

        holder.itemView.rootView.setOnLongClickListener {
            val action =
                VideoViewFragmentDirections.actionVideoViewFragmentToExoPlayerFragment(BackState.VIDEO,video.data)
            Navigation.findNavController(holder.itemView).navigate(action)
            return@setOnLongClickListener true
        }
    }

    override fun onViewDetachedFromWindow(holder: ThumbnailViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.release()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val posFirst =
                    (recyclerView.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition()
                val posLast =
                    (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()

                val firstIncomplete =
                    (recyclerView.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
                val lastIncomplete =
                    (recyclerView.layoutManager as? LinearLayoutManager)?.findLastVisibleItemPosition()
                if (posLast == -1 || posFirst == -1) return

                Log.d(TAG, "onScrolled: Scrolled First $posFirst")
                Log.d(TAG, "onScrolled: Scrolled Last $posLast")

                if (posFirst != firstIncomplete && firstIncomplete != null) {
                    Log.d(TAG, "onScrolled: Scrolled not matched")
                    val viewHolder =
                        recyclerView.findViewHolderForLayoutPosition(firstIncomplete) as ThumbnailViewHolder
                    //viewHolder.release()
                }

                if (posLast != lastIncomplete && lastIncomplete != null) {
                    Log.d(TAG, "onScrolled last: Scrolled not matched")
                    val viewHolder =
                        recyclerView.findViewHolderForLayoutPosition(lastIncomplete) as ThumbnailViewHolder
                    //viewHolder.release()
                }

                Log.d(TAG, "onScrolled: finishCheckPauseTask")
            }
        })
    }

    private fun mediaPlayer(videoContainer: RelativeLayout) {
        //keep mini width
        val layoutParamsMini = videoContainer.layoutParams
        val layoutParamsMax = layoutParamsMini
        // set the new height and width values in pixels
        layoutParamsMax.height = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParamsMax.width = ViewGroup.LayoutParams.MATCH_PARENT
        videoContainer.layoutParams = layoutParamsMax
    }
}
