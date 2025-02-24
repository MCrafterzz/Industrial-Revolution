package me.steven.indrev.blockentities.solarpowerplant

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount
import me.steven.indrev.api.machines.Tier
import me.steven.indrev.blockentities.crafters.CraftingMachineBlockEntity
import me.steven.indrev.blockentities.crafters.SmelterBlockEntity
import me.steven.indrev.components.TemperatureComponent
import me.steven.indrev.components.FluidComponent
import me.steven.indrev.components.trackObject
import me.steven.indrev.inventories.inventory
import me.steven.indrev.items.upgrade.Enhancer
import me.steven.indrev.recipes.machines.DistillerRecipe
import me.steven.indrev.recipes.machines.IRRecipeType
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.bucket
import me.steven.indrev.utils.rawId
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

class DistillerBlockEntity(pos: BlockPos, state: BlockState) : CraftingMachineBlockEntity<DistillerRecipe>(Tier.MK4, MachineRegistry.DISTILLER_REGISTRY, pos, state) {

    override val enhancerSlots: IntArray = intArrayOf(3, 4, 5, 6)
    override val availableEnhancers: Array<Enhancer> = Enhancer.DEFAULT

    init {
        this.temperatureComponent = TemperatureComponent(this, 0.01, 70..120, 200)
        this.fluidComponent = FluidComponent({ this }, bucket)
        this.inventoryComponent = inventory(this) {
            output { slot = 2 }
        }
        trackObject(CRAFTING_COMPONENT_ID, craftingComponents[0])
        trackObject(TANK_ID, fluidComponent!![0])
    }

    override val type: IRRecipeType<DistillerRecipe> = DistillerRecipe.TYPE

    override fun getMaxCount(enhancer: Enhancer): Int {
        return if (enhancer == Enhancer.SPEED) return 2 else super.getMaxCount(enhancer)
    }

    companion object {
        const val CRAFTING_COMPONENT_ID = 4
        const val TANK_ID = 5
    }
}