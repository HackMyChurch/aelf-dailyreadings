/**
 * Custom reimplementation of `detectTransformGestures` from Jetpack Compose.
 *
 * Reason for reimplementation:
 * - Needed fine-grained control over pan, zoom, and rotation gesture detection.
 * - Added `panDirection` tracking to distinguish horizontal vs vertical gestures.
 * - Introduced `onPanEnd` callback with velocity information for inertial scrolling.
 * - Preserved compatibility with both locked pan/zoom modes and unrestricted gestures.
 * - Ensured precise control of touch slop thresholds and velocity tracking beyond
 *   the capabilities of the stock implementation.
 */
package co.epitre.aelf_lectures.compose.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue

suspend fun PointerInputScope.customDetectTransformGestures(
    panZoomLock: Boolean = false,
    onPanEnd: (velocityY: Float, panDirection: Int) -> Unit = { _, _ -> },
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float, panDirection: Int) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        val tracker = VelocityTracker()

        awaitFirstDown(requireUnconsumed = false)

        var panDirection: Int? = null

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }

            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (panDirection == null) {
                    panDirection =
                        if (panChange.x.absoluteValue > panChange.y.absoluteValue) {
                            0
                        } else {
                            1
                        }
                }


                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (
                        zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f || zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation, panDirection)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }

                    tracker.addPointerInputChange(event.changes[0])
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })

        if (zoom == 1f && pan != Offset.Zero) {
            val velocity = tracker.calculateVelocity()
            tracker.resetTracking()
            if (panDirection == 0) {
                onPanEnd(velocity.x, panDirection)
            } else if (panDirection == 1) {
                onPanEnd(velocity.y, panDirection)
            }
        }
    }
}