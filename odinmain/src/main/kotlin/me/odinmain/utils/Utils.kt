@file:Suppress("NOTHING_TO_INLINE", "FunctionName", "NAME_SHADOWING")

package me.odinmain.utils

import me.odinmain.features.ModuleManager
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

private val FORMATTING_CODE_PATTERN = Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE)

/**
 * Returns the string without any minecraft formatting codes.
 */
val String?.noControlCodes: String
    get() = this?.let { FORMATTING_CODE_PATTERN.replace(it, "") } ?: ""

/**
 * Checks if the current string contains at least one of the specified strings.
 *
 * @param options List of strings to check.
 * @param ignoreCase If comparison should be case-sensitive or not.
 * @return `true` if the string contains at least one of the specified options, otherwise `false`.
 */
fun String.containsOneOf(vararg options: String, ignoreCase: Boolean = false): Boolean {
    return options.any { this.contains(it, ignoreCase) }
}

/**
 * Checks if the current string contains at least one of the specified strings.
 *
 * @param options List of strings to check.
 * @param ignoreCase If comparison should be case-sensitive or not.
 * @return `true` if the string contains at least one of the specified options, otherwise `false`.
 */
fun String.containsOneOf(options: List<String>, ignoreCase: Boolean = false): Boolean {
    return options.any { this.contains(it, ignoreCase) }
}

/**
 * Checks if the current object is equal to at least one of the specified objects.
 *
 * @param options List of other objects to check.
 * @return `true` if the object is equal to one of the specified objects.
 */
fun Any?.equalsOneOf(vararg options: Any): Boolean {
    return options.any { this == it }
}

/**
 * Checks if the first value in the pair equals the first argument and the second value in the pair equals the second argument.
 */
fun Pair<Any?, Any?>?.equal(first: Any?, second: Any?): Boolean {
    return this?.first == first && this?.second == second
}

/**
 * Floors the double.
 */
inline fun Double.floor(): Double = floor(this)

/**
 * Rounds the double to the specified number of decimal places.
 */
fun Double.round(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return round(this * multiplier) / multiplier
}

/**
 * Rounds the float to the specified number of decimal places.
 */
fun Float.round(decimals: Int): Float {
    val multiplier = 10f.pow(decimals)
    return round(this * multiplier) / multiplier
}

val ContainerChest.name: String
    get() = this.lowerChestInventory.displayName.unformattedText

operator fun Number.div(number: Number): Number {
    return this.toDouble() / number.toDouble()
}

operator fun Number.times(number: Number): Number {
    return this.toDouble() * number.toDouble()
}

operator fun Number.minus(number: Number): Number {
    return this.toDouble() - number.toDouble()
}

operator fun Number.plus(number: Number): Number {
    return this.toDouble() + number.toDouble()
}

operator fun Number.unaryMinus(): Number {
    return -this.toDouble()
}

operator fun Number.compareTo(number: Number): Int {
    return this.toDouble().compareTo(number.toDouble())
}

fun Number.coerceInNumber(min: Number, max: Number): Number {
    if (this < min) return min
    if (this > max) return max
    return this
}

/**
 * Returns a random number between the specified range.
 */
fun IntRange.getRandom(): Int {
    return this.toList().getRandom()
}

/**
 * Returns a random element from the specified collection.
 */
fun <T> Collection<T>.getRandom(): T {
    return this.elementAt((Math.random() * this.size).floor().toInt())
}

/**
 * Returns the value of the pair which gives the lowest value when passed to the selector function.
 * If both values are equal, the first value is returned.
 */
inline fun <T>Pair<T, T>.minBy(selector: (T) -> Number): T {
    return if (selector(this.first) <= selector(this.second)) this.first else this.second
}

/**
 * Converts the HSB color to RGB Int.
 * @author PolyUI
 */
fun HSBtoRGB(hue: Float, saturation: Float, brightness: Float): Int {
    var r = 0
    var g = 0
    var b = 0
    if (saturation == 0f) {
        b = (brightness * 255.0f + 0.5f).toInt()
        g = b
        r = g
    } else {
        val h = (hue - floor(hue)) * 6.0f
        val f = h - floor(h)
        val p = brightness * (1f - saturation)
        val q = brightness * (1f - saturation * f)
        val t = brightness * (1f - saturation * (1f - f))
        when (h.toInt()) {
            0 -> {
                r = (brightness * 255f + 0.5f).toInt()
                g = (t * 255f + .5f).toInt()
                b = (p * 255f + .5f).toInt()
            }

            1 -> {
                r = (q * 255f + .5f).toInt()
                g = (brightness * 255.0f + .5f).toInt()
                b = (p * 255f + .5f).toInt()
            }

            2 -> {
                r = (p * 255f + .5f).toInt()
                g = (brightness * 255f + .5f).toInt()
                b = (t * 255f + .5f).toInt()
            }

            3 -> {
                r = (p * 255f + .5f).toInt()
                g = (q * 255f + .5f).toInt()
                b = (brightness * 255f + .5f).toInt()
            }

            4 -> {
                r = (t * 255f + .5f).toInt()
                g = (p * 255f + .5f).toInt()
                b = (brightness * 255f + .5f).toInt()
            }

            5 -> {
                r = (brightness * 255f + .5f).toInt()
                g = (p * 255f + .5f).toInt()
                b = (q * 255f + .5f).toInt()
            }
        }
    }
    return -0x1000000 or (r shl 16) or (g shl 8) or (b shl 0)
}

/**
 * Taken from PolyUI
 */
fun RGBtoHSB(r: Int, g: Int, b: Int, out: FloatArray? = null): FloatArray {
    var hue: Float
    val saturation: Float
    val brightness: Float

    val out = out ?: FloatArray(3)

    var cmax = if (r > g) r else g
    if (b > cmax) cmax = b
    var cmin = if (r < g) r else g
    if (b < cmin) cmin = b

    brightness = cmax.toFloat() / 255f
    saturation = if (cmax != 0) (cmax - cmin).toFloat() / cmax.toFloat() else 0f
    if (saturation == 0f) {
        hue = 0f
    } else {
        val redc = (cmax - r).toFloat() / (cmax - cmin).toFloat()
        val greenc = (cmax - g).toFloat() / (cmax - cmin).toFloat()
        val bluec = (cmax - b).toFloat() / (cmax - cmin).toFloat()
        hue = if (r == cmax) bluec - greenc else if (g == cmax) 2f + redc - bluec else 4f + greenc - redc
        hue /= 6f
        if (hue < 0) hue += 1f
    }
    out[0] = hue
    out[1] = saturation
    out[2] = brightness
    return out
}

/**
 * Posts an event to the event bus and catches any errors.
 * @author Skytils
 */
fun Event.postAndCatch(): Boolean {
    return runCatching {
        MinecraftForge.EVENT_BUS.post(this)
    }.onFailure {
        it.printStackTrace()
        modMessage("Caught and logged an ${it::class.simpleName ?: "error"} at ${this::class.simpleName}. Please report this!")
    }.getOrDefault(isCanceled)
}

/**
 * Executes the specified function after the specified number of **minecraft** ticks.
 * @param ticks The number of ticks to wait.
 * @param func The function to execute after the specified number of
 */
fun runIn(ticks: Int, func: () -> Unit) {
    if (ticks <= 0) {
        func()
        return
    }
    ModuleManager.tickTasks.add(ModuleManager.TickTask(ticks, func))
}