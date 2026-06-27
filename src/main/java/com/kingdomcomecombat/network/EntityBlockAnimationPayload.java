package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityBlockAnimationPayload(
        int entityId,
        int animationType,
        int directionOrdinal,
        int attackerEntityId
) implements CustomPayload {
    public static final int PERFECT = 0;
    public static final int UNPERFECT_1 = 1;
    public static final int UNPERFECT_2 = 2;

    public static final Id<EntityBlockAnimationPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_block_animation"));

    public static final PacketCodec<RegistryByteBuf, EntityBlockAnimationPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.entityId());
                        buf.writeInt(value.animationType());
                        buf.writeInt(value.directionOrdinal());
                        buf.writeInt(value.attackerEntityId());
                    },
                    buf -> new EntityBlockAnimationPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
