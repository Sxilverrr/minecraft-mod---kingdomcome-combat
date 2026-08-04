package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityCinematicVictimAnimationPayload(
        int entityId,
        int directionOrdinal,
        String animationName,
        float speedMultiplier,
        boolean holdLastFrame
) implements CustomPayload {
    public static final Id<EntityCinematicVictimAnimationPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_cinematic_victim_animation"));

    public static final PacketCodec<RegistryByteBuf, EntityCinematicVictimAnimationPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.entityId());
                        buf.writeInt(value.directionOrdinal());
                        buf.writeString(value.animationName());
                        buf.writeFloat(value.speedMultiplier());
                        buf.writeBoolean(value.holdLastFrame());
                    },
                    buf -> new EntityCinematicVictimAnimationPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readString(64),
                            buf.readFloat(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
