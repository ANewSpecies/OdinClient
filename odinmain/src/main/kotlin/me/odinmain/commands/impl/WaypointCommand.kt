package me.odinmain.commands.impl

import me.odinmain.OdinMain.display
import me.odinmain.OdinMain.mc
import me.odinmain.commands.AbstractCommand
import me.odinmain.features.impl.render.WaypointManager
import me.odinmain.ui.waypoint.WaypointGUI
import me.odinmain.utils.floored
import me.odinmain.utils.floor
import me.odinmain.utils.render.Color
import me.odinmain.utils.skyblock.modMessage
import me.odinmain.utils.skyblock.partyMessage
import me.odinmain.utils.skyblock.PlayerUtils.posX
import me.odinmain.utils.skyblock.PlayerUtils.posY
import me.odinmain.utils.skyblock.PlayerUtils.posZ
import java.util.*

object WaypointCommand : AbstractCommand("waypoint", "wp", "odwp",) {
    init {
        does { modMessage("§cArguments empty. §rUsage: gui, share, here, add, help") }

        "help" does { modMessage(HELP_MESSAGE) }
        "gui" does { display = WaypointGUI }

        "share" does {
            val message = when (it.size) {
                0 -> "x: ${posX.floor().toInt()} y: ${posY.floor().toInt()} z: ${posZ.floor().toInt()}"
                3 -> "x: ${it[0]} y: ${it[1]} z: ${it[2]}"
                else -> return@does modMessage("§cInvalid arguments, §r/wp share (x y z).")
            }
            partyMessage(message)
        }

        "here" {
            does {
                modMessage("§cInvalid arguments. §r/wp here (temp | perm).")
            }

            "temp" does {
                WaypointManager.addTempWaypoint(vec3 = mc.thePlayer.positionVector.floored())
                modMessage("Added temporary waypoint.")
            }

            "perm" does {
                WaypointManager.addWaypoint(vec3 = mc.thePlayer.positionVector.floored(), color = randomColor())
                modMessage("Added permanent waypoint.")
            }
        }

        "add" {
            does {
                modMessage("§cInvalid arguments. §r/wp add (temp | perm) x y z.")
            }

            "temp" does {
                if (it.size != 3) return@does modMessage("§cInvalid coordinates")
                val pos = it.getInt() ?: return@does modMessage("§cInvalid coordinates")
                WaypointManager.addTempWaypoint(x = pos[0], y = pos[1], z = pos[2])
                modMessage("Added temporary waypoint at ${pos[0]}, ${pos[1]}, ${pos[2]}.")
            }

            "perm" does {
                if (it.size != 3) return@does modMessage("§cInvalid coordinates")
                val pos = it.getInt() ?: return@does modMessage("§cInvalid coordinates")
                WaypointManager.addWaypoint(x = pos[0], y = pos[1], z = pos[2], color = randomColor())
                modMessage("Added permanent waypoint at ${pos[0]}, ${pos[1]}, ${pos[2]}.")
            }
        }
    }

    private const val HELP_MESSAGE =
        " - GUI » §7Opens the Gui \n" +
                " - Share (x y z) » §7Sends a message with your current coords, unless coords are specified \n" +
                " - Here (temp | perm) » §7Adds a permanent or temporary waypoint at your current coords\n" +
                " - Add (temp | perm) x y z » §7Adds a permanent or temporary waypoint at the coords specified\n" +
                " - Help » §7Shows this message"

    private fun Array<out String>.getInt(start: Int = 0, end: Int = size): List<Int>? {
        val result = mutableListOf<Int>()
        for (i in start until end) {
            try {
                result.add(this[i].toInt())
            } catch (e: NumberFormatException) {
                return null
            }
        }
        return result
    }

    fun randomColor(): Color {
        val random = Random()

        val hue = random.nextFloat()
        val saturation = random.nextFloat() * 0.5f + 0.5f // High saturation
        val brightness = random.nextFloat() * 0.5f + 0.5f // High brightness

        return Color(hue, saturation, brightness)
    }
}