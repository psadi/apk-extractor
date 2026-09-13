package com.psadi.apkextractor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psadi.apkextractor.util.letterIndexForOffset
import com.psadi.apkextractor.util.resolveTargetListIndex
import kotlin.math.roundToInt

/**
 * Vertical A-Z fast scroller with a floating letter-preview bubble.
 *
 * While the user presses or drags along the track the active letter is
 * highlighted, a zoomed-in bubble follows the touch position, and
 * [onLetterSelected] is invoked with the target list index in real time.
 */
@Composable
fun AlphabeticalFastScroller(
    alphabetMap: Map<Char, Int>,
    onLetterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    val haptic = LocalHapticFeedback.current
    val bubbleSize = 60.dp

    var isDragging by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var thumbOffsetY by remember { mutableStateOf(0f) }
    var trackTop by remember { mutableStateOf(0f) }
    var trackLeft by remember { mutableStateOf(0f) }
    var trackHeight by remember { mutableStateOf(1f) }
    var containerHeight by remember { mutableStateOf(1f) }
    var containerWidth by remember { mutableStateOf(0f) }

    fun handleSelection(y: Float) {
        val clampedY = y.coerceIn(0f, trackHeight)
        thumbOffsetY = clampedY
        val index = letterIndexForOffset(clampedY, trackHeight, alphabet.size)
        val letter = alphabet[index]
        if (letter != activeLetter) {
            activeLetter = letter
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            resolveTargetListIndex(alphabet, index, alphabetMap)?.let(onLetterSelected)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .onGloballyPositioned {
                containerHeight = it.size.height.toFloat()
                containerWidth = it.size.width.toFloat()
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating, zoomed-in letter preview bubble.
        AnimatedVisibility(
            visible = isDragging && activeLetter != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    // thumbOffsetY is relative to the track; translate it into
                    // the parent's coordinate space and centre the bubble on it.
                    // Horizontally, keep the bubble hugging the track's left
                    // edge (2dp gap) so it reads as attached to the sidebar.
                    val rawY = trackTop + thumbOffsetY - bubbleSize.toPx() / 2f
                    val maxY = (containerHeight - bubbleSize.toPx()).coerceAtLeast(0f)
                    IntOffset(
                        x = (trackLeft - 2.dp.toPx() - containerWidth).roundToInt(),
                        y = rawY.coerceIn(0f, maxY).roundToInt()
                    )
                }
        ) {
            Surface(
                modifier = Modifier
                    .size(bubbleSize)
                    .shadow(10.dp, CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeLetter?.toString() ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Vertical A-Z track.
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .onGloballyPositioned { coordinates ->
                    trackTop = coordinates.positionInParent().y
                    trackLeft = coordinates.positionInParent().x
                    trackHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(alphabetMap) {
                    // A single gesture loop handles both taps and drags, so
                    // the bubble can never be cancelled mid-drag by a
                    // competing tap detector.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        activeLetter = null
                        handleSelection(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                            handleSelection(change.position.y)
                        }
                        isDragging = false
                        activeLetter = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            alphabet.forEach { char ->
                val hasItems = alphabetMap.containsKey(char)
                val isActive = char == activeLetter
                Text(
                    text = char.toString(),
                    fontSize = if (isActive) 11.sp else 9.sp,
                    fontWeight = if (isActive) {
                        FontWeight.ExtraBold
                    } else if (hasItems) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else if (hasItems) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }
        }
    }
}
