package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityDodgeAnimationPayload(int entityId, int directionOrdinal) implements CustomPayload {
    public static final Id<EntityDodgeAnimationPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_dodge_animation"));

    public static final PacketCodec<RegistryByteBuf, EntityDodgeAnimationPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.entityId());
                        buf.writeInt(value.directionOrdinal());
                    },
                    buf -> new EntityDodgeAnimationPayload(buf.readInt(), buf.readInt())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
