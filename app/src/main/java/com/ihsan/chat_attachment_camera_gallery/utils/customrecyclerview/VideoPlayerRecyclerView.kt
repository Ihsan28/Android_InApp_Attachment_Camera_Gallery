package com.ihsan.chat_attachment_camera_gallery.utils.customrecyclerview

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.Display
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.model.Message

private const val TAG = "VideoPlayerRecyclerView"
class VideoPlayerRecyclerView : RecyclerView {
    private enum class VolumeState { ON, OFF }

    // ui
    private var thumbnail: ImageView? = null
    private var volumeControl: ImageView? = null
    private var progressBar: ProgressBar? = null
    private var viewHolderParent: View? = null
    private lateinit var frameLayout: FrameLayout
    private var videoPlayerSurfaceView: StyledPlayerView? = null
    private var videoPlayer: ExoPlayer? = null

    // vars
    private var mediaObjects = ArrayList<Message>()
    private var videoSurfaceDefaultHeight: Int = 0
    private var screenDefaultHeight: Int = 0
    private lateinit var context: Context
    private var playPosition: Int = -1
    private var isVideoViewAdded = false
    private lateinit var requestManager: RequestManager

    // controlling playback state
    private lateinit var volumeState: VolumeState

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        this.context = context.applicationContext
        val display: Display? =
            (getContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

        val point = Point()
        display?.getSize(point)

        videoSurfaceDefaultHeight = point.x
        screenDefaultHeight = point.y

        videoPlayerSurfaceView = StyledPlayerView(this.context)
        videoPlayerSurfaceView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        val trackSelector = DefaultTrackSelector(context)
        val loadControl = DefaultLoadControl()
        val renderersFactory = DefaultRenderersFactory(context)

        videoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()


        // Bind the player to the view.
        videoPlayerSurfaceView!!.useController = false
        videoPlayerSurfaceView!!.player = videoPlayer

        setVolumeControl(VolumeState.ON)

        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    Log.d(TAG, "onScrollStateChanged: called.")
                    if (thumbnail != null) { // show the old thumbnail
                        thumbnail!!.visibility = VISIBLE
                    }

                    // There's a special case when the end of the list has been reached.
                    // Need to handle that with this bit of logic
                    if (!recyclerView.canScrollVertically(1)) {
                        playVideo(true)

                    } else {

                        playVideo(false)
                        Log.e(TAG, "onScrollStateChanged: not last")
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {}

            override fun onChildViewDetachedFromWindow(view: View) {
                if (viewHolderParent != null && viewHolderParent == view) {
                    if (!isVideoViewVisible(viewHolderParent)) {
                        resetVideoView()
                    }
                }
            }
        })

