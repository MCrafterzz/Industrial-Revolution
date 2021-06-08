package me.steven.indrev.blocks.machine.pipes

import me.steven.indrev.api.machines.Tier
import me.steven.indrev.config.IRConfig
import me.steven.indrev.networks.Network
import me.steven.indrev.utils.energyOf
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView

class CableBlock(settings: Settings, tier: Tier) : BasePipeBlock(settings, tier, Network.Type.ENERGY) {
    override fun appendTooltip(
        stack: ItemStack?,
        world: BlockView?,
        tooltip: MutableList<Text>?,
        options: TooltipContext?
    ) {
        tooltip?.add(
            TranslatableText("gui.indrev.tooltip.maxTransferRate").formatted(Formatting.AQUA)
                .append(TranslatableText("gui.indrev.tooltip.lftick", getMaxTransferRate()).formatted(Formatting.GRAY))
        )
    }

    override fun getOutlineShape(
        state: BlockState,
        view: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape {
        return if (state[COVERED]) VoxelShapes.fullCube()
        else getShape(state)
    }

    override fun isConnectable(world: ServerWorld, pos: BlockPos, dir: Direction) =
        energyOf(world, pos, dir.opposite) != null || world.getBlockState(pos).block.let { it is CableBlock && it.tier == tier }

    private fun getMaxTransferRate() = when(tier) {
        Tier.MK1 -> IRConfig.cables.cableMk1
        Tier.MK2 -> IRConfig.cables.cableMk2
        Tier.MK3 -> IRConfig.cables.cableMk3
        else -> IRConfig.cables.cableMk4
    }

    override fun getShape(blockState: BlockState): VoxelShape {
        val directions = Direction.values().filter { dir -> blockState[getProperty(dir)] }.toTypedArray()
        var cableShapeCache = SHAPE_CACHE.firstOrNull { shape -> shape.directions.contentEquals(directions) }
        if (cableShapeCache == null) {
            var shape = CENTER_SHAPE
            Direction.values().forEach { direction ->
                if (blockState[getProperty(direction)]) shape = VoxelShapes.union(shape, getShape(direction))
            }
            cableShapeCache = PipeShape(directions, shape)
            SHAPE_CACHE.add(cableShapeCache)
        }
        return cableShapeCache.shape
    }

    companion object {

        val SHAPE_CACHE = hashSetOf<PipeShape>()

        val DOWN_SHAPE: VoxelShape = createCuboidShape(6.0, 0.0, 6.0, 10.0, 6.0, 10.0)
        val UP_SHAPE: VoxelShape = createCuboidShape(6.0, 10.5, 6.0, 10.0, 16.0, 10.0)
        val SOUTH_SHAPE: VoxelShape = createCuboidShape(6.0, 6.0, 10.5, 10.0, 10.0, 16.0)
        val NORTH_SHAPE: VoxelShape = createCuboidShape(6.0, 6.0, 0.0, 10.0, 10.0, 5.5)
        val EAST_SHAPE: VoxelShape = createCuboidShape(10.5, 6.0, 6.0, 16.0, 10.0, 10.0)
        val WEST_SHAPE: VoxelShape = createCuboidShape(0.0, 6.0, 6.0, 5.5, 10.0, 10.0)

        val CENTER_SHAPE: VoxelShape = createCuboidShape(5.5, 5.5, 5.5, 10.5, 10.5, 10.5)

        private fun getShape(direction: Direction): VoxelShape {
            var shape = VoxelShapes.empty()
            if (direction == Direction.NORTH) shape = NORTH_SHAPE
            if (direction == Direction.SOUTH) shape = SOUTH_SHAPE
            if (direction == Direction.EAST) shape = EAST_SHAPE
            if (direction == Direction.WEST) shape = WEST_SHAPE
            if (direction == Direction.UP) shape = UP_SHAPE
            if (direction == Direction.DOWN) shape = DOWN_SHAPE
            return shape
        }

    }
}