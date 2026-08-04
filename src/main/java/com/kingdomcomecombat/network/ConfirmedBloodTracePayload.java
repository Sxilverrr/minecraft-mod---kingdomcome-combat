package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConfirmedBloodTracePayload(int targetEntityId) implements CustomPayload {
    public static final Id<ConfirmedBloodTracePayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "confirmed_blood_trace"));
    public static final PacketCodec<RegistryByteBuf, ConfirmedBloodTracePayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeInt(value.targetEntityId()),
            buf -> new ConfirmedBloodTracePayload(buf.readInt())
    );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
