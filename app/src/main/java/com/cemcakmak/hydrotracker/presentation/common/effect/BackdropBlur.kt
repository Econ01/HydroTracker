package com.cemcakmak.hydrotracker.presentation.common.effect

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader as AndroidShader
import android.graphics.Shader.TileMode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/*
* The separated Gaussian blur shaders (HORIZONTAL_BLUR_AGSL and VERTICAL_BLUR_WITH_TINT_AGSL)
* are adapted from Haze by Christopher Banes and the Haze project contributors.
*
* Haze is licensed under the Apache Licence, Version 2.0.
* See https://github.com/chrisbanes/haze for the original source.
*
* Adaptations made: converted from SKSL to AGSL, added progressive tint,
* and simplified for a single-source/single-effect use case.
*/

/**
 * State shared between a [backdropSource] and its matching [backdropBlur].
 *
 * The source layer holds an opaque snapshot of the backdrop content. The blur layer is an
 * intermediate buffer sized to the blur panel; the platform blur [RenderEffect] is applied to this
 * layer so the panel's own children can be drawn on top, unblurred.
 */
class BackdropBlurState internal constructor(
    internal val sourceLayer: GraphicsLayer,
    internal val blurLayer: GraphicsLayer
) {
    /** Window-space position of the source, used to align the backdrop under the effect. */
    internal var sourceCoordinates: LayoutCoordinates? = null
}

@Composable
fun rememberBackdropBlurState(): BackdropBlurState {
    val sourceLayer = rememberGraphicsLayer()
    val blurLayer = rememberGraphicsLayer()
    return remember(sourceLayer, blurLayer) { BackdropBlurState(sourceLayer, blurLayer) }
}

/** Vertical progressive ramp: full blur at [startFraction], fading to none at [endFraction]. */
data class BackdropProgressive(
    val startFraction: Float = 0f,
    val endFraction: Float = 1f
)

/**
 * @param blurRadius maximum blur radius.
 * @param progressive vertical ramp, or null for a uniform blur across the whole region.
 * @param tint optional translucent wash over the blur ([Color.Transparent] = off).
 * @param noise optional film-grain amount in [0, 1] (0 = off).
 */
data class BackdropBlurStyle(
    val blurRadius: Dp = 24.dp,
    val progressive: BackdropProgressive? = null,
    val tint: Color = Color.Transparent,
    val noise: Float = 0f
)

/**
 * Marks [this] as the backdrop captured for [state].
 *
 * The content is recorded into [BackdropBlurState.sourceLayer] and then drawn via that layer.
 */
fun Modifier.backdropSource(state: BackdropBlurState): Modifier =
    this
        .onGloballyPositioned { state.sourceCoordinates = it }
        .drawWithContent {
            val layer = state.sourceLayer
            layer.record(size = IntSize(size.width.toInt(), size.height.toInt())) {
                this@drawWithContent.drawContent()
            }
            drawLayer(layer)
        }

