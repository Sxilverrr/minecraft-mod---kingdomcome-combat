package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityCombatStancePayload(
        int entityId,
        int targetEntityId,
        int directionOrdinal,
        float speedMultiplier,
        String animationName
) implements CustomPayload {
    public static final Id<EntityCombatStancePayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_combat_stance"));

    public static final PacketCodec<RegistryByteBuf, EntityCombatStancePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.entityId());
                        buf.writeInt(value.targetEntityId());
                        buf.writeInt(value.directionOrdinal());
                        buf.writeFloat(value.speedMultiplier());
                        buf.writeString(value.animationName());
                    },
                    buf -> new EntityCombatStancePayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readFloat(),
                            buf.readString()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
