package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityComboAttackAnimationPayload(
        int entityId,
        int directionOrdinal,
        String animationName,
        float speedMultiplier,
        boolean bladeTrail
) implements CustomPayload {
    public static final Id<EntityComboAttackAnimationPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_combo_attack_animation"));

    public static final PacketCodec<RegistryByteBuf, EntityComboAttackAnimationPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.entityId());
                        buf.writeInt(value.directionOrdinal());
                        buf.writeString(value.animationName());
                        buf.writeFloat(value.speedMultiplier());
                        buf.writeBoolean(value.bladeTrail());
                    },
                    buf -> new EntityComboAttackAnimationPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readString(),
                            buf.readFloat(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
