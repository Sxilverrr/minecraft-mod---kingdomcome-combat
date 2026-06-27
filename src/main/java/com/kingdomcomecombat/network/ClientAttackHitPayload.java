package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClientAttackHitPayload(
        int attackerEntityId,
        int targetEntityId,
        int partOrdinal,
        double hitX,
        double hitY,
        double hitZ,
        boolean extraHeadHit,
        long attackInstanceId
) implements CustomPayload {
    public static final Id<ClientAttackHitPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "client_attack_hit"));

    public static final PacketCodec<RegistryByteBuf, ClientAttackHitPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.attackerEntityId());
                        buf.writeInt(value.targetEntityId());
                        buf.writeInt(value.partOrdinal());
                        buf.writeDouble(value.hitX());
                        buf.writeDouble(value.hitY());
                        buf.writeDouble(value.hitZ());
                        buf.writeBoolean(value.extraHeadHit());
                        buf.writeLong(value.attackInstanceId());
                    },
                    buf -> new ClientAttackHitPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readBoolean(),
                            buf.readLong()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
