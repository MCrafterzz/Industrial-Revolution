package me.steven.indrev.networks.energy

import dev.technici4n.fasttransferlib.api.Simulation
import dev.technici4n.fasttransferlib.api.energy.EnergyIo
import dev.technici4n.fasttransferlib.api.energy.EnergyMovement
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import me.steven.indrev.api.machines.Tier
import me.steven.indrev.blocks.machine.pipes.CableBlock
import me.steven.indrev.config.IRConfig
import me.steven.indrev.networks.Network
import me.steven.indrev.networks.NetworkState
import me.steven.indrev.utils.energyOf
import me.steven.indrev.utils.isLoaded
import net.minecraft.block.Block
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.*
import kotlin.math.absoluteValue

open class EnergyNetwork(
    world: ServerWorld,
    val cables: MutableSet<BlockPos> = hashSetOf(),
    val machines: MutableMap<BlockPos, EnumSet<Direction>> = hashMapOf()
) : Network(Type.ENERGY, world, cables, machines) {

    var tier = Tier.MK1
    private val maxCableTransfer: Double
        get() = when (tier) {
            Tier.MK1 -> IRConfig.cables.cableMk1
            Tier.MK2 -> IRConfig.cables.cableMk2
            Tier.MK3 -> IRConfig.cables.cableMk3
            else -> IRConfig.cables.cableMk4
        }

    override fun tick(world: ServerWorld) {
        if (machines.isEmpty()) return
        else if (queue.isEmpty()) {
            buildQueue()
        }
        if (queue.isNotEmpty()) {
            val remainingInputs = Object2DoubleOpenHashMap<BlockPos>()
            machines.forEach { (pos, directions) ->
                val q = PriorityQueue(queue[pos] ?: return@forEach)
                if (!world.isLoaded(pos)) return@forEach
                directions.forEach inner@{ dir ->
                    val energyIo = energyOf(world, pos, dir.opposite) ?: return@inner
                    var remaining = energyIo.maxOutput

                    while (q.isNotEmpty() && energyIo.supportsExtraction() && remaining > 1e-9) {
                        val (_, targetPos, _, targetDir) = q.poll()
                        if (!world.isLoaded(targetPos)) continue
                        val target = energyOf(world, targetPos, targetDir.opposite) ?: continue
                        if (!target.supportsInsertion()) continue
                        val maxInput = remainingInputs.computeIfAbsent(targetPos) { target.maxInput }
                        if (maxInput < 1e-9) continue

                        val amount = remaining.coerceAtMost(maxInput).coerceAtMost(maxCableTransfer)
                        val before = target.energy
                        val moved = EnergyMovement.move(energyIo, target, amount)
                        val after = target.energy
                        if (moved > 1e-9 && (after - before).absoluteValue > 1e-9) {
                            remaining -= moved
                            remainingInputs.addTo(targetPos, -moved)
                        }
                    }
                }
            }
        }
    }

    override fun <K : Network> appendPipe(state: NetworkState<K>, block: Block, blockPos: BlockPos) {
        val cable = block as? CableBlock ?: return
        this.tier = cable.tier
        super.appendPipe(state, block, blockPos)
    }

    override fun writeNbt(tag: NbtCompound): NbtCompound {
        super.writeNbt(tag)
        tag.putInt("tier", tier.ordinal)
        return tag
    }

    override fun readNbt(world: ServerWorld, tag: NbtCompound) {
        super.readNbt(world, tag)
        val tier = Tier.values()[tag.getInt("tier")]
        this.tier = tier
    }

    companion object {

        private const val MAX_VALUE = (Integer.MAX_VALUE - 1).toDouble()

        private val EnergyIo.maxInput: Double
            get() = MAX_VALUE - insert(MAX_VALUE, Simulation.SIMULATE)
        private val EnergyIo.maxOutput: Double
            get() = extract(MAX_VALUE, Simulation.SIMULATE)

        fun readNbt(world: ServerWorld, tag: NbtCompound): EnergyNetwork {
            val network = Network.readNbt(world, tag) as EnergyNetwork
            val tier = Tier.values()[tag.getInt("tier")]
            network.tier = tier
            return network
        }
    }
}