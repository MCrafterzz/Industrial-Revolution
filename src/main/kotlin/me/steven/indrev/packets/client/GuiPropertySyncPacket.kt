package me.steven.indrev.packets.client

import me.steven.indrev.components.SyncableObject
import me.steven.indrev.gui.screenhandlers.IRGuiScreenHandler
import me.steven.indrev.utils.identifier
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object GuiPropertySyncPacket {

    val SYNC_PROPERTY = identifier("sync_property") 

     fun register() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_PROPERTY) { client, _, buf, _ ->
            val syncId = buf.readInt()
            val property = buf.readInt()

            val handler = client.player!!.currentScreenHandler
            if (handler.syncId == syncId && handler is IRGuiScreenHandler) {
                handler.component?.properties?.get(property)?.fromPacket(buf)
            }
        }
    }
}