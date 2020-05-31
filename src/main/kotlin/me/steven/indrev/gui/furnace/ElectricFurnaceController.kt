package me.steven.indrev.gui.furnace

import io.github.cottonmc.cotton.gui.CottonCraftingController
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import me.steven.indrev.blockentities.TemperatureController
import me.steven.indrev.blockentities.crafters.UpgradeProvider
import me.steven.indrev.gui.widgets.EnergyWidget
import me.steven.indrev.gui.widgets.ProcessWidget
import me.steven.indrev.gui.widgets.StringWidget
import me.steven.indrev.gui.widgets.TemperatureWidget
import net.minecraft.client.resource.language.I18n
import net.minecraft.container.BlockContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.recipe.RecipeType

class ElectricFurnaceController(syncId: Int, playerInventory: PlayerInventory, blockContext: BlockContext)
    : CottonCraftingController(RecipeType.SMELTING, syncId, playerInventory, getBlockInventory(blockContext), getBlockPropertyDelegate(blockContext)) {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        root.setSize(150, 120)

        root.add(StringWidget(I18n.translate("block.indrev.electric_furnace"), titleColor), 4, 0)

        root.add(createPlayerInventoryPanel(), 0, 5)

        root.add(EnergyWidget(propertyDelegate), 0, 0, 16, 64)
        val batterySlot = WItemSlot.of(blockInventory, 0)
        root.add(batterySlot, 0, 4)
        batterySlot.setLocation(0, (3.7 * 18).toInt())

        val inputSlot = WItemSlot.of(blockInventory, 2)
        root.add(inputSlot, 3, 2)
        inputSlot.setLocation((2.3 * 18).toInt(), (1.5 * 18).toInt())

        val processWidget = ProcessWidget(propertyDelegate)
        root.add(processWidget, 4, 2)
        processWidget.setLocation((3.5 * 18).toInt(), (1.5 * 18).toInt())

        val outputSlot = WItemSlot.outputOf(blockInventory, 3)
        outputSlot.isInsertingAllowed = false
        root.add(outputSlot, 6, 2)
        outputSlot.setLocation((5.5 * 18).toInt(), (1.5 * 18).toInt())

        blockContext.run { world, pos ->
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is UpgradeProvider) {
                for ((i, slot) in blockEntity.getUpgradeSlots().withIndex()) {
                    val s = WItemSlot.of(blockInventory, slot)
                    root.add(s, 8, i)
                }
            }
            if (blockEntity is TemperatureController) {
                root.add(TemperatureWidget(propertyDelegate, blockEntity), 1, 0, 16, 64)
                val coolerSlot = WItemSlot.of(blockInventory, 1)
                root.add(coolerSlot, 1, 4)
                coolerSlot.setLocation(1 * 18, (3.7 * 18).toInt())
            }
        }

        root.validate(this)
    }
}