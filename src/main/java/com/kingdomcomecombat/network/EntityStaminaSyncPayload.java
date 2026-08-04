package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityStaminaSyncPayload(
        int entityId,
        float current,
        float max
) implements CustomPayload {
    public static final Id<EntityStaminaSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_stamina_sync"));

    public static final PacketCodec<RegistryByteBuf, EntityStaminaSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.entityId());
                        buf.writeFloat(value.current());
                        buf.writeFloat(value.max());
                    },
                    buf -> new EntityStaminaSyncPayload(
                            buf.readVarInt(),
                            buf.readFloat(),
                            buf.readFloat()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
