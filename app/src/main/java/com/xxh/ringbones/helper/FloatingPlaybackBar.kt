package com.xxh.ringbones.helper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.xxh.ringbones.R
import java.lang.reflect.Modifier

//@Composable
//fun FloatingPlaybackBar(
//    // 1. Dynamic State Management with Flow
//    selectedTrackStateFlow: Flow<SelectedTrackState> = flowOf(SelectedTrackState()),
//    // 2. Interactive Controls through Lambda Functions
//    onPreviousClicked: () -> Unit = {},
//    onPlayPauseClicked: () -> Unit = {},
//    onNextClicked: () -> Unit = {}
//) {
//    // 3. Real-Time UI Updates with State Observing
//    val selectedTrackState = selectedTrackStateFlow.collectAsState(initial = SelectedTrackState()).value
//    // 4. Efficient Image Rendering with Coil
//    val imagePainter = rememberImagePainter(
//        data =
//        // 5. Image Handling with Fallbacks
//        if (selectedTrackState.track.coverDrawableId == -1) null
//        else selectedTrackState.track.coverDrawableId,
//        builder = {
//            fallback(R.drawable.fallback_album_cover)
//        }
//    )
//    Card(
//        modifier = Modifier
//            .padding(MediumDp)
//            .height(FloatingPlaybackBarHeight)
//            .background(Color.Transparent)
//            .fillMaxWidth(),
//        elevation = CardDefaults.cardElevation(defaultElevation = LargeDp),
//        shape = RoundedCornerShape(MediumDp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(PlaybackBarColor)
//                .padding(SmallDp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(FloatingPlaybackBarCoverSize)
//                    .clip(RoundedCornerShape(MediumDp)),
//                contentAlignment = Alignment.Center
//            ) {
//                Image(
//                    painter = imagePainter,
//                    contentDescription = null,
//                    modifier = Modifier.fillMaxSize()
//                )
//            }
//            Column(
//                modifier = Modifier
//                    .padding(start = LargeDp)
//                    .weight(1f)
//            ) {
//                Text(
//                    style = FloatingPlaybackBarPrimaryTextStyle,
//                    text = selectedTrackState.track.title,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Text(
//                    style = FloatingPlaybackBarSecondaryTextStyle,
//                    text = selectedTrackState.track.artist,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//            // 6. Wrapping Icon inside IconButton
//            IconButton(
//                onClick = { onPreviousClicked() },
//                modifier = Modifier.size(FloatingPlaybackBarButtonSize)
//            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_previous),
//                    contentDescription = null,
//                    tint = PrimaryWhite,
//                    modifier = Modifier.size(FloatingPlaybackBarButtonIconSize)
//                )
//            }
//            // 7. Responsive Control Icons
//            val iconId =
//                if (selectedTrackState.playbackState == PlaybackState.PLAYING) {
//                    R.drawable.ic_pause
//                } else {
//                    R.drawable.ic_play
//                }
//            IconButton(
//                onClick = { onPlayPauseClicked() },
//                modifier = Modifier.size(FloatingPlaybackBarButtonSize)
//            ) {
//                Icon(
//                    painter = painterResource(id = iconId),
//                    contentDescription = null,
//                    tint = PrimaryWhite,
//                    modifier = Modifier.size(FloatingPlaybackBarButtonIconSize)
//                )
//            }
//            IconButton(
//                onClick = { onNextClicked() },
//                modifier = Modifier.size(FloatingPlaybackBarButtonSize)
//            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_next),
//                    contentDescription = null,
//                    tint = PrimaryWhite,
//                    modifier = Modifier.size(FloatingPlaybackBarButtonIconSize)
//                )
//            }
//        }
//    }
//}
//
//@Preview
//@Composable
//fun FloatingPlaybackBarPreview() {
//    FloatingPlaybackBar()
//}