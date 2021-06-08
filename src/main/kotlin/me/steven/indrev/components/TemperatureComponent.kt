package me.steven.indrev.components

import io.github.cottonmc.cotton.gui.PropertyDelegateHolder
import me.steven.indrev.api.machines.properties.Property
import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.items.misc.IRCoolerItem
import me.steven.indrev.registry.IRItemRegistry
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.PropertyDelegate

class TemperatureComponent(
    private val machineProvider: () -> MachineBlockEntity<*>,
    private val heatingSpeed: Double,
    val optimalRange: IntRange,
    explosionLimit: Double
) : PropertyDelegateHolder {

    var temperature: Double by Property(2, 12.0 + (getTemperatureModifier() * 10))
    var cooling = 0
    var coolingModifier = heatingSpeed
    var explosionPower = 2f
    var inputOverflow = false
    val explosionLimit: Double = explosionLimit
        get() {
            propertyDelegate[3] = field.toInt()
            return field
        }

    fun readNbt(tag: NbtCompound?) {
        temperature = tag?.getDouble("Temperature") ?: 0.0
        cooling = tag?.getInt("Cooling") ?: 0
    }

    fun writeNbt(tag: NbtCompound): NbtCompound {
        tag.putDouble("Temperature", temperature)
        tag.putInt("Cooling", cooling)
        return tag
    }

    fun isFullEfficiency() = (cooling <= 0 || getCoolerStack() != null) && temperature.toInt() in optimalRange

    fun tick(shouldHeatUp: Boolean) {
        val machine = machineProvider()
        val coolerStack = getCoolerStack()
        val coolerItem = coolerStack?.item
        val isHeatingUp = shouldHeatUp || (coolerItem == IRItemRegistry.HEAT_COIL && machine.use(16.0))
        val overflowModifier = 0//if (inputOverflow) 20 else 0
        if (!isHeatingUp && !inputOverflow && temperature > 30.5)
            temperature -= coolingModifier
        else if (cooling <= 0 && (temperature > optimalRange.last - 10)) {
            cooling = 70
            coolingModifier = heatingSpeed
            if (coolerStack != null && coolerItem is IRCoolerItem) {
                coolingModifier = coolerItem.coolingModifier
                coolerStack.damage++
                if (coolerStack.damage >= coolerStack.maxDamage) coolerStack.decrement(1)
            }
        } else if (cooling > 0 && temperature > 25) {
            cooling--
            temperature -= coolingModifier - overflowModifier
        } else
            temperature += heatingSpeed + overflowModifier
        if (temperature > explosionLimit - 5) {
            //machine.explode = true
        }
        inputOverflow = false
        //machine.markForUpdate { floor(previous) != floor(temperature) }
    }

    fun getCoolerStack(): ItemStack? = machineProvider().inventoryComponent?.inventory?.getStack(1)

    private fun getTemperatureModifier(): Float {
        val machine = machineProvider()
        return machine.world?.getBiome(machine.pos)?.getTemperature(machine.pos) ?: 0f
    }

    override fun getPropertyDelegate(): PropertyDelegate = machineProvider().propertyDelegate
}