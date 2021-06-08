package me.steven.indrev.gui.screenhandlers.machines

import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.gui.PatchouliEntryShortcut
import me.steven.indrev.gui.screenhandlers.IRGuiScreenHandler
import me.steven.indrev.gui.widgets.machines.WEnergy
import me.steven.indrev.gui.widgets.misc.WPlayerRender
import me.steven.indrev.gui.widgets.misc.WStaticTooltip
import me.steven.indrev.gui.widgets.misc.WText
import me.steven.indrev.utils.add
import me.steven.indrev.utils.addBookEntryShortcut
import me.steven.indrev.utils.getEnergySlotPainter
import me.steven.indrev.utils.identifier
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ArmorItem
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import java.util.function.Predicate

class LazuliFluxContainerScreenHandler(syncId: Int, playerInventory: PlayerInventory, ctx: ScreenHandlerContext) :
    IRGuiScreenHandler(
        IndustrialRevolution.BATTERY_HANDLER,
        syncId,
        playerInventory,
        ctx
    ), PatchouliEntryShortcut {

    val shieldPainter = getSlotPainter(40, Identifier("minecraft", "textures/item/empty_armor_slot_shield.png"))
    val helmetPainter = getSlotPainter(39, Identifier("minecraft", "textures/item/empty_armor_slot_helmet.png"))
    val chestplatePainter = getSlotPainter(38, Identifier("minecraft", "textures/item/empty_armor_slot_chestplate.png"))
    val leggingsPainter = getSlotPainter(37, Identifier("minecraft", "textures/item/empty_armor_slot_leggings.png"))
    val bootsPainter = getSlotPainter(36, Identifier("minecraft", "textures/item/empty_armor_slot_boots.png"))

    init {
        val root = WGridPanel()
        setRootPanel(root)
        root.setSize(150, 120)

        root.add(
            WText(TranslatableText("block.indrev.lazuli_flux_container_1"), HorizontalAlignment.CENTER, 0x404040),
            5.95,
            0.0
        )
        root.add(
            WText(TranslatableText("block.indrev.lazuli_flux_container_2"), HorizontalAlignment.CENTER, 0x404040),
            5.95,
            0.7
        )

        val wEnergy = WEnergy()
        root.add(wEnergy, 8.0, 0.5)

        ctx.run { world, _ ->
            val itemSlot = WItemSlot.of(blockInventory, 0)
            if (world.isClient)
                itemSlot.backgroundPainter = getEnergySlotPainter(blockInventory, 0)
            root.add(itemSlot, 5.4, 1.3)

            root.add(createPlayerInventoryPanel(), 0.0, 4.2)

            val boots = WItemSlot.of(playerInventory, 36)
            if (world.isClient)
                boots.backgroundPainter = bootsPainter
            boots.filter = Predicate { stack ->
                val item = stack.item
                item is ArmorItem && item.slotType == EquipmentSlot.FEET
            }
            root.add(boots, 0, 3)

            val leggings = WItemSlot.of(playerInventory, 37)
            if (world.isClient)
                leggings.backgroundPainter = leggingsPainter
            leggings.filter = Predicate { stack ->
                val item = stack.item
                item is ArmorItem && item.slotType == EquipmentSlot.LEGS
            }
            root.add(leggings, 0, 2)

            val chestplate = WItemSlot.of(playerInventory, 38)
            if (world.isClient)
                chestplate.backgroundPainter = chestplatePainter
            chestplate.filter = Predicate { stack ->
                val item = stack.item
                item is ArmorItem && item.slotType == EquipmentSlot.CHEST
            }
            root.add(chestplate, 0, 1)

            val helmet = WItemSlot.of(playerInventory, 39)
            if (world.isClient)
                helmet.backgroundPainter = helmetPainter
            helmet.filter = Predicate { stack ->
                val item = stack.item
                item is ArmorItem && item.slotType == EquipmentSlot.HEAD
            }
            root.add(helmet, 0, 0)

            val shield = WItemSlot.of(playerInventory, 40)
            if (world.isClient)
                shield.backgroundPainter = shieldPainter
            root.add(shield, 3.8, 3.0)
        }

        val playerBg = WStaticTooltip()
        root.add(playerBg, 1.3, 0.2)
        playerBg.setSize(40, 65)

        val playerWidget = WPlayerRender()
        root.add(playerWidget, 2.4, 3.5)

        addBookEntryShortcut(playerInventory, root, -1.8, -0.47)

        root.validate(this)
    }

    override fun canUse(player: PlayerEntity?): Boolean = true

    override fun getEntry(): Identifier = identifier("machines/batteries")

    override fun getPage(): Int = 0

    private fun getSlotPainter(slot: Int, identifier: Identifier) = BackgroundPainter { matrices, left, top, panel ->
        BackgroundPainter.SLOT.paintBackground(matrices, left, top, panel)
        if (playerInventory.getStack(slot).isEmpty)
            ScreenDrawing.texturedRect(matrices, left + 1, top + 1, 16, 16, identifier, -1)
    }

    companion object {
        val SCREEN_ID = identifier("battery_screen")
    }
}