package com.fakegram.profile.ui.post

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Instagram's post-action icons, drawn as 24x24 outline vectors (2px round strokes) so they keep the
 * app's chunky, rounded look instead of Material's thinner glyphs. Tint them with `Icon(tint = ...)`.
 */
object IgIcons {
    private const val HEART =
        "M16.792 3.904A4.989 4.989 0 0 1 21.5 9.122c0 3.072-2.652 4.959-5.197 7.222-2.512 2.243-3.865 3.469-4.303 3.752" +
            "-.477-.309-2.143-1.823-4.303-3.752C5.141 14.072 2.5 12.167 2.5 9.122a4.989 4.989 0 0 1 4.708-5.218 " +
            "4.21 4.21 0 0 1 3.675 1.941c.84 1.175.98 1.763 1.12 1.763s.278-.588 1.11-1.766a4.17 4.17 0 0 1 3.679-1.938Z"

    private fun outline(name: String, path: String, filled: Boolean = false, width: Float = 2f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = addPathNodes(path),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()

    val Heart = outline("IgHeart", HEART)
    val HeartFilled = outline("IgHeartFilled", HEART, filled = true)

    /** Round speech bubble with the tail at the bottom right. */
    val Comment = outline("IgComment", "M20.656 17.008a9.993 9.993 0 1 0-3.59 3.615L22 22Z")

    /** Two looping arrows: top one points right, bottom one points left. */
    val Repost = outline(
        "IgRepost",
        "M3.5 11V9.5a3.5 3.5 0 0 1 3.5-3.5h12.5 M16.5 3 19.5 6 16.5 9 " +
            "M20.5 13v1.5a3.5 3.5 0 0 1-3.5 3.5H4.5 M7.5 15 4.5 18 7.5 21"
    )

    /** Paper plane. */
    val Share = outline("IgShare", "M22 3 9.218 10.083 M11.698 20.334 22 3.001H2l7.218 7.082 2.48 10.251Z")

    val Bookmark = outline("IgBookmark", "M20 21 12 13.44 4 21 4 3 20 3 20 21Z")

    /** The two-line "options" glyph: long line on top, short line below, both left-aligned. */
    val Menu = outline("IgMenu", "M4 8.5H20 M4 15.5H15", width = 2.4f)
}
