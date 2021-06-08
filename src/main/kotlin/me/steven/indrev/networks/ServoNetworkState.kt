package me.steven.indrev.networks

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.steven.indrev.IndustrialRevolution
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.*
import java.util.function.LongFunction

abstract class ServoNetworkState<T : Network>(type: Network.Type<T>, world: ServerWorld) : NetworkState<T>(type, world) {
    val endpointData = Long2ObjectOpenHashMap<Object2ObjectOpenHashMap<Direction, EndpointData>>()
    private val recentlyRemoved = Long2ObjectOpenHashMap<Object2ObjectOpenHashMap<Direction, EndpointData>>()

    private val syncedMaps = Object2IntOpenHashMap<UUID>()

    init {
        syncedMaps.defaultReturnValue(-1)
    }

    var version = 0

    fun sync(world: ServerWorld) {
        world.players.forEach { player ->
            val v = syncedMaps.getInt(player.uuid)
            if (version > v) {
                val buf = PacketByteBufs.create()
                write(buf)
                ServerPlayNetworking.send(player, IndustrialRevolution.SYNC_NETWORK_SERVOS, buf)
                syncedMaps[player.uuid] = version
            }
        }
    }

    private fun write(buf: PacketByteBuf) {
        buf.writeString(type.key)
        buf.writeInt(endpointData.size)
        endpointData.forEach { (pos, info) ->
            buf.writeLong(pos)
            buf.writeByte(info.size)
            info.forEach { (dir, data) ->
                buf.writeByte(dir.ordinal)
                buf.writeByte(data.type.ordinal)
                buf.writeBoolean(data.mode != null)
                if (data.mode != null) buf.writeByte(data.mode!!.ordinal)
            }
        }
    }

    override fun remove(pos: BlockPos) {
        version++
        super.remove(pos)
        if (endpointData.containsKey(pos.asLong()))
            recentlyRemoved[pos.asLong()] = endpointData.remove(pos.asLong())
    }

    override fun set(blockPos: BlockPos, network: T) {
        version++
        super.set(blockPos, network)
        if (recentlyRemoved.containsKey(blockPos.asLong())) {
            endpointData[blockPos.asLong()] = recentlyRemoved.remove(blockPos.asLong())
        }
    }

    fun hasServo(blockPos: BlockPos, direction: Direction): Boolean {
        return endpointData.get(blockPos.asLong())?.get(direction)?.type?.let { it != EndpointData.Type.INPUT } == true
    }

    fun getEndpointData(pos: BlockPos, direction: Direction, createIfAbsent: Boolean = false): EndpointData? {
        return if (createIfAbsent)
            endpointData.computeIfAbsent(pos.asLong(), LongFunction { Object2ObjectOpenHashMap() })
                .computeIfAbsent(direction) { EndpointData(EndpointData.Type.INPUT, null) }
        else
            endpointData.get(pos.asLong())?.get(direction)
    }

    fun removeEndpointData(pos: BlockPos, direction: Direction): EndpointData? {
        version++
        val datas = endpointData.get(pos.asLong()) ?: return null
        val d = datas.remove(direction)
        if (datas.isEmpty()) endpointData.remove(pos.asLong())
        return d
    }

    open fun clearCachedData(importCache: Boolean) {
        if (importCache) {
            recentlyRemoved.forEach { e -> endpointData[e.key] = e.value }
        }
        this.recentlyRemoved.clear()
    }

    override fun writeNbt(tag: NbtCompound): NbtCompound {
        val modesTag = NbtList()
        endpointData.forEach { (pos, modes) ->
            val sidesTag = NbtList()
            modes.forEach { (dir, mode) ->
                val t = NbtCompound()
                t.put(dir.ordinal.toString(), mode.writeNbt(NbtCompound()))
                sidesTag.add(t)
            }
            val posTag = NbtCompound()
            posTag.putLong("pos", pos)
            posTag.put("sides", sidesTag)
            modesTag.add(posTag)

        }
        tag.put("modes", modesTag)
        return super.writeNbt(tag)
    }

    companion object {
        fun <T : Network, P : ServoNetworkState<T>> readNbt(tag: NbtCompound, supplier: () -> P): P {
            val state = supplier()
            val modesTag = tag.getList("modes", 10)
            modesTag.forEach { posTag ->
                posTag as NbtCompound
                val pos = posTag.getLong("pos")
                val map = Object2ObjectOpenHashMap<Direction, EndpointData>()
                val sidesTag = posTag.getList("sides", 10)
                sidesTag.forEach { t ->
                    t as NbtCompound
                    t.keys.forEach { id ->
                        val data = EndpointData(EndpointData.Type.INPUT, EndpointData.Mode.NEAREST_FIRST).readNbt(t.getCompound(id))
                        val dir = Direction.values()[id.toInt()]
                        map[dir] = data
                    }
                }

                state.endpointData[pos] = map
            }

            NetworkState.readNbt(tag, supplier)

            return state
        }
    }
}