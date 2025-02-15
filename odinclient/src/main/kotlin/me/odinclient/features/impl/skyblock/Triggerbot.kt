package me.odinclient.features.impl.skyblock

import me.odinclient.utils.skyblock.PlayerUtils
import me.odinclient.utils.skyblock.PlayerUtils.leftClick
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.DualSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.utils.clock.Clock
import me.odinmain.utils.equalsOneOf
import me.odinmain.utils.isFacingAABB
import me.odinmain.utils.noControlCodes
import me.odinmain.utils.skyblock.LocationUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent


object Triggerbot : Module(
    name = "Triggerbot",
    description = "Various Triggerbots.",
    category = Category.DUNGEON
) {
    private val blood: Boolean by BooleanSetting("Blood Mobs")
    private val spiritBear: Boolean by BooleanSetting("Spirit Bear")
    private val bloodClickType: Boolean by DualSetting("Blood Click Type", "Left", "Right", description = "What button to click for blood mobs.").withDependency {
        blood
    }
    private val crystal: Boolean by BooleanSetting("Crystal", default = false)
    private val take: Boolean by BooleanSetting("Take", default = true).withDependency { crystal }
    private val place: Boolean by BooleanSetting("Place", default = true).withDependency { crystal }

    private val secretTriggerbot: Boolean by BooleanSetting("Secret Triggerbot", default = false)
    private val stbDelay: Long by NumberSetting("Delay", 200L, 70, 1000).withDependency { secretTriggerbot }
    private val stbCH: Boolean by BooleanSetting("Crystal Hollows Chests", true, description = "Opens chests in crystal hollows when looking at them").withDependency { secretTriggerbot }
    private val secretTBInBoss: Boolean by BooleanSetting("In Boss", true, description = "Makes the triggerbot work in dungeon boss aswell.").withDependency { secretTriggerbot }

    private val triggerBotClock = Clock(stbDelay)
    private var clickedPositions = mapOf<BlockPos, Long>()
    private val clickClock = Clock(500)
    private val bloodMobs = setOf(
        "Revoker", "Tear", "Ooze", "Cannibal", "Walker", "Putrid", "Mute", "Parasite", "WanderingSoul", "Leech",
        "Flamer", "Skull", "Mr.Dead", "Vader", "Frost", "Freak", "Bonzo", "Scarf", "Livid", "Psycho", "Reaper",
    )

    @SubscribeEvent
    fun onEntityJoin(event: EntityJoinWorldEvent) {
        if (event.entity !is EntityOtherPlayerMP || mc.currentScreen != null || !DungeonUtils.inDungeons) return
        val ent = event.entity
        val name = ent.name.replace(" ", "")
        if (!(bloodMobs.contains(name) && blood) && !(name == "Spirit Bear" && spiritBear)) return

        val (x, y, z) = Triple(ent.posX, ent.posY, ent.posZ)
        if (!isFacingAABB(AxisAlignedBB(x - .5, y - 2.0, z - .5, x + .5, y + 3.0, z + .5), 30f)) return

        if (bloodClickType && name != "Spirit Bear") PlayerUtils.rightClick()
        else leftClick()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!DungeonUtils.inBoss || DungeonUtils.getPhase() != 1 || !clickClock.hasTimePassed() || mc.objectMouseOver == null) return
        if (take && mc.objectMouseOver.entityHit is EntityEnderCrystal || (place && mc.objectMouseOver.entityHit?.name?.noControlCodes == "Energy Crystal Missing" && mc.thePlayer.heldItem.displayName.noControlCodes == "Energy Crystal")) {
            PlayerUtils.rightClick()
            clickClock.update()
        }
    }

    init {
        execute(0) {
            if (
                !enabled ||
                !triggerBotClock.hasTimePassed(stbDelay) ||
                DungeonUtils.currentRoomName.equalsOneOf("Water Board", "Three Weirdos") ||
                mc.currentScreen != null
            ) return@execute

            val pos = mc.objectMouseOver?.blockPos ?: return@execute
            val state = mc.theWorld.getBlockState(pos) ?: return@execute
            clickedPositions = clickedPositions.filter { it.value + 1000L > System.currentTimeMillis() }
            if (
                (pos.x in 58..62 && pos.y in 133..136 && pos.z == 142) || // looking at lights device
                clickedPositions.containsKey(pos) // already clicked
            ) return@execute

            if (stbCH && LocationUtils.currentArea == "Crystal Hollows" && state.block == Blocks.chest) {
                PlayerUtils.rightClick()
                triggerBotClock.update()
                clickedPositions = clickedPositions.plus(pos to System.currentTimeMillis())
                return@execute
            }

            if (!DungeonUtils.inDungeons || (!secretTBInBoss && DungeonUtils.inBoss) || !DungeonUtils.isSecret(state, pos)) return@execute

            PlayerUtils.rightClick()
            triggerBotClock.update()
            clickedPositions = clickedPositions.plus(pos to System.currentTimeMillis())
        }
    }
}