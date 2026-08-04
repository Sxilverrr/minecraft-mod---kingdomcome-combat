package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartExecutionPayload(int targetEntityId) implements CustomPayload {
    public static final Id<StartExecutionPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "start_execution"));

    public static final PacketCodec<RegistryByteBuf, StartExecutionPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeInt(value.targetEntityId()),
                    buf -> new StartExecutionPayload(buf.readInt())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
