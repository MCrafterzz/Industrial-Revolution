package me.steven.indrev.compat.rei.categories

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount
import it.unimi.dsi.fastutil.ints.IntList
import me.shedaniel.math.Point
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.gui.DisplayRenderer
import me.shedaniel.rei.api.client.gui.Renderer
import me.shedaniel.rei.api.client.gui.SimpleDisplayRenderer
import me.shedaniel.rei.api.client.gui.widgets.Widget
import me.shedaniel.rei.api.client.gui.widgets.Widgets
import me.shedaniel.rei.api.client.registry.display.TransferDisplayCategory
import me.shedaniel.rei.api.common.category.CategoryIdentifier
import me.shedaniel.rei.api.common.entry.EntryStack
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes
import me.steven.indrev.compat.rei.plugins.IRMachinePlugin
import me.steven.indrev.recipes.machines.IRFluidRecipe
import me.steven.indrev.utils.createREIFluidWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier

open class IRMachineRecipeCategory(
    private val identifier: Identifier,
    private val logo: EntryStack<*>,
    private val categoryName: String
) : TransferDisplayCategory<IRMachinePlugin> {

    override fun setupDisplay(recipeDisplay: IRMachinePlugin, bounds: Rectangle): MutableList<Widget> {
        val recipe = recipeDisplay.recipe
        val startPoint = Point(bounds.centerX - 41, bounds.centerY - 27)
        val widgets = super.setupDisplay(recipeDisplay, bounds).toMutableList()
        widgets.add(Widgets.createArrow(Point(startPoint.x + 24, startPoint.y + 18)))
        if (recipe.input.isNotEmpty()) {
            val input = recipeDisplay.inputEntries.flatten().filter { it.type == VanillaEntryTypes.ITEM }
            widgets.add(Widgets.createSlot(Point(startPoint.x + 1, startPoint.y + 19)).entry(input[0]))
            if (recipe.input.size > 1)
                widgets.add(
                    Widgets.createSlot(Point(startPoint.x - 17, startPoint.y + 19)).entry(input[1])
                )
        }
        if (recipe.outputs.isNotEmpty()) {
            widgets.add(Widgets.createResultSlotBackground(Point(startPoint.x + 61, startPoint.y + 19)))
            widgets.add(
                Widgets.createSlot(Point(startPoint.x + 61, startPoint.y + 19)).entries(recipeDisplay.outputEntries.flatten().filter { it.type == VanillaEntryTypes.ITEM }).disableBackground().markOutput()
            )
        }

        if (recipe is IRFluidRecipe) {
            if (recipe.fluidOutput != null && recipe.fluidOutput?.amount() != FluidAmount.ZERO) {
                val outputFluidPoint = Point(startPoint.x + 83, startPoint.y)
                createREIFluidWidget(widgets, outputFluidPoint, recipe.fluidOutput!!)
            }
            if (recipe.fluidInput != null && recipe.fluidInput?.amount() != FluidAmount.ZERO) {
                val inputFluidPoint = Point(startPoint.x - 20, startPoint.y)
                createREIFluidWidget(widgets, inputFluidPoint, recipe.fluidInput!!)
            }
        }
        return widgets
    }

    override fun renderRedSlots(
        matrices: MatrixStack?,
        widgets: MutableList<Widget>?,
        bounds: Rectangle?,
        display: IRMachinePlugin?,
        redSlots: IntList?
    ) {
    }

    override fun getDisplayRenderer(display: IRMachinePlugin): DisplayRenderer {
        return SimpleDisplayRenderer.from(listOf(display.inputEntries[0]), display.outputEntries)
    }

    override fun getDisplayHeight(): Int = 66

    override fun getIdentifier(): Identifier = identifier

    override fun getIcon(): Renderer {
        return logo
    }

    override fun getTitle(): Text = TranslatableText(categoryName)

    override fun getCategoryIdentifier(): CategoryIdentifier<IRMachinePlugin> {
        return CategoryIdentifier.of(identifier)
    }
}