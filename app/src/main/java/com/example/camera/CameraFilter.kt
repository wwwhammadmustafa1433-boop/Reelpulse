package com.example.camera

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter

enum class CameraFilterPreset(
    val id: String,
    val displayName: String,
    val tagline: String,
    val swatchColors: List<Color>,
    val colorMatrixValues: FloatArray,
    val hasScanlines: Boolean = false,
    val vignetteAlpha: Float = 0f,
    val tintOverlayColor: Color? = null
) {
    NORMAL(
        id = "normal",
        displayName = "Normal",
        tagline = "Original clip",
        swatchColors = listOf(Color(0xFF4C516D), Color(0xFF282B3D)),
        colorMatrixValues = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    CYBER_PULSE(
        id = "cyber_pulse",
        displayName = "Cyber",
        tagline = "Cyan & rose",
        swatchColors = listOf(Color(0xFF00F0FF), Color(0xFFFF2E54)),
        colorMatrixValues = floatArrayOf(
            1.18f, 0f, 0.08f, 0f, -10f,
            0f, 1.04f, 0.12f, 0f, -6f,
            0.06f, 0.14f, 1.28f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        ),
        vignetteAlpha = 0.25f,
        tintOverlayColor = Color(0xFF00F0FF).copy(alpha = 0.07f)
    ),
    WARM_SUNSET(
        id = "warm_sunset",
        displayName = "Sunset",
        tagline = "Warm amber",
        swatchColors = listOf(Color(0xFFFF8C42), Color(0xFFFF2E54)),
        colorMatrixValues = floatArrayOf(
            1.22f, 0.08f, 0f, 0f, 14f,
            0.04f, 1.05f, 0f, 0f, 6f,
            0f, 0f, 0.84f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        ),
        vignetteAlpha = 0.18f,
        tintOverlayColor = Color(0xFFFF8C42).copy(alpha = 0.09f)
    ),
    GOLDEN_HOUR(
        id = "golden_hour",
        displayName = "Golden",
        tagline = "Sunlit gold",
        swatchColors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
        colorMatrixValues = floatArrayOf(
            1.26f, 0.10f, 0f, 0f, 16f,
            0.06f, 1.14f, 0f, 0f, 8f,
            0f, 0f, 0.78f, 0f, -12f,
            0f, 0f, 0f, 1f, 0f
        ),
        vignetteAlpha = 0.14f,
        tintOverlayColor = Color(0xFFF59E0B).copy(alpha = 0.08f)
    ),
    NEON_NIGHT(
        id = "neon_night",
        displayName = "Neon",
        tagline = "Electric night",
        swatchColors = listOf(Color(0xFFEC4899), Color(0xFF6366F1)),
        colorMatrixValues = floatArrayOf(
            1.20f, 0f, 0.14f, 0f, -8f,
            0f, 0.94f, 0.10f, 0f, -10f,
            0.12f, 0.08f, 1.32f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        ),
        vignetteAlpha = 0.34f,
        tintOverlayColor = Color(0xFFEC4899).copy(alpha = 0.08f)
    ),
    NOIR_CINEMA(
        id = "noir_cinema",
        displayName = "Noir",
        tagline = "Crisp B&W",
        swatchColors = listOf(Color(0xFFE5E7EB), Color(0xFF1F2937)),
        colorMatrixValues = floatArrayOf(
            0.39f, 0.77f, 0.19f, 0f, -24f,
            0.39f, 0.77f, 0.19f, 0f, -24f,
            0.39f, 0.77f, 0.19f, 0f, -24f,
            0f, 0f, 0f, 1f, 0f
        ),
        vignetteAlpha = 0.42f
    ),
    VIVID_POP(
        id = "vivid_pop",
        displayName = "Vivid",
        tagline = "Punchy color",
        swatchColors = listOf(Color(0xFFFF2E54), Color(0xFFFBBF24)),
        colorMatrixValues = floatArrayOf(
            1.32f, -0.14f, -0.10f, 0f, -4f,
            -0.12f, 1.28f, -0.10f, 0f, -4f,
            -0.10f, -0.12f, 1.32f, 0f, -4f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    RETRO_VHS(
        id = "retro_vhs",
        displayName = "VHS 98",
        tagline = "90s tape",
        swatchColors = listOf(Color(0xFFF472B6), Color(0xFF2DD4BF)),
        colorMatrixValues = floatArrayOf(
            1.05f, 0.08f, 0.06f, 0f, 12f,
            0.04f, 0.96f, 0.08f, 0f, 8f,
            0.10f, 0.04f, 1.08f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f
        ),
        hasScanlines = true,
        vignetteAlpha = 0.30f
    ),
    EMERALD_FILM(
        id = "emerald_film",
        displayName = "Emerald",
        tagline = "Cool cinema",
        swatchColors = listOf(Color(0xFF10B981), Color(0xFF065F46)),
        colorMatrixValues = floatArrayOf(
            0.92f, 0.04f, 0f, 0f, -8f,
            0f, 1.15f, 0.08f, 0f, 6f,
            0f, 0.10f, 1.06f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
        ),
        vignetteAlpha = 0.22f
    ),
    DREAMY_GLOW(
        id = "dreamy_glow",
        displayName = "Dreamy",
        tagline = "Soft pastel",
        swatchColors = listOf(Color(0xFFC084FC), Color(0xFFF472B6)),
        colorMatrixValues = floatArrayOf(
            1.10f, 0.06f, 0.08f, 0f, 12f,
            0.04f, 1.02f, 0.06f, 0f, 10f,
            0.08f, 0.06f, 1.16f, 0f, 16f,
            0f, 0f, 0f, 1f, 0f
        ),
        tintOverlayColor = Color(0xFFC084FC).copy(alpha = 0.08f)
    );

    fun toAndroidColorFilter(): ColorMatrixColorFilter? {
        if (this == NORMAL) return null
        return ColorMatrixColorFilter(ColorMatrix(colorMatrixValues))
    }

    fun vignetteBrush(): Brush? {
        if (vignetteAlpha <= 0f) return null
        return Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = vignetteAlpha)
            )
        )
    }

    @OptIn(UnstableApi::class)
    fun toMedia3Effects(): List<Effect> {
        return when (this) {
            NORMAL -> emptyList()
            CYBER_PULSE -> listOf(
                RgbAdjustment.Builder()
                    .setRedScale(1.12f)
                    .setGreenScale(1.00f)
                    .setBlueScale(1.25f)
                    .build(),
                Contrast(0.18f),
                HslAdjustment.Builder().adjustSaturation(22f).build()
            )
            WARM_SUNSET -> listOf(
                RgbAdjustment.Builder()
                    .setRedScale(1.22f)
                    .setGreenScale(1.06f)
                    .setBlueScale(0.84f)
                    .build(),
                Contrast(0.10f)
            )
            GOLDEN_HOUR -> listOf(
                RgbAdjustment.Builder()
                    .setRedScale(1.26f)
                    .setGreenScale(1.12f)
                    .setBlueScale(0.78f)
                    .build(),
                HslAdjustment.Builder()
                    .adjustSaturation(18f)
                    .adjustLightness(4f)
                    .build(),
                Contrast(0.14f)
            )
            NEON_NIGHT -> listOf(
                RgbAdjustment.Builder()
                    .setRedScale(1.18f)
                    .setGreenScale(0.92f)
                    .setBlueScale(1.30f)
                    .build(),
                HslAdjustment.Builder()
                    .adjustSaturation(28f)
                    .build(),
                Contrast(0.22f)
            )
            NOIR_CINEMA -> listOf(
                RgbFilter.createGrayscaleFilter(),
                Contrast(0.28f)
            )
            VIVID_POP -> listOf(
                HslAdjustment.Builder().adjustSaturation(38f).build(),
                Contrast(0.16f)
            )
            RETRO_VHS -> listOf(
                RgbAdjustment.Builder()
                    .setRedScale(1.06f)
                    .setGreenScale(0.96f)
                    .setBlueScale(1.12f)
                    .build(),
                HslAdjustment.Builder()
                    .adjustSaturation(-14f)
                    .adjustLightness(4f)
                    .build()
            )
            EMERALD_FILM -> listOf(
                RgbAdjustment.Builder()
                    .setRedScale(0.90f)
                    .setGreenScale(1.14f)
                    .setBlueScale(1.06f)
                    .build(),
                Contrast(0.12f)
            )
            DREAMY_GLOW -> listOf(
                RgbAdjustment.Builder()
                    .setRedScale(1.10f)
                    .setGreenScale(1.02f)
                    .setBlueScale(1.16f)
                    .build(),
                HslAdjustment.Builder()
                    .adjustSaturation(14f)
                    .adjustLightness(6f)
                    .build()
            )
        }
    }

    companion object {
        fun fromId(id: String?): CameraFilterPreset {
            return entries.firstOrNull { it.id == id } ?: NORMAL
        }
    }
}