/**
 * Draws the [state] backdrop, blurred per [style], behind this node's own content. The panel
 * content is drawn unblurred on top.
 *
 * API 33+ only; callers must provide their own fallback on older devices.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.backdropBlur(
    state: BackdropBlurState,
    style: BackdropBlurStyle,
    shape: Shape = RectangleShape
): Modifier = composed {
    val density = LocalDensity.current
    val effectCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    this
        .onGloballyPositioned { effectCoordinates.value = it }
        .clip(shape)
        .drawWithContent {
            val source = state.sourceCoordinates
            val effect = effectCoordinates.value

            if (source != null && effect != null && source.isAttached && effect.isAttached) {
                val offset = effect.localPositionOf(source, Offset.Zero)
                val panelSize = IntSize(size.width.toInt(), size.height.toInt())
                val blurRadiusPx = with(density) { style.blurRadius.toPx() }

                // Copy the relevant region of the source into a layer sized to the blur panel.
                val blurLayer = state.blurLayer
                blurLayer.record(size = panelSize) {
                    translate(offset.x, offset.y) {
                        drawLayer(state.sourceLayer)
                    }
                }

                // Apply the blur RenderEffect to the intermediate layer
                blurLayer.renderEffect = createBlurRenderEffect(
                    blurRadiusPx = blurRadiusPx,
                    size = size,
                    progressive = style.progressive,
                    tint = style.tint
                )

                drawLayer(blurLayer)
            }

            // Draw the panel's own content (text, icons, etc.) unblurred on top.
            drawContent()
        }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createBlurRenderEffect(
    blurRadiusPx: Float,
    size: Size,
    progressive: BackdropProgressive?,
    tint: Color
): RenderEffect {
    val blur = if (progressive != null) {
        // Two-pass separated Gaussian blur, modulated by a vertical gradient mask.
        val maskShader = createProgressiveMaskShader(size, progressive)
        val crop = floatArrayOf(0f, 0f, size.width, size.height)

        val horizontalShader = RuntimeShader(HORIZONTAL_BLUR_AGSL).apply {
            setFloatUniform("blurRadius", blurRadiusPx)
            setFloatUniform("crop", crop[0], crop[1], crop[2], crop[3])
            setInputShader("mask", maskShader)
        }
        val verticalShader = RuntimeShader(VERTICAL_BLUR_WITH_TINT_AGSL).apply {
            setFloatUniform("blurRadius", blurRadiusPx)
            setFloatUniform("crop", crop[0], crop[1], crop[2], crop[3])
            setInputShader("mask", maskShader)
            setFloatUniform("tint", tint.red, tint.green, tint.blue, tint.alpha)
        }

        val horizontal = AndroidRenderEffect.createRuntimeShaderEffect(horizontalShader, "content")
        val vertical = AndroidRenderEffect.createRuntimeShaderEffect(verticalShader, "content")
        // Chain: horizontal blur first, then vertical blur.
        AndroidRenderEffect.createChainEffect(vertical, horizontal)
    } else {
        // Uniform blur: use the platform blur RenderEffect directly.
        AndroidRenderEffect.createBlurEffect(
            blurRadiusPx,
            blurRadiusPx,
            TileMode.CLAMP
        )
    }

    return if (tint.alpha > 0.005f && progressive == null) {
        // Uniform blur: tint is applied as a single colour filter over the whole output.
        // Coller should set the transparency of the tint colour
        val tintColor = android.graphics.Color.argb(
            (tint.alpha * 255).toInt(),
            (tint.red * 255).toInt(),
            (tint.green * 255).toInt(),
            (tint.blue * 255).toInt()
        )
        AndroidRenderEffect.createColorFilterEffect(
            PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN),
            blur
        ).asComposeRenderEffect()
    } else {
        blur.asComposeRenderEffect()
    }
}

private fun createProgressiveMaskShader(size: Size, progressive: BackdropProgressive): AndroidShader {
    val startFraction = progressive.startFraction.coerceIn(0f, 1f)
    val endFraction = progressive.endFraction.coerceIn(0f, 1f)
    val brush = Brush.verticalGradient(
        startFraction to Color.Black,
        endFraction to Color.Transparent
    )
    return checkNotNull(brush.toShader(size)) { "Gradient brush must produce a shader" }
}

private fun Brush.toShader(size: Size): AndroidShader? = when (this) {
    is ShaderBrush -> createShader(size)
    else -> null
}

/**
 * Horizontal-pass separated Gaussian blur. A matching vertical pass is required for the final
 * result. The blur radius is modulated by the alpha of mask (black = full blur,
 * transparent = none).
 *
 * Adapted from Haze's HazeBlurShaders.HORIZONTAL_BLUR_SKSL.
 */
