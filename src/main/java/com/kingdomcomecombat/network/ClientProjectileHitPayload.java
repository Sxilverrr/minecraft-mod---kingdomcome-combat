package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClientProjectileHitPayload(
        int projectileEntityId,
        int targetEntityId,
        double hitX,
        double hitY,
        double hitZ
) implements CustomPayload {
    public static final Id<ClientProjectileHitPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "client_projectile_hit"));

    public static final PacketCodec<RegistryByteBuf, ClientProjectileHitPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.projectileEntityId());
                        buf.writeInt(value.targetEntityId());
                        buf.writeDouble(value.hitX());
                        buf.writeDouble(value.hitY());
                        buf.writeDouble(value.hitZ());
                    },
                    buf -> new ClientProjectileHitPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
