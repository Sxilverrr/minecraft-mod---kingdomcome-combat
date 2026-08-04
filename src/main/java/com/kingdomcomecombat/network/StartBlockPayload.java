package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartBlockPayload(int directionOrdinal, boolean holding) implements CustomPayload {
    public static final Id<StartBlockPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "start_block"));

    public static final PacketCodec<RegistryByteBuf, StartBlockPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.directionOrdinal());
                        buf.writeBoolean(value.holding());
                    },
                    buf -> new StartBlockPayload(buf.readInt(), buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
