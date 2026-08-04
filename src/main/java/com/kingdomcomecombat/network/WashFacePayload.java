package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record WashFacePayload(BlockPos pos) implements CustomPayload {
    public static final Id<WashFacePayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "wash_face"));

    public static final PacketCodec<RegistryByteBuf, WashFacePayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeBlockPos(value.pos()),
                    buf -> new WashFacePayload(buf.readBlockPos())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
