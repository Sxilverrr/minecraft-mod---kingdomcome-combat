package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartAttackPayload(
        int directionOrdinal,
        int targetEntityId,
        boolean lockedLunge,
        boolean movementKeyPressed,
        int lungeForwardInput,
        int lungeSideInput,
        long attackInstanceId
) implements CustomPayload {
    public static final Id<StartAttackPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "start_attack"));

    public static final PacketCodec<RegistryByteBuf, StartAttackPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.directionOrdinal());
                        buf.writeInt(value.targetEntityId());
                        buf.writeBoolean(value.lockedLunge());
                        buf.writeBoolean(value.movementKeyPressed());
                        buf.writeInt(value.lungeForwardInput());
                        buf.writeInt(value.lungeSideInput());
                        buf.writeLong(value.attackInstanceId());
                    },
                    buf -> new StartAttackPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readLong()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
