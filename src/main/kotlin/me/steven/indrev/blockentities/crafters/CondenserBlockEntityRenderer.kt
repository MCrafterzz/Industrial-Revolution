package me.steven.indrev.blockentities.crafters

import alexiil.mc.lib.attributes.fluid.render.FluidRenderFace
import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer
import me.steven.indrev.blocks.machine.HorizontalFacingMachineBlock
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Direction

class CondenserBlockEntityRenderer : BlockEntityRenderer<CondenserBlockEntity> {

    override fun render(
        entity: CondenserBlockEntity?,
        tickDelta: Float,
        matrices: MatrixStack?,
        vertexConsumers: VertexConsumerProvider?,
        light: Int,
        overlay: Int
    ) {
        val fluidComponent = entity?.fluidComponent ?: return
        val faces = when (entity.cachedState[HorizontalFacingMachineBlock.HORIZONTAL_FACING]) {
            Direction.NORTH -> NORTH_FACE
            Direction.SOUTH -> SOUTH_FACE
            Direction.WEST -> WEST_FACE
            Direction.EAST -> EAST_FACE
            else -> return
        }
        val volume = fluidComponent[0]
        if (!volume.isEmpty) {
            volume.render(faces, FluidVolumeRenderer.VCPS, matrices)
            FluidVolumeRenderer.VCPS.draw()
        }
    }

    companion object {
        private val NORTH_FACE =
            listOf(FluidRenderFace.createFlatFaceZ(0.815, 0.625, -0.005, 0.19, 0.815, -0.005, 1.0, true, false))
        private val SOUTH_FACE =
            listOf(FluidRenderFace.createFlatFaceZ(0.185, 0.625, 1.005, 0.81, 0.815, 1.005, 1.0, true, false))
        private val WEST_FACE =
            listOf(FluidRenderFace.createFlatFaceX(-0.005, 0.625, 0.185, -0.005, 0.815, 0.81, 1.0, false, false))
        private val EAST_FACE =
            listOf(FluidRenderFace.createFlatFaceX(1.005, 0.625, 0.815, 1.005, 0.815, 0.19, 1.0, false, false))
    }
}