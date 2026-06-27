package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HardcoreModeSyncPayload(boolean enabled) implements CustomPayload {
    public static final Id<HardcoreModeSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "hardcore_mode_sync"));

    public static final PacketCodec<RegistryByteBuf, HardcoreModeSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeBoolean(value.enabled()),
                    buf -> new HardcoreModeSyncPayload(buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
