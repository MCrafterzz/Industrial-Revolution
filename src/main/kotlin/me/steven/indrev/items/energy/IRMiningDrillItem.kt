package me.steven.indrev.items.energy

import me.steven.indrev.api.machines.Tier
import me.steven.indrev.utils.buildEnergyTooltip
import me.steven.indrev.utils.energyOf
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.ItemStack
import net.minecraft.item.PickaxeItem
import net.minecraft.item.ToolMaterial
import net.minecraft.text.Text
import net.minecraft.world.World

open class IRMiningDrillItem(
    toolMaterial: ToolMaterial,
    private val tier: Tier,
    private val maxStored: Double,
    val baseMiningSpeed: Float,
    settings: Settings
) : PickaxeItem(toolMaterial, 0, 0F, settings.maxDamage(-1)), IREnergyItem {

    override fun getMiningSpeedMultiplier(stack: ItemStack, state: BlockState?): Float {
        val material = state?.material
        val hasEnergy = (energyOf(stack)?.energy ?: 0.0) > 0
        return when {
            SUPPORTED_MATERIALS.contains(material) && hasEnergy -> baseMiningSpeed
            !hasEnergy -> 0F
            else -> super.getMiningSpeedMultiplier(stack, state)
        }
    }

    override fun getItemBarColor(stack: ItemStack?): Int = getDurabilityBarColor(stack)

    override fun isItemBarVisible(stack: ItemStack?): Boolean = hasDurabilityBar(stack)

    override fun getItemBarStep(stack: ItemStack?): Int = getDurabilityBarProgress(stack)

    override fun isEnchantable(stack: ItemStack?): Boolean = false

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>?,
        context: TooltipContext?
    ) {
        buildEnergyTooltip(stack, tooltip)
    }

    override fun canRepair(stack: ItemStack?, ingredient: ItemStack?): Boolean = false

    companion object {
        val SUPPORTED_MATERIALS = arrayOf(
            Material.METAL,
            Material.STONE,
            Material.WOOD,
            Material.BAMBOO,
            Material.COBWEB,
            Material.PISTON,
            Material.GOURD,
            Material.SOIL,
            Material.SOLID_ORGANIC,
            Material.LEAVES,
            Material.AGGREGATE
        )
    }
}