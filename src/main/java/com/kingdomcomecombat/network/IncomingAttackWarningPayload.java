package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record IncomingAttackWarningPayload(
        int attackerEntityId,
        int directionOrdinal,
        int warningType
) implements CustomPayload {
    public static final Id<IncomingAttackWarningPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "incoming_attack_warning"));

    public static final PacketCodec<RegistryByteBuf, IncomingAttackWarningPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.attackerEntityId());
                        buf.writeInt(value.directionOrdinal());
                        buf.writeInt(value.warningType());
                    },
                    buf -> new IncomingAttackWarningPayload(
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
