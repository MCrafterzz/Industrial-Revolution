package me.steven.indrev.blockentities.drill

import me.steven.indrev.blocks.machine.DrillBlock
import me.steven.indrev.registry.IRItemRegistry
import me.steven.indrev.utils.identifier
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3f

class DrillBlockEntityRenderer : BlockEntityRenderer<DrillBlockEntity> {
    override fun render(
        entity: DrillBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack?,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val variant = when (entity.inventory[0].item) {
            IRItemRegistry.STONE_DRILL_HEAD -> "stone"
            IRItemRegistry.IRON_DRILL_HEAD -> "iron"
            IRItemRegistry.DIAMOND_DRILL_HEAD -> "diamond"
            IRItemRegistry.NETHERITE_DRILL_HEAD -> "netherite"
            else -> return
        }
        val model =
            MinecraftClient.getInstance().bakedModelManager.getModel(ModelIdentifier(identifier("drill_head"), variant))
        matrices?.run {
            push()
            val entry = peek()
            translate(0.5, 0.0, 0.5)
            if (entity.position <= 0.0 && entity.cachedState[DrillBlock.WORKING]) {
                multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion( (entity.world!!.time + tickDelta) * 12))
            }
            translate(-0.5, entity.position, -0.5)
            MinecraftClient.getInstance().blockRenderManager.modelRenderer.render(
                entry,
                vertexConsumers.getBuffer(RenderLayers.getBlockLayer(entity.cachedState)),
                null,
                model,
                -1f,
                -1f,
                -1f,
                WorldRenderer.getLightmapCoordinates(entity.world, entity.pos),
                OverlayTexture.DEFAULT_UV
            )
            pop()
        }
    }

    override fun rendersOutsideBoundingBox(blockEntity: DrillBlockEntity?): Boolean = true
}