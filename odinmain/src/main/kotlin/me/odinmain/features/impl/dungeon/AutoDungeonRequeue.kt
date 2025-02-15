package me.odinmain.features.impl.dungeon

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.odinmain.OdinMain.scope
import me.odinmain.events.impl.ChatPacketEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.skyblock.ChatCommands
import me.odinmain.features.settings.impl.DualSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.utils.skyblock.LocationUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.sendCommand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AutoDungeonRequeue : Module(
    name = "Auto Requeue",
    description = "Automatically starts a new dungeon at the end of a dungeon.",
    category = Category.DUNGEON
) {
    private val delay: Int by NumberSetting("Delay", 10, 0, 30, 1)
    private val type: Boolean by DualSetting("Type", "Normal", "Requeue", default = true)

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        if (!DungeonUtils.inDungeons || event.message != "                             > EXTRA STATS <") return
        if (ChatCommands.disableReque == true) {
            ChatCommands.disableReque = false
            return
        }
        scope.launch {
            delay(delay * 1000L)
            sendCommand(if (type) "instancerequeue" else "od ${LocationUtils.currentDungeon?.floor?.name?.lowercase()}")
        }
    }
}