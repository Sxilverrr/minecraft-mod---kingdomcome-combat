package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityAttackAnimationPayload(
        int entityId,
        int directionOrdinal,
        float speedMultiplier,
        float startupSlowdown,
        String animationName
) implements CustomPayload {
    public static final Id<EntityAttackAnimationPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_attack_animation"));

    public static final PacketCodec<RegistryByteBuf, EntityAttackAnimationPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.entityId());
                        buf.writeInt(value.directionOrdinal());
                        buf.writeFloat(value.speedMultiplier());
                        buf.writeFloat(value.startupSlowdown());
                        buf.writeString(value.animationName());
                    },
                    buf -> new EntityAttackAnimationPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readString()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
