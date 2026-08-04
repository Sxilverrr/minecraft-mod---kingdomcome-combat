package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdatePlayerInterruptConfigPayload(boolean enabled) implements CustomPayload {
    public static final Id<UpdatePlayerInterruptConfigPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "update_player_interrupt_config"));

    public static final PacketCodec<RegistryByteBuf, UpdatePlayerInterruptConfigPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeBoolean(value.enabled()),
                    buf -> new UpdatePlayerInterruptConfigPayload(buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
