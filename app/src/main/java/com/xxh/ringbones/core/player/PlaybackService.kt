package com.xxh.ringbones.core.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.xxh.ringbones.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Notification channel ID used for the playback notification. */
private const val PLAYBACK_CHANNEL_ID = "playback_channel"

/** Notification channel display name shown in system settings. */
private const val PLAYBACK_CHANNEL_NAME = "Playback"

/** Notification ID for the foreground service. */
private const val NOTIFICATION_ID = 1001

/**
 * Foreground service that owns the ExoPlayer instance and MediaSession.
 *
 * Implements the standard Media3 MediaSessionService pattern:
 * - [onCreate] creates ExoPlayer + MediaSession + notification channel
 * - [onGetSession] returns the session to connecting MediaControllers
 * - [onTaskRemoved] stops the service when the app is swiped away and no
 *   content is playing
 * - [onDestroy] releases player and session resources
 *
 * Custom playback state (AB loop, sleep timer, visualizer, EQ) is published
 * to [PlayerStateBridge] so the ViewModel can observe it.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var playerStateBridge: PlayerStateBridge

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer = player

        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(com.xxh.ringbones.R.mipmap.musicology)

        setMediaNotificationProvider(notificationProvider)

        val sessionCallback = PlaybackServiceCallback(
            service = this,
            exoPlayer = player,
            playerStateBridge = playerStateBridge,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
        // PlaybackServiceCallback self-registers a Player.Listener in its init
        // block for visualizer / progress lifecycle management.
    }

    @UnstableApi
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer ?: run {
            stopSelf()
            return
        }
        if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player?.stop()
            release()
        }
        exoPlayer?.release()
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    /** Creates the notification channel required for foreground service. */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            PLAYBACK_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Playback controls"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}