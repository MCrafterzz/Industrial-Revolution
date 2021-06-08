package me.steven.indrev.blockentities.generators

import alexiil.mc.lib.attributes.Simulation
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount
import me.steven.indrev.api.machines.Tier
import me.steven.indrev.api.machines.properties.Property
import me.steven.indrev.components.TemperatureComponent
import me.steven.indrev.components.fluid.FluidComponent
import me.steven.indrev.inventories.inventory
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.MB
import net.minecraft.block.BlockState
import net.minecraft.fluid.Fluids
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.util.math.BlockPos

class HeatGeneratorBlockEntity(tier: Tier, pos: BlockPos, state: BlockState)
    : GeneratorBlockEntity(tier, MachineRegistry.HEAT_GENERATOR_REGISTRY, pos, state) {
    init {
        this.propertyDelegate = ArrayPropertyDelegate(7)
        this.temperatureComponent = TemperatureComponent({ this }, 0.8, 7000..9000, 10000.0)
        this.inventoryComponent = inventory(this) {
            input { slot = 2 }
        }
        this.fluidComponent = FluidComponent({ this }, FluidAmount.ofWhole(4))

    }
    private var burnTime: Int by Property(4, 0)
    private var maxBurnTime: Int by Property(5, 0)

    override fun shouldGenerate(): Boolean {
        if (burnTime > 0) burnTime--
        else if (energyCapacity > energy) {
            val fluidComponent = fluidComponent!!
            val volume = fluidComponent[0]
            val extractable = fluidComponent.extractable
            fluidComponent.extractable
            val consume = getConsumptionRate()
            if (volume.rawFluid == Fluids.LAVA
                && extractable.attemptAnyExtraction(consume, Simulation.SIMULATE).amount() == consume) {
                burnTime = 10
                maxBurnTime = burnTime
                extractable.extract(consume)
            }
            markDirty()
        }
        return burnTime > 0 && energy < energyCapacity
    }

    override fun getGenerationRatio(): Double {
        val ratio = config.ratio * (temperatureComponent!!.temperature / temperatureComponent!!.optimalRange.first).coerceAtMost(1.0)
        propertyDelegate[6] = ratio.toInt()
        return ratio
    }

    fun getConsumptionRate(temperature: Double = temperatureComponent!!.temperature): FluidAmount {
        val r = ((temperature / temperatureComponent!!.optimalRange.first).coerceIn(0.001, 1.0) * 500).toLong()
        return MB.mul(r)
    }

    override fun readNbt(tag: NbtCompound?) {
        super.readNbt(tag)
        burnTime = tag?.getInt("BurnTime") ?: 0
        maxBurnTime = tag?.getInt("MaxBurnTime") ?: 0
    }

    override fun writeNbt(tag: NbtCompound?): NbtCompound {
        tag?.putInt("BurnTime", burnTime)
        tag?.putInt("MaxBurnTime", maxBurnTime)
        return super.writeNbt(tag)
    }

    override fun fromClientTag(tag: NbtCompound?) {
        super.fromClientTag(tag)
        burnTime = tag?.getInt("BurnTime") ?: 0
        maxBurnTime = tag?.getInt("MaxBurnTime") ?: 0
    }

    override fun toClientTag(tag: NbtCompound?): NbtCompound {
        tag?.putInt("BurnTime", burnTime)
        tag?.putInt("MaxBurnTime", maxBurnTime)
        return super.toClientTag(tag)
    }
}