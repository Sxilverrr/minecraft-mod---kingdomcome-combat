package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayerInterruptConfigSyncPayload(boolean enabled) implements CustomPayload {
    public static final Id<PlayerInterruptConfigSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "player_interrupt_config_sync"));

    public static final PacketCodec<RegistryByteBuf, PlayerInterruptConfigSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeBoolean(value.enabled()),
                    buf -> new PlayerInterruptConfigSyncPayload(buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
