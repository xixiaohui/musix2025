package com.xxh.ringbones.presentation.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import com.xxh.ringbones.domain.usecase.RecordPlayHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val ringtoneRepository: RingtoneRepository,
    private val recordPlayHistoryUseCase: RecordPlayHistoryUseCase
) : ViewModel() {

    private val ringtoneId: Long = savedStateHandle["ringtoneId"] ?: 0L

    private val _ringtone = MutableStateFlow<Ringtone?>(null)
    val ringtone: StateFlow<Ringtone?> = _ringtone.asStateFlow()

    private val _exoPlayer = MutableStateFlow<ExoPlayer?>(null)
    val exoPlayer: StateFlow<ExoPlayer?> = _exoPlayer.asStateFlow()

    private var currentPosition: Long = 0L

    init {
        viewModelScope.launch {
            ringtoneRepository.getById(ringtoneId).collect { ringtone ->
                _ringtone.value = ringtone
                ringtone?.let { initializePlayer(it) }
            }
        }
    }

    private fun initializePlayer(ringtone: Ringtone) {
        if (_exoPlayer.value != null) return
        val player = ExoPlayer.Builder(context).build().apply {
            val uri = resolveUri(ringtone)
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            seekTo(currentPosition)
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    android.util.Log.e("PlayerViewModel", "Playback error: ${error.message}")
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        viewModelScope.launch {
                            recordPlayHistoryUseCase(ringtone.id)
                        }
                    }
                }
            })
        }
        _exoPlayer.value = player
    }

    private fun resolveUri(ringtone: Ringtone): Uri {
        return ringtone.downloadPath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) Uri.parse("file://$path")
            else Uri.parse(ringtone.url)
        } ?: Uri.parse(ringtone.url)
    }

    fun savePlayerState() {
        _exoPlayer.value?.let { currentPosition = it.currentPosition }
    }

    override fun onCleared() {
        super.onCleared()
        _exoPlayer.value?.release()
        _exoPlayer.value = null
    }
}
