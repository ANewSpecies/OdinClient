package me.odinmain.ui.hud

import me.odinmain.OdinMain.mc
import me.odinmain.features.Module
import me.odinmain.features.ModuleManager.huds
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.ui.clickgui.util.ColorUtil.withAlpha
import me.odinmain.ui.clickgui.util.HoverHandler
import me.odinmain.ui.hud.EditHUDGui.dragging
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.gui.MouseUtils.isAreaHovered
import me.odinmain.utils.render.gui.animations.impl.EaseInOut
import me.odinmain.utils.render.gui.nvg.*
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.Display

/**
 * Class to render elements on hud
 *
 * Inspired by [FloppaClient](https://github.com/FloppaCoding/FloppaClient/blob/master/src/main/kotlin/floppaclient/ui/hud/HudElement.kt)
 * @author Stivais, Aton
 */
open class HudElement(
    x: Float = 0f,
    y: Float = 0f,
    val displayToggle: Boolean,
    defaultScale: Float = 2f,
    val render: Render = { 0f to 0f }
) {

    private var parentModule: Module? = null

    var enabled = false

    private val isEnabled: Boolean
        get() = parentModule?.enabled ?: false && enabled

    internal val xSetting: NumberSetting<Float>
    internal val ySetting: NumberSetting<Float>
    internal val scaleSetting: NumberSetting<Float>
    val enabledSetting: BooleanSetting = BooleanSetting("hudEnabled", default = enabled, hidden = true)


    val hoverHandler = HoverHandler(200)

    fun init(module: Module) {
        parentModule = module

        module.register(
            xSetting,
            ySetting,
            scaleSetting,
            enabledSetting,
            enabledSetting
        )

        huds.add(this)
    }

    internal var x: Float
        inline get() = xSetting.value
        set(value) {
            xSetting.value = value
        }

    internal var y: Float
        inline get() = ySetting.value
        set(value) {
            ySetting.value = value
        }

    internal var scale: Float
        inline get() = scaleSetting.value
        set(value) {
            if (value > .8f) scaleSetting.value = value
        }

    /**
     * Good practice to keep the example as similar to the actual hud.
     */
    open fun render(nvg: NVG, example: Boolean): Pair<Float, Float> {
        return nvg.render(example)
    }

    /**
     * Renders and positions the element and if it's rendering the example then draw a rect behind it.
     */
    fun draw(vg: NVG, example: Boolean) {
        if (displayToggle) enabled = enabledSetting.value
        if (!isEnabled) return

        xHud.max = Display.getWidth()
        yHud.max = Display.getHeight()

        vg.translate(x, y)
        vg.scale(scale, scale)
        GlStateManager.pushMatrix()
        val sr = ScaledResolution(mc)
        GlStateManager.scale(1.0 / sr.scaleFactor, 1.0 / sr.scaleFactor, 1.0)
        GlStateManager.translate(x, y, 0f)
        GlStateManager.scale(scale, scale, 1f)

        val (width, height) = render(vg, example)

        if (example) {
            hoverHandler.handle(x, y, width * scale, height * scale)
            var thickness = anim.get(.25f, 1f, !hasStarted)
            if (anim2.isAnimating() || dragging != null) {
                thickness += anim2.get(0f, .5f, dragging == null)
            }

            vg.rectOutline(
                -1.5f,
                -1.5f,
                3f + width,
                3f + height,
                Color.WHITE.withAlpha(percent / 100f),
                5f,
                thickness / (scale / 3)
            )
        }
        vg.resetTransform()
        GlStateManager.popMatrix()

        this.width = width
        this.height = height
    }

    fun accept(): Boolean {
        return isAreaHovered(x, y, width * scale, height * scale)
    }

    /**
     * Needs to be set for preview boxes to be displayed correctly
     */
    var width: Float = 10f

    /**
     * Needs to be set for preview boxes to be displayed correctly
     */
    var height: Float = 10f

    /**
     * Animation for clicking on it
     */
    val anim2 = EaseInOut(200)

    /** Wrapper */
    private inline val anim
        get() = hoverHandler.anim

    /** Wrapper */
    private inline val percent: Int
        get() = hoverHandler.percent()

    /** Wrapper */
    private inline val hasStarted: Boolean
        get() = hoverHandler.hasStarted

    /** Used for smooth resetting animations */
    internal var resetX: Float = 0f

    /** Used for smooth resetting animations */
    internal var resetY: Float = 0f

    /** Used for smooth resetting animations */
    internal var resetScale: Float = 0f

    private val xHud = NumberSetting("xHud", default = x, hidden = true, min = 0f, max = Display.getWidth())
    private val yHud = NumberSetting("yHud", default = y, hidden = true, min = 0f, max = Display.getHeight())

    init {
        val scaleHud = NumberSetting("scaleHud", defaultScale, 0.8f, 6.0f, 0.01f, hidden = true)

        this.xSetting = xHud
        this.ySetting = yHud
        this.scaleSetting = scaleHud
        this.enabled = enabledSetting.value
    }
}

typealias Render = NVG.(Boolean) -> Pair<Float, Float>