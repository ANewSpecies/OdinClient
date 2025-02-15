package me.odinmain.features

import me.odinmain.OdinMain.mc
import me.odinmain.events.impl.*
import me.odinmain.features.impl.dungeon.*
import me.odinmain.features.impl.floor7.*
import me.odinmain.features.impl.floor7.p3.InactiveWaypoints
import me.odinmain.features.impl.floor7.p3.TerminalSolver
import me.odinmain.features.impl.floor7.p3.TerminalTimes
import me.odinmain.features.impl.render.*
import me.odinmain.features.impl.skyblock.*
import me.odinmain.features.settings.AlwaysActive
import me.odinmain.ui.hud.HudElement
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.render.gui.nvg.drawNVG
import net.minecraft.network.Packet
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.reflect.full.hasAnnotation

/**
 * Class that contains all Modules and huds
 * @author Aton, Bonsai
 */
object ModuleManager {
    data class PacketFunction<T : Packet<*>>(
        val type: Class<T>,
        val function: (T) -> Unit,
        val shouldRun: () -> Boolean
    )

    data class MessageFunction(val filter: Regex, val shouldRun: () -> Boolean, val function: (String) -> Unit)
    data class TickTask(var ticksLeft: Int, val function: () -> Unit)

    val packetFunctions = mutableListOf<PacketFunction<Packet<*>>>()
    val messageFunctions = mutableListOf<MessageFunction>()
    val worldLoadFunctions = mutableListOf<() -> Unit>()
    val tickTasks = mutableListOf<TickTask>()
    val huds = arrayListOf<HudElement>()
    val executors = ArrayList<Pair<Module, Executor>>()

    val modules: ArrayList<Module> = arrayListOf(
        AutoDungeonRequeue,
        BlessingDisplay,
        ExtraStats,
        KeyHighlight,
        MimicMessage,
        TeammatesHighlight,
        TerracottaTimer,
        WatcherBar,
        TerminalSolver,
        TerminalTimes,
        DragonBoxes,
        DragonDeathCheck,
        DragonTimer,
        LeapHelper,
        MelodyMessage,
        NecronDropTimer,
        RelicAnnouncer,
        BPSDisplay,
        Camera,
        ClickedChests,
        ClickGUIModule,
        CustomESP,
        CPSDisplay,
        DragonHitboxes,
        GyroRange,
        NickHider,
        NoCursorReset,
        PersonalDragon,
        RenderOptimizer,
        ServerDisplay,
        Waypoints,
        AutoRenewCrystalHollows,
        AutoSprint,
        BlazeAttunement,
        CanClip,
        ChatCommands,
        DeployableTimer,
        DianaHelper,
        Reminders,
        VanqNotifier,
        Animations,
        SpaceHelmet,
        EscrowFix,
        DungeonWaypoints,
        SecretChime,
        ShareCoords,
        LeapMenu,
        PuzzleSolvers,
        ArrowHit,
        InactiveWaypoints,
    )


    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        tickTasks.removeAll {
            if (it.ticksLeft <= 0) {
                it.function()
                return@removeAll true
            }
            it.ticksLeft--
            false
        }
    }

    @SubscribeEvent
    fun onReceivePacket(event: ReceivePacketEvent) {
        packetFunctions
            .filter { it.type.isInstance(event.packet) && it.shouldRun.invoke() }
            .forEach { it.function(event.packet) }
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketSentEvent) {
        packetFunctions
            .filter { it.type.isInstance(event.packet) }
            .forEach { it.function(event.packet) }
    }

    @SubscribeEvent
    fun onChatPacket(event: ChatPacketEvent) {
        messageFunctions
            .filter { event.message matches it.filter && it.shouldRun() }
            .forEach { it.function(event.message) }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        worldLoadFunctions
            .forEach { it.invoke() }
    }

    @SubscribeEvent
    fun activateModuleKeyBinds(event: PreKeyInputEvent) {
        modules
            .filter { it.keyCode == event.keycode }
            .forEach { it.onKeybind() }
    }

    @SubscribeEvent
    fun activateModuleMouseBinds(event: PreMouseInputEvent) {
        modules
            .filter { it.keyCode + 100 == event.button }
            .forEach { it.onKeybind() }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Pre) {
        if (mc.currentScreen != null || event.type != RenderGameOverlayEvent.ElementType.ALL) return
        drawNVG {
            for (i in 0 until huds.size) {
                huds[i].draw(this, false)
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        executors.removeAll {
            if (!it.first.enabled && !it.first::class.hasAnnotation<AlwaysActive>()) return@removeAll false // pls test i cba
            it.second.run()
        }
    }

    inline fun getModuleByName(name: String): Module? = modules.firstOrNull { it.name.equals(name, true) }
}