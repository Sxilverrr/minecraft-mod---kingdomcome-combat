package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ScaledHitFeedbackPayload(
        int directionOrdinal,
        boolean penetratedArmor,
        float feedbackScale
) implements CustomPayload {
    public static final Id<ScaledHitFeedbackPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "scaled_hit_feedback"));

    public static final PacketCodec<RegistryByteBuf, ScaledHitFeedbackPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.directionOrdinal());
                        buf.writeBoolean(value.penetratedArmor());
                        buf.writeFloat(value.feedbackScale());
                    },
                    buf -> new ScaledHitFeedbackPayload(
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readFloat()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
