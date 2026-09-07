package com.psadi.apkextractor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AlphabeticalFastScroller(
    alphabetMap: Map<Char, Int>,
    onLetterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    var isDragging by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var thumbOffsetY by remember { mutableStateOf(0f) }
    var totalHeight by remember { mutableStateOf(1f) }

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    fun handleSelection(y: Float) {
        val clampedY = y.coerceIn(0f, totalHeight)
        thumbOffsetY = clampedY
        val index = ((clampedY / totalHeight) * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
        val letter = alphabet[index]
        if (letter != activeLetter) {
            activeLetter = letter
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val targetListIndex = alphabetMap[letter] ?: run {
                // Find nearest fallback letter in map
                alphabet.subList(index, alphabet.size).firstNotNullOfOrNull { alphabetMap[it] }
                    ?: alphabetMap.values.lastOrNull()
            }
            if (targetListIndex != null) {
                onLetterSelected(targetListIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Bubble Indicator on drag
        AnimatedVisibility(
            visible = isDragging && activeLetter != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    IntOffset(
                        x = (-56).dp.roundToPx(),
                        y = (thumbOffsetY - 28.dp.toPx()).roundToInt().coerceAtLeast(0)
                    )
                }
        ) {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeLetter?.toString() ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Vertical A-Z Track
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .onGloballyPositioned { coordinates ->
                    totalHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(alphabetMap) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            handleSelection(offset.y)
                            tryAwaitRelease()
                            isDragging = false
                            activeLetter = null
                        }
                    )
                }
                .pointerInput(alphabetMap) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            handleSelection(offset.y)
                        },
                        onDragEnd = {
                            isDragging = false
                            activeLetter = null
                        },
                        onDragCancel = {
                            isDragging = false
                            activeLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            handleSelection(change.position.y)
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            alphabet.forEach { char ->
                val hasItems = alphabetMap.containsKey(char)
                Text(
                    text = char.toString(),
                    fontSize = 9.sp,
                    fontWeight = if (hasItems) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (hasItems) {
                        MaterialTheme.colorScheme.primary
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
