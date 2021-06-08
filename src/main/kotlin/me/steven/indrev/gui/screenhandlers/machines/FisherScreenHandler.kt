package me.steven.indrev.gui.screenhandlers.machines

import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.gui.PatchouliEntryShortcut
import me.steven.indrev.gui.screenhandlers.IRGuiScreenHandler
import me.steven.indrev.gui.widgets.misc.WTooltipedItemSlot
import me.steven.indrev.utils.add
import me.steven.indrev.utils.configure
import me.steven.indrev.utils.identifier
import me.steven.indrev.utils.setIcon
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier

class FisherScreenHandler(syncId: Int, playerInventory: PlayerInventory, ctx: ScreenHandlerContext) :
    IRGuiScreenHandler(
        IndustrialRevolution.FISHER_HANDLER,
        syncId,
        playerInventory,
        ctx
    ), PatchouliEntryShortcut {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        configure("block.indrev.fisher", ctx, playerInventory, blockInventory)

        root.add(WItemSlot.of(blockInventory, 2, 2, 2), 3.95, 0.7)

        val fishingRodSlot = WTooltipedItemSlot.of(blockInventory, 1, TranslatableText("gui.indrev.fishingrod"))
        fishingRodSlot.setIcon(ctx, blockInventory, 1, FISHING_ROD_ICON)
        root.add(fishingRodSlot, 4.45, 3.5)

        root.validate(this)
    }

    override fun getEntry(): Identifier = identifier("machines/fisher")

    override fun getPage(): Int = 0

    companion object {
        val SCREEN_ID = identifier("fishing_farm_screen")
        val FISHING_ROD_ICON = identifier("textures/gui/fishing_rod_icon.png")
    }
}