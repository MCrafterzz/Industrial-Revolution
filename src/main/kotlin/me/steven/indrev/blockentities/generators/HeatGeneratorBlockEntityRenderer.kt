package me.steven.indrev.blockentities.generators

import alexiil.mc.lib.attributes.fluid.render.FluidRenderFace
import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume
import me.steven.indrev.blocks.machine.HorizontalFacingMachineBlock
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3f

class HeatGeneratorBlockEntityRenderer : BlockEntityRenderer<HeatGeneratorBlockEntity> {
    override fun render(
        entity: HeatGeneratorBlockEntity?,
        tickDelta: Float,
        matrices: MatrixStack?,
        vertexConsumers: VertexConsumerProvider?,
        light: Int,
        overlay: Int
    ) {
        val volume = entity?.fluidComponent?.get(0) ?: return
        if (!volume.isEmpty) {
            matrices?.run {
                push()
                val direction = entity.cachedState[HorizontalFacingMachineBlock.HORIZONTAL_FACING]
                    .let { if (it.axis == Direction.Axis.X) it.opposite else it }
                translate(0.5, 0.5, 0.5)
                multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(direction.asRotation()))
                translate(-0.5, -0.5, -0.5)
                matrices.renderFluid(volume)
                FluidVolumeRenderer.VCPS.draw()
                pop()
            }
        }
    }

    private fun MatrixStack.renderFluid(inputVolume: FluidVolume) {
        val yMax = (((inputVolume.amount().asInexactDouble().toFloat() / 4f) * 10.0) / 16.0).coerceAtLeast(0.1)
        val face =
            listOf(
                FluidRenderFace.createFlatFaceZ(0.01, 0.1, 0.99, 0.99, yMax, 0.99, 2.0, true, false),
                FluidRenderFace.createFlatFaceY(0.01, yMax, 0.8, 0.99, yMax, 0.99, 2.0, true, false),
                FluidRenderFace.createFlatFaceX(0.01, 0.1, 0.8, 0.01, yMax, 0.99, 2.0, false, false),
                FluidRenderFace.createFlatFaceX(0.99, 0.1, 0.8, 0.99, yMax, 0.99, 2.0, true, false)
            )
        inputVolume.render(face, FluidVolumeRenderer.VCPS, this)
    }

}