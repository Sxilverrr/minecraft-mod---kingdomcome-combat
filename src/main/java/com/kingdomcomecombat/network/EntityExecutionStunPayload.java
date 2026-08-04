package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityExecutionStunPayload(
        int targetEntityId,
        int attackerEntityId,
        int directionOrdinal,
        int ticks
) implements CustomPayload {
    public static final Id<EntityExecutionStunPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_execution_stun"));

    public static final PacketCodec<RegistryByteBuf, EntityExecutionStunPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.targetEntityId());
                        buf.writeInt(value.attackerEntityId());
                        buf.writeInt(value.directionOrdinal());
                        buf.writeInt(value.ticks());
                    },
                    buf -> new EntityExecutionStunPayload(
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
