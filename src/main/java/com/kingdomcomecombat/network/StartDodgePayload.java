package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartDodgePayload(int directionOrdinal) implements CustomPayload {
    public static final Id<StartDodgePayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "start_dodge"));

    public static final PacketCodec<RegistryByteBuf, StartDodgePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeInt(value.directionOrdinal()),
                    buf -> new StartDodgePayload(buf.readInt())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
