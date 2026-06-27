package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HorseControlPayload(
        float sideways,
        float forward,
        boolean sprintPressed,
        boolean jumpPressed,
        float yaw
) implements CustomPayload {
    public static final Id<HorseControlPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "horse_control"));

    public static final PacketCodec<RegistryByteBuf, HorseControlPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeFloat(value.sideways());
                        buf.writeFloat(value.forward());
                        buf.writeBoolean(value.sprintPressed());
                        buf.writeBoolean(value.jumpPressed());
                        buf.writeFloat(value.yaw());
                    },
                    buf -> new HorseControlPayload(
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readFloat()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