        videoPlayer?.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {}
            override fun onTracksChanged(tracks: Tracks) {}
            override fun onIsLoadingChanged(isLoading: Boolean) {}

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        Log.e(TAG, "onPlayerStateChanged: Buffering video.")
                        if (progressBar != null) {
                            progressBar!!.visibility = VISIBLE
                        }
                    }

                    Player.STATE_ENDED -> {
                        Log.d(TAG, "onPlayerStateChanged: Video ended.")
                        videoPlayer?.seekTo(0)
                    }

                    Player.STATE_IDLE -> {
                    }

                    Player.STATE_READY -> {
                        Log.e(TAG, "onPlayerStateChanged: Ready to play.")
                        if (progressBar != null) {
                            progressBar!!.visibility = GONE
                        }
                        if (!isVideoViewAdded) {
                            addVideoView()
                        }
                    }

                    else -> {
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {}

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

            override fun onPlayerError(error: PlaybackException) {}

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
            override fun onSeekProcessed() {}
        })
    }

    private fun isVideoViewVisible(parent: View?): Boolean {
        val percentThreshold = 0.5
        val videoViewBounds = Rect()
        videoPlayerSurfaceView?.getGlobalVisibleRect(videoViewBounds)
        val parentBounds = Rect()
        parent?.getGlobalVisibleRect(parentBounds)
        val visibleArea = videoViewBounds.width() * videoViewBounds.height()
        val totalArea = parentBounds.width() * parentBounds.height()
        return visibleArea / totalArea >= percentThreshold
    }

    fun playVideo(isEndOfList: Boolean) {
        val targetPosition: Int

        if (!isEndOfList) {
            val startPosition =
                (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            var endPosition = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

            // if there is more than 2 list-items on the screen, set the difference to be 1
            if (endPosition - startPosition > 1) {
                endPosition = startPosition + 1
            }

            // something is wrong. return.
            if (startPosition < 0 || endPosition < 0) {
                return
            }

            // if there is more than 1 list-item on the screen
            targetPosition = if (startPosition != endPosition) {
                val startPositionVideoHeight = getVisibleVideoSurfaceHeight(startPosition)
                val endPositionVideoHeight = getVisibleVideoSurfaceHeight(endPosition)
                if (startPositionVideoHeight > endPositionVideoHeight) startPosition else endPosition
            } else {
                startPosition
            }
        } else {
            targetPosition = mediaObjects.size - 1
        }
        Log.d(TAG, "playVideo: target position: $targetPosition")

        // video is already playing so return
        if (targetPosition == playPosition) {
            return
        }

        // set the position of the list-item that is to be played
        playPosition = targetPosition
        if (videoPlayerSurfaceView == null) {
            return;
        }
        // remove any old surface views from previously playing videos
        videoPlayerSurfaceView!!.visibility = INVISIBLE
        removeVideoView(videoPlayerSurfaceView!!)

        val currentPosition =
            targetPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        val child = getChildAt(currentPosition) ?: return

        val holder = child.tag as VideoPlayerViewHolder?
        if (holder == null) {
            playPosition = -1
            return
        }
        thumbnail = holder.thumbnail
        progressBar = holder.progressBar
        volumeControl = holder.volumeControl
        viewHolderParent = holder.itemView
        requestManager = holder.requestManager
        frameLayout = holder.media_container

        videoPlayerSurfaceView!!.player = videoPlayer

        viewHolderParent!!.setOnClickListener(videoViewClickListener)

        val dataSourceFactory = DefaultDataSource.Factory(context)
        val videoUrl = mediaObjects[targetPosition].data
        val mediaItem = MediaItem.fromUri(videoUrl)
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
        //videoPlayer?.setMediaSource(videoSource)
        videoPlayer?.prepare(videoSource)
        videoPlayer?.playWhenReady = true
        //addVideoView()
    }

    private val videoViewClickListener = OnClickListener {
        toggleVolume()
    }

    private fun getVisibleVideoSurfaceHeight(playPosition: Int): Int {
        val at =
            playPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        Log.d(TAG, "getVisibleVideoSurfaceHeight: at: $at")

        val child = getChildAt(at) ?: return 0

        val location = IntArray(2)
        child.getLocationInWindow(location)

        return if (location[1] < 0) {
            location[1] + videoSurfaceDefaultHeight
        } else {
            screenDefaultHeight - location[1]
        }
    }

    private fun addVideoView() {
        frameLayout.addView(videoPlayerSurfaceView)
        isVideoViewAdded = true
        videoPlayerSurfaceView?.requestFocus()
        videoPlayerSurfaceView?.visibility = VISIBLE
        videoPlayerSurfaceView?.alpha = 1f
        thumbnail?.visibility = GONE
    }

    private fun resetVideoView() {
        if (isVideoViewAdded) {
            videoPlayerSurfaceView?.let { removeVideoView(it) }
            playPosition = -1
            videoPlayerSurfaceView?.visibility = INVISIBLE
            thumbnail?.visibility = VISIBLE
        }
    }

    // Remove the old player
    private fun removeVideoView(videoView: StyledPlayerView) {
        val parent = videoView.parent as ViewGroup? ?: return

        val index = parent.indexOfChild(videoView)
        if (index >= 0) {
            parent.removeViewAt(index)
            isVideoViewAdded = false
            viewHolderParent?.setOnClickListener(null)
        }
    }

    fun releasePlayer() {
        if (videoPlayer != null) {
            videoPlayer!!.release()
            videoPlayer = null
        }
        viewHolderParent = null
    }

    private fun toggleVolume() {

        //if (isVideoViewAdded) {
        if (videoPlayer != null) {
            videoPlayer?.let {
                when (volumeState) {
                    VolumeState.OFF -> {
                        Log.d(TAG, "togglePlaybackState: enabling volume.")
                        setVolumeControl(VolumeState.ON)
                    }

                    VolumeState.ON -> {
                        Log.d(TAG, "togglePlaybackState: disabling volume.")
                        setVolumeControl(VolumeState.OFF)
                    }
                }
            }
        }
    }

    private fun setVolumeControl(state: VolumeState) {
        volumeState = state
        when (state) {
            VolumeState.OFF -> {
                videoPlayer?.volume = 0f
                animateVolumeControl()
            }

            VolumeState.ON -> {
                videoPlayer?.volume = 1f
                animateVolumeControl()
            }
        }
    }

    private fun animateVolumeControl() {
        volumeControl?.let {
            it.bringToFront()
            when (volumeState) {
                VolumeState.OFF -> {
                    requestManager.load(R.drawable.ic_volume_off_grey_24dp)
                        .into(it)
                }

                VolumeState.ON -> {
                    requestManager.load(R.drawable.ic_volume_up_grey_24dp)
                        .into(it)
                }
            }
            it.animate().cancel()

            it.alpha = 1f

            it.animate()
                .alpha(0f)
                .setDuration(600).setStartDelay(1000)
        }
    }

    fun setMediaObjects(mediaObjects: ArrayList<Message>) {
        this.mediaObjects = mediaObjects
    }
}

