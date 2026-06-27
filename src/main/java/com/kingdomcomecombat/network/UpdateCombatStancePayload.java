package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateCombatStancePayload(int directionOrdinal, boolean locked) implements CustomPayload {
    public static final Id<UpdateCombatStancePayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "update_combat_stance"));

    public static final PacketCodec<RegistryByteBuf, UpdateCombatStancePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.directionOrdinal());
                        buf.writeBoolean(value.locked());
                    },
                    buf -> new UpdateCombatStancePayload(buf.readInt(), buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
