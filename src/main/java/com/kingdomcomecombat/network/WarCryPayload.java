package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WarCryPayload() implements CustomPayload {
    public static final Id<WarCryPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "war_cry"));

    public static final PacketCodec<RegistryByteBuf, WarCryPayload> CODEC =
            PacketCodec.of((value, buf) -> {}, buf -> new WarCryPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
