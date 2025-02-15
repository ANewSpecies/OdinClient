package me.odinclient.features.impl.floor7.p3

import me.odinclient.utils.skyblock.PlayerUtils.rightClick
import me.odinmain.events.impl.BlockChangeEvent
import me.odinmain.events.impl.PostEntityMetadata
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.ui.clickgui.util.ColorUtil.withAlpha
import me.odinmain.utils.clock.Clock
import me.odinmain.utils.floor
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.world.RenderUtils
import me.odinmain.utils.runIn
import me.odinmain.utils.skyblock.devMessage
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.block.BlockButtonStone
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SimonSays : Module(
    name = "Simon Says",
    description = "Different features for the Simon Says puzzle in f7/m7.",
    category = Category.FLOOR7,
    tag = TagType.NEW
) {
    private val solver: Boolean by BooleanSetting("Solver")
    private val start: Boolean by BooleanSetting("Start", default = true, description = "Starts the device when it can be started.")
    private val startClicks: Int by NumberSetting("Start Clicks", 1, 1, 10).withDependency { start }
    private val startClickDelay: Int by NumberSetting("Start Click Delay", 3, 1, 5).withDependency { start }
    private val triggerBot: Boolean by BooleanSetting("Triggerbot")
    private val delay: Long by NumberSetting<Long>("Delay", 200, 70, 500).withDependency { triggerBot }
    private val fullBlock: Boolean by BooleanSetting("Full Block (needs SBC)", false).withDependency { triggerBot }
    private val clearAfter: Boolean by BooleanSetting("Clear After", false, description = "Clears the clicks when showing next, should work better with ss skip, but will be less consistent")

    private val triggerBotClock = Clock(delay)
    private val firstClickClock = Clock(800)

    private val firstButton = BlockPos(110, 121, 91)
    private val clickInOrder = ArrayList<BlockPos>()
    private var clickNeeded = 0
    private var currentPhase = 0
    private val phaseClock = Clock(500)

    private fun start() {
        if (mc.objectMouseOver?.blockPos == firstButton)
            repeat(startClicks) {
                runIn(it * startClickDelay) {
                    rightClick()
                }
            }
    }

    init {
        onMessage(Regex("\\[BOSS] Goldor: Who dares trespass into my domain\\?"), { start && enabled }) {
            start()
            modMessage("Starting Simon Says")
        }

        onWorldLoad {
            clickInOrder.clear()
            clickNeeded = 0
            currentPhase = 0
        }
    }

    override fun onKeybind() = start()

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        //if (DungeonUtils.getPhase() != 3) return
        val pos = event.pos
        val old = event.old
        val state = event.update

        if (pos == firstButton && state.block == Blocks.stone_button && state.getValue(BlockButtonStone.POWERED)) {
            clickInOrder.clear()
            clickNeeded = 0
            currentPhase = 0
            return
        }

        if (pos.y !in 120..123 || pos.z !in 92..95) return

        if (pos.x == 111 && state.block == Blocks.sea_lantern && pos !in clickInOrder) {
            clickInOrder.add(pos)
        } else if (pos.x == 110) {
            if (state.block == Blocks.air) {
                clickNeeded = 0
                if (phaseClock.hasTimePassed()) {
                    currentPhase++
                    phaseClock.update()
                }
                if (clearAfter) clickInOrder.clear()
            } else if (state.block == Blocks.stone_button) {
                if (old.block == Blocks.air && clickInOrder.size > currentPhase + 1) {
                    devMessage("was skipped!?!?!")
                }
                if (old.block == Blocks.stone_button && state.getValue(BlockButtonStone.POWERED)) {
                    val index = clickInOrder.indexOf(pos.add(1, 0, 0)) + 1
                    clickNeeded = if (index >= clickInOrder.size) 0 else index
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityJoin(event: PostEntityMetadata) {
        val ent = mc.theWorld.getEntityByID(event.packet.entityId)
        if (ent !is EntityItem || Item.getIdFromItem(ent.entityItem.item) != 77) return
        val pos = BlockPos(ent.posX.floor(), ent.posY.floor(), ent.posZ.floor()).east()
        val index = clickInOrder.indexOf(pos)
        if (index == 2 && clickInOrder.size == 3) {
            clickInOrder.removeFirst()
        } else if (index == 0 && clickInOrder.size == 2) {
            clickInOrder.reverse()
        }
    }

    private fun triggerBot() {
        if (!triggerBotClock.hasTimePassed(delay) || clickInOrder.size == 0 || mc.currentScreen != null) return
        val pos = mc.objectMouseOver?.blockPos ?: return
        if (clickInOrder[clickNeeded] != pos.east() && !(fullBlock && clickInOrder[clickNeeded] == pos)) return
        if (clickNeeded == 0) { // Stops spamming the first button and breaking the puzzle.
            if (!firstClickClock.hasTimePassed()) return
            rightClick()
            firstClickClock.update()
            return
        }
        rightClick()
        triggerBotClock.update()
    }

    @SubscribeEvent

    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (clickNeeded >= clickInOrder.size) return

        if (triggerBot) triggerBot()

        if (!solver) return
        GlStateManager.disableCull()

        for (index in clickNeeded until clickInOrder.size) {
            val pos = clickInOrder[index]
            val x = pos.x - .125
            val y = pos.y + .3125
            val z = pos.z + .25
            val color = when (index) {
                clickNeeded -> Color(0, 170, 0)
                clickNeeded + 1 -> Color(255, 170, 0)
                else -> Color(170, 0, 0)
            }.withAlpha(.5f)
            RenderUtils.drawFilledBox(AxisAlignedBB(x, y, z, x + .1875, y + .375, z + .5), color)
        }
        GlStateManager.enableCull()
    }
}