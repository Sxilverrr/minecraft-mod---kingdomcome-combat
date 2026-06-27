package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityAttackImpactPayload(
        int entityId
) implements CustomPayload {
    public static final Id<EntityAttackImpactPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_attack_impact"));

    public static final PacketCodec<RegistryByteBuf, EntityAttackImpactPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeInt(value.entityId()),
                    buf -> new EntityAttackImpactPayload(buf.readInt())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
