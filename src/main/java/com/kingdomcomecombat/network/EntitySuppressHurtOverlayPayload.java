package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntitySuppressHurtOverlayPayload(
        int entityId,
        int ticks
) implements CustomPayload {
    public static final Id<EntitySuppressHurtOverlayPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_suppress_hurt_overlay"));

    public static final PacketCodec<RegistryByteBuf, EntitySuppressHurtOverlayPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.entityId());
                        buf.writeInt(value.ticks());
                    },
                    buf -> new EntitySuppressHurtOverlayPayload(
                            buf.readInt(),
                            buf.readInt()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
