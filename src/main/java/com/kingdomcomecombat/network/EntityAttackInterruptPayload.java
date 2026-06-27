package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityAttackInterruptPayload(int entityId) implements CustomPayload {
    public static final Id<EntityAttackInterruptPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_attack_interrupt"));

    public static final PacketCodec<RegistryByteBuf, EntityAttackInterruptPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeInt(value.entityId()),
                    buf -> new EntityAttackInterruptPayload(buf.readInt())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
