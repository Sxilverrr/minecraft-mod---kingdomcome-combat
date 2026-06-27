package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StaminaSyncPayload(
        float current,
        float max
) implements CustomPayload {
    public static final Id<StaminaSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "stamina_sync"));

    public static final PacketCodec<RegistryByteBuf, StaminaSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeFloat(value.current());
                        buf.writeFloat(value.max());
                    },
                    buf -> new StaminaSyncPayload(
                            buf.readFloat(),
                            buf.readFloat()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
