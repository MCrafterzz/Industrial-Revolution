package me.steven.indrev.blockentities

import me.steven.indrev.IREnergyStorage
import me.steven.indrev.api.machines.Tier
import me.steven.indrev.api.machines.TransferMode
import me.steven.indrev.api.sideconfigs.ConfigurationType
import me.steven.indrev.api.sideconfigs.SideConfiguration
import me.steven.indrev.blockentities.crafters.EnhancerProvider
import me.steven.indrev.blocks.machine.MachineBlock
import me.steven.indrev.components.*
import me.steven.indrev.components.multiblock.MultiBlockComponent
import me.steven.indrev.config.IConfig
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.bucket
import me.steven.indrev.utils.transferEnergy
import me.steven.indrev.utils.transferFluids
import me.steven.indrev.utils.transferItems
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.inventory.SidedInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.WorldAccess
import team.reborn.energy.api.base.SimpleSidedEnergyContainer
import kotlin.collections.set

abstract class MachineBlockEntity<T : IConfig>(val tier: Tier, val registry: MachineRegistry, pos: BlockPos, state: BlockState)
    : BaseMachineBlockEntity(registry.blockEntityType(tier), pos, state) {

    val validConnections = mutableSetOf<Direction>().also { it.addAll(Direction.values()) }

    var guiSyncableComponent: GuiSyncableComponent? = GuiSyncableComponent()

    internal var energy: Long by autosync(ENERGY_ID, 0L) { value ->
        when (tier) {
            Tier.CREATIVE -> getCapacity()
            else -> value.coerceIn(0, getCapacity())
        }
    }

    open val maxInput: Long = tier.io
    open val maxOutput: Long = tier.io

    var inventoryComponent: InventoryComponent? = null
    var temperatureComponent: TemperatureComponent? = null
    var fluidComponent: FluidComponent? = null
    var multiblockComponent: MultiBlockComponent? = null

    var itemTransferCooldown = 0

    var workingState: Boolean = false
        set(value) {
            val update = value != field
            field = value
            if (update && world?.isClient == false)
                GlobalStateController.update(world!!, pos, value)
        }
        get() {
            if (world?.isClient == true && GlobalStateController.workingStateTracker.contains(pos.asLong())) {
                MinecraftClient.getInstance().execute { field = GlobalStateController.workingStateTracker.remove(pos.asLong()) }
            }
            return field
        }

    var ticks = 0

    @Suppress("UNCHECKED_CAST")
    val config: T by lazy { registry.config(tier) as T }

    init {
        trackLong(MAX_ENERGY_ID) { getCapacity() }
    }

    open val storage = object : IREnergyStorage() {

        override fun getAmount(): Long = energy

        override fun setAmount(v: Long) {
            energy = v
        }

        override fun getCapacity(): Long = this@MachineBlockEntity.getCapacity()

        override fun getMaxExtract(side: Direction?): Long = maxOutput

        override fun getMaxInsert(side: Direction?): Long = maxInput

        override fun onFinalCommit() {
            super.onFinalCommit()
            markDirty()
        }
    }


    protected open fun machineTick() {}

    @Environment(EnvType.CLIENT)
    open fun machineClientTick() {}

    fun tick() {
        if (this is EnhancerProvider && inventoryComponent != null)
            this.updateEnhancers(inventoryComponent!!.inventory)
        ticks++
        multiblockComponent?.tick(world!!, pos, cachedState)
        if (multiblockComponent?.isBuilt(world!!, pos, cachedState) == false) return

        transferEnergy()
        transferItems()
        transferFluids()

        machineTick()
        if (isMarkedForUpdate) {
            markDirty()
            if (this is BlockEntityClientSerializable) sync()
            isMarkedForUpdate = false
        }
    }

    open fun getCapacity(): Long = config.maxEnergyStored

    open fun getEnergyCost(): Long = 0

    // internal consumption
    fun use(amount: Long): Boolean {
        val extracted = amount.coerceAtMost(energy)
        if (extracted == amount) {
            this.energy -= extracted
            return true
        }
        return false
    }

    fun canUse(amount: Long): Boolean {
        val extracted = amount.coerceAtMost(energy)
        return extracted == amount
    }

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory? = inventoryComponent?.inventory

    override fun isConfigurable(type: ConfigurationType): Boolean {
        return when (type) {
            ConfigurationType.ITEM -> inventoryComponent != null
                    && (inventoryComponent?.inventory?.inputSlots?.isNotEmpty() == true
                    || inventoryComponent?.inventory?.outputSlots?.isNotEmpty() == true)
            ConfigurationType.FLUID -> fluidComponent != null
            ConfigurationType.ENERGY -> false
        }
    }

    override fun applyDefault(state: BlockState, type: ConfigurationType, configuration: MutableMap<Direction, TransferMode>) {
        val direction = (state.block as MachineBlock).getFacing(state)
        when (type) {
            ConfigurationType.ITEM -> {
                configuration[direction.rotateYClockwise()] = TransferMode.INPUT
                configuration[direction.rotateYCounterclockwise()] = TransferMode.OUTPUT
            }
            ConfigurationType.ENERGY -> throw IllegalArgumentException("cannot apply energy configuration to $this")
            else -> return
        }
    }

    override fun getValidConfigurations(type: ConfigurationType): Array<TransferMode> {
        return when (type) {
            ConfigurationType.ITEM -> TransferMode.DEFAULT
            ConfigurationType.FLUID, ConfigurationType.ENERGY -> arrayOf(TransferMode.INPUT, TransferMode.OUTPUT, TransferMode.NONE)
        }
    }

    override fun getCurrentConfiguration(type: ConfigurationType): SideConfiguration {
        return when (type) {
            ConfigurationType.ITEM -> inventoryComponent!!.itemConfig
            ConfigurationType.FLUID -> fluidComponent!!.transferConfig
            ConfigurationType.ENERGY -> error("nope")
        }
    }

    override fun isFixed(type: ConfigurationType): Boolean = false

    override fun <T> get(key: ComponentKey<T>): Any? {
        return when (key) {
            ComponentKey.FLUID -> fluidComponent
            ComponentKey.ITEM -> inventoryComponent
            ComponentKey.TEMPERATURE -> temperatureComponent
            ComponentKey.MULTIBLOCK -> multiblockComponent
            ComponentKey.GUI_SYNCABLE -> guiSyncableComponent
            ComponentKey.WORLD_OBJECT -> world
            else -> null
        }
    }

    open fun getFluidTransferRate(): Long = when (tier) {
        Tier.MK1 -> bucket / 3
        Tier.MK2 -> bucket * 2 / 3
        else -> bucket
    }

    override fun readNbt(tag: NbtCompound?) {
        super.readNbt(tag)
        inventoryComponent?.readNbt(tag)
        temperatureComponent?.readNbt(tag)
        fluidComponent?.fromTag(tag)
        multiblockComponent?.readNbt(tag)
        energy = tag?.getLong("Energy") ?: 0
    }

    override fun writeNbt(tag: NbtCompound?): NbtCompound {
        tag?.putLong("Energy", energy)
        if (tag != null) {
            inventoryComponent?.writeNbt(tag)
            temperatureComponent?.writeNbt(tag)
            fluidComponent?.toTag(tag)
            multiblockComponent?.writeNbt(tag)
        }
        return super.writeNbt(tag)
    }

    companion object {
        const val ENERGY_ID = 0
        const val MAX_ENERGY_ID = 1
        const val TEMPERATURE_ID = 2
        const val MAX_TEMPERATURE_ID = 3
    }
}