private val HORIZONTAL_BLUR_AGSL = """
    uniform shader content;
    uniform float blurRadius;
    uniform float4 crop;
    uniform shader mask;

    const half maxRadius = 150.0;

    float gaussian(float x, float sigma) {
        return exp(-(x * x) / (2.0 * sigma * sigma));
    }

    float4 blur(float2 coord, float radius) {
        half r = floor(radius + 0.5);
        float sigma = max(radius / 2.0, 1.0);
        float weightSum = 1.0;
        float4 result = float4(content.eval(coord));

        for (half i = 1.0; i < maxRadius; i += 2.0) {
            if (i >= r) { break; }
            float fi = float(i);
            float weightL = gaussian(fi, sigma);
            float weightH = gaussian(fi + 1.0, sigma);
            float weight = weightL + weightH;
            float2 offset = float2(fi + weightH / weight, 0.0);

            float2 newCoord = coord - offset;
            if (newCoord.x >= crop.x && newCoord.y >= crop.y) {
                result += weight * float4(content.eval(newCoord));
                weightSum += weight;
            }

            newCoord = coord + offset;
            if (newCoord.x <= crop.z && newCoord.y <= crop.w) {
                result += weight * float4(content.eval(newCoord));
                weightSum += weight;
            }
        }

        if (r < maxRadius && mod(float(r), 2.0) == 1.0) {
            float weight = gaussian(float(r), sigma);
            float2 offset = float2(float(r), 0.0);

            float2 newCoord = coord - offset;
            if (newCoord.x >= crop.x && newCoord.y >= crop.y) {
                result += weight * float4(content.eval(newCoord));
                weightSum += weight;
            }

            newCoord = coord + offset;
            if (newCoord.x <= crop.z && newCoord.y <= crop.w) {
                result += weight * float4(content.eval(newCoord));
                weightSum += weight;
            }
        }

        return result / weightSum;
    }

    half4 main(float2 coord) {
        float2 maskCoord = max(coord - crop.xy, float2(0.0, 0.0));
        float intensity = mask.eval(maskCoord).a;
        return half4(blur(coord, mix(0.0, blurRadius, intensity)));
    }
""".trimIndent()

/**
 * Vertical-pass separated Gaussian blur, and the final compositing pass. See
 * [HORIZONTAL_BLUR_AGSL] for the horizontal pass. The tint is modulated by the same mask
 * intensity as the blur, so the tint fades where the blur fades.
 */
private val VERTICAL_BLUR_WITH_TINT_AGSL = """
    uniform shader content;
    uniform float blurRadius;
    uniform float4 crop;
    uniform shader mask;
    uniform float4 tint;

    const half maxRadius = 150.0;

    float gaussian(float x, float sigma) {
        return exp(-(x * x) / (2.0 * sigma * sigma));
    }

    float4 blur(float2 coord, float radius) {
        half r = floor(radius + 0.5);
        float sigma = max(radius / 2.0, 1.0);
        float weightSum = 1.0;
        float4 result = float4(content.eval(coord));

        for (half i = 1.0; i < maxRadius; i += 2.0) {
            if (i >= r) { break; }
            float fi = float(i);
            float weightL = gaussian(fi, sigma);
            float weightH = gaussian(fi + 1.0, sigma);
            float weight = weightL + weightH;
            float2 offset = float2(0.0, fi + weightH / weight);

            float2 newCoord = coord - offset;
            if (newCoord.x >= crop.x && newCoord.y >= crop.y) {
                result += weight * float4(content.eval(newCoord));
                weightSum += weight;
            }

            newCoord = coord + offset;
            if (newCoord.x <= crop.z && newCoord.y <= crop.w) {
                result += weight * float4(content.eval(newCoord));
                weightSum += weight;
            }
        }

        if (r < maxRadius && mod(float(r), 2.0) == 1.0) {
            float weight = gaussian(float(r), sigma);
            float2 offset = float2(0.0, float(r));

            float2 newCoord = coord - offset;
            if (newCoord.x >= crop.x && newCoord.y >= crop.y) {
                result += weight * float4(content.eval(newCoord));
                weightSum += weight;
            }

            newCoord = coord + offset;
            if (newCoord.x <= crop.z && newCoord.y <= crop.w) {
                result += weight * float4(content.eval(newCoord));
                weightSum += weight;
            }
        }

        return result / weightSum;
    }

    half4 main(float2 coord) {
        float2 maskCoord = max(coord - crop.xy, float2(0.0, 0.0));
        float intensity = mask.eval(maskCoord).a;
        float4 result = blur(coord, mix(0.0, blurRadius, intensity));

        float4 tintF = float4(tint);
        if (tintF.a > 0.0) {
            result = float4(mix(result.rgb, tintF.rgb, tintF.a * intensity), result.a);
        }

        return half4(result);
    }
""".trimIndent()
