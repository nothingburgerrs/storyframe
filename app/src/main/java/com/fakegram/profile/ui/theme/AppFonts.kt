@file:OptIn(ExperimentalTextApi::class)

package com.fakegram.profile.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontStyle as PlatformFontStyle
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs

/** One choice in Settings > Font: its picture files by weight (all under assets/fonts). */
class AppFontOption(val id: String, val label: String, val files: List<Pair<Int, String>>)

object AppFonts {
    const val DEFAULT_ID = "roboto"
    private const val EMOJI_PATH = "fonts/apple_emoji.ttf"

    val options = listOf(
        AppFontOption("roboto", "Roboto", listOf(400 to "fonts/roboto_400Regular.ttf", 500 to "fonts/roboto_500Medium.ttf", 700 to "fonts/roboto_700Bold.ttf")),
        AppFontOption("inter", "Inter", listOf(400 to "fonts/inter_400Regular.ttf", 500 to "fonts/inter_500Medium.ttf", 700 to "fonts/inter_700Bold.ttf")),
        AppFontOption("poppins", "Poppins", listOf(400 to "fonts/poppins_400Regular.ttf", 500 to "fonts/poppins_500Medium.ttf", 700 to "fonts/poppins_700Bold.ttf")),
        AppFontOption("nunito", "Nunito", listOf(400 to "fonts/nunito_400Regular.ttf", 700 to "fonts/nunito_700Bold.ttf")),
        AppFontOption("playfair", "Playfair Display", listOf(400 to "fonts/playfair_400Regular.ttf", 700 to "fonts/playfair_700Bold.ttf")),
        AppFontOption("lora", "Lora", listOf(400 to "fonts/lora_400Regular.ttf", 700 to "fonts/lora_700Bold.ttf")),
        AppFontOption("robotomono", "Roboto Mono", listOf(400 to "fonts/robotomono_400Regular.ttf", 700 to "fonts/robotomono_700Bold.ttf")),
        AppFontOption("caveat", "Caveat", listOf(400 to "fonts/caveat_400Regular.ttf", 700 to "fonts/caveat_700Bold.ttf")),
        AppFontOption("dancing", "Dancing Script", listOf(400 to "fonts/dancing_400Regular.ttf", 700 to "fonts/dancing_700Bold.ttf")),
        AppFontOption("pacifico", "Pacifico", listOf(400 to "fonts/pacifico_400Regular.ttf"))
    )

    fun byId(id: String?): AppFontOption = options.firstOrNull { it.id == id } ?: options.first()

    /** A Compose font family for showing the option's name in its own letters (no emoji fallback needed there). */
    fun previewFamily(context: Context, option: AppFontOption): FontFamily = FontFamily(
        option.files.map { (weight, path) ->
            androidx.compose.ui.text.font.Font(path, context.assets, FontWeight(weight))
        }
    )

    internal fun emojiPath() = EMOJI_PATH
}

/** The phone's current font family. Text that names no font of its own takes this one (see StoryframeTheme). */
val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** For text styles built by hand: use the phone's font unless the style already picks one. */
@Composable
fun TextStyle.inAppFont(): TextStyle = if (fontFamily == null) copy(fontFamily = LocalAppFontFamily.current) else this

/**
 * The font family behind a Settings > Font choice. Every weight is a Typeface made on demand: the chosen font first,
 * then the bundled Apple-style emoji, then the system fonts (so Korean, Japanese and so on still show). If a
 * typeface cannot be built, that weight quietly uses the normal system font.
 */
object AppFontFamilies {
    private val cache = HashMap<String, FontFamily>()

    fun familyFor(context: Context, option: AppFontOption): FontFamily = synchronized(cache) {
        cache.getOrPut(option.id) {
            val app = context.applicationContext
            val emoji = EmojiFamilyHolder(app)
            FontFamily(listOf(400, 500, 600, 700).map { weight -> ComposedFont(weight, option, app, emoji) })
        }
    }
}

private class EmojiFamilyHolder(private val context: Context) {
    private var family: android.graphics.fonts.FontFamily? = null
    private var tried = false

    @Synchronized
    fun get(): android.graphics.fonts.FontFamily? {
        if (!tried) {
            tried = true
            family = try {
                android.graphics.fonts.FontFamily.Builder(Font.Builder(context.assets, AppFonts.emojiPath()).build()).build()
            } catch (t: Throwable) {
                null
            }
        }
        return family
    }
}

private object ComposedTypefaceLoader : AndroidFont.TypefaceLoader {
    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? = (font as? ComposedFont)?.typeface()
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? = (font as? ComposedFont)?.typeface()
}

private class ComposedFont(
    private val weightValue: Int,
    private val option: AppFontOption,
    private val context: Context,
    private val emoji: EmojiFamilyHolder
) : AndroidFont(FontLoadingStrategy.Blocking, ComposedTypefaceLoader, FontVariation.Settings()) {

    override val weight: FontWeight = FontWeight(weightValue)
    override val style: FontStyle = FontStyle.Normal

    private var built: Typeface? = null
    private var tried = false

    @Synchronized
    fun typeface(): Typeface? {
        if (!tried) {
            tried = true
            built = try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) null else build()
            } catch (t: Throwable) {
                null
            }
        }
        return built
    }

    private fun build(): Typeface {
        val (fileWeight, path) = option.files.minByOrNull { abs(it.first - weightValue) } ?: option.files.first()
        val textFont = Font.Builder(context.assets, path).setWeight(fileWeight).build()
        val textFamily = android.graphics.fonts.FontFamily.Builder(textFont).build()
        val builder = Typeface.CustomFallbackBuilder(textFamily)
        emoji.get()?.let { builder.addCustomFallback(it) }
        builder.setSystemFallback("sans-serif")
        builder.setStyle(PlatformFontStyle(fileWeight, PlatformFontStyle.FONT_SLANT_UPRIGHT))
        return builder.build()
    }

    override fun equals(other: Any?): Boolean = other is ComposedFont && other.weightValue == weightValue && other.option.id == option.id
    override fun hashCode(): Int = 31 * option.id.hashCode() + weightValue
}
