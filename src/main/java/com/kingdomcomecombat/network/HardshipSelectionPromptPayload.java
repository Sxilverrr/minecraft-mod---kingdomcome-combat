package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HardshipSelectionPromptPayload(String definitionsJson, int minimum) implements CustomPayload {
    public static final Id<HardshipSelectionPromptPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "hardship_selection_prompt"));
    public static final PacketCodec<RegistryByteBuf, HardshipSelectionPromptPayload> CODEC = PacketCodec.of(
            (value, buf) -> { buf.writeString(value.definitionsJson(), 1_000_000); buf.writeVarInt(value.minimum()); },
            buf -> new HardshipSelectionPromptPayload(buf.readString(1_000_000), buf.readVarInt())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
