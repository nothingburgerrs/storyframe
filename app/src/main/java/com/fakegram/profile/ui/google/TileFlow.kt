@file:OptIn(
    androidx.compose.animation.core.ExperimentalAnimatableApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.fakegram.profile.ui.google

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.CoroutineScope

/**
 * Animates a composable's size and position toward wherever its parent lays it out, so a card
 * that jumps from a half-width slot to a full-width row visibly grows into it (and its
 * neighbours glide out of the way) instead of snapping.
 */
class BoundsAnimation(
    internal val scope: LookaheadScope,
    internal val coroutineScope: CoroutineScope
) {
    internal val size = DeferredTargetAnimation(IntSize.VectorConverter)
    internal val offset = DeferredTargetAnimation(IntOffset.VectorConverter)
}

@Composable
fun LookaheadScope.rememberBoundsAnimation(): BoundsAnimation {
    val coroutineScope = rememberCoroutineScope()
    val lookaheadScope = this
    return remember { BoundsAnimation(lookaheadScope, coroutineScope) }
}

// Soft, slightly under-damped spring: fast start, long gentle settle (Google's "emphasized" feel).
private val BoundsSpec = spring<IntSize>(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)
private val OffsetSpec = spring<IntOffset>(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)

fun Modifier.animateBounds(anim: BoundsAnimation): Modifier = with(anim.scope) {
    this@animateBounds.approachLayout(
        isMeasurementApproachInProgress = { lookaheadSize ->
            anim.size.updateTarget(lookaheadSize, anim.coroutineScope, BoundsSpec)
            !anim.size.isIdle
        },
        isPlacementApproachInProgress = { lookaheadCoordinates ->
            val target = lookaheadScopeCoordinates.localLookaheadPositionOf(lookaheadCoordinates)
            anim.offset.updateTarget(target.round(), anim.coroutineScope, OffsetSpec)
            !anim.offset.isIdle
        }
    ) { measurable, _ ->
        val (w, h) = anim.size.updateTarget(lookaheadSize, anim.coroutineScope, BoundsSpec)
        val placeable = measurable.measure(Constraints.fixed(w.coerceAtLeast(0), h.coerceAtLeast(0)))
        layout(placeable.width, placeable.height) {
            coordinates?.let { coords ->
                val target = lookaheadScopeCoordinates.localLookaheadPositionOf(coords).round()
                val animated = anim.offset.updateTarget(target, anim.coroutineScope, OffsetSpec)
                val current = lookaheadScopeCoordinates.localPositionOf(coords, Offset.Zero).round()
                val delta = animated - current
                placeable.place(delta.x, delta.y)
            } ?: placeable.place(0, 0)
        }
    }
}

/** Marks a child of [TileFlow] as an expanded (full-width) or collapsed (half-width) tile. */
fun Modifier.tileExpanded(expanded: Boolean): Modifier = this.layoutId(expanded)

/**
 * Two-column flow: expanded tiles take a full-width row each and are placed first; the remaining
 * tiles fill two per row beneath them. Children are always composed in the same order, so a tile
 * keeps its identity when it moves - which is what lets [animateBounds] morph it in place.
 */
@Composable
fun TileFlow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = 12.dp.roundToPx()
        val full = constraints.maxWidth
        val half = (full - gap) / 2
        val placements = arrayOfNulls<androidx.compose.ui.layout.Placeable>(measurables.size)
        val positions = arrayOfNulls<IntOffset>(measurables.size)

        var y = 0
        var first = true
        val expandedIdx = measurables.indices.filter { measurables[it].layoutId == true }
        val collapsedIdx = measurables.indices.filter { measurables[it].layoutId != true }

        for (i in expandedIdx) {
            if (!first) y += gap
            first = false
            val p = measurables[i].measure(Constraints(full, full, 0, Constraints.Infinity))
            placements[i] = p
            positions[i] = IntOffset(0, y)
            y += p.height
        }
        for (pair in collapsedIdx.chunked(2)) {
            if (!first) y += gap
            first = false
            // Cards in a row share one height (as on Google), so ask for the tallest first.
            val rowH = pair.maxOf { measurables[it].maxIntrinsicHeight(half) }
            pair.forEachIndexed { col, i ->
                placements[i] = measurables[i].measure(Constraints(half, half, rowH, rowH))
                positions[i] = IntOffset(col * (half + gap), y)
            }
            y += rowH
        }

        layout(full, y) {
            for (i in measurables.indices) placements[i]?.place(positions[i] ?: IntOffset.Zero)
        }
    }
}
