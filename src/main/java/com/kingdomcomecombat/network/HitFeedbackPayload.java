package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HitFeedbackPayload(
        int directionOrdinal,
        boolean penetratedArmor
) implements CustomPayload {
    public static final Id<HitFeedbackPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "hit_feedback"));

    public static final PacketCodec<RegistryByteBuf, HitFeedbackPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.directionOrdinal());
                        buf.writeBoolean(value.penetratedArmor());
                    },
                    buf -> new HitFeedbackPayload(
                            buf.readInt(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
