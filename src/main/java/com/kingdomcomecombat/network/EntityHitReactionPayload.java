package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityHitReactionPayload(
        int entityId,
        int partOrdinal,
        String detailedPart,
        int attackDirectionOrdinal,
        boolean strong,
        float strength,
        String animationName,
        boolean hitReaction
) implements CustomPayload {
    public static final Id<EntityHitReactionPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "entity_hit_reaction"));

    public static final PacketCodec<RegistryByteBuf, EntityHitReactionPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.entityId());
                        buf.writeInt(value.partOrdinal());
                        buf.writeString(value.detailedPart());
                        buf.writeInt(value.attackDirectionOrdinal());
                        buf.writeBoolean(value.strong());
                        buf.writeFloat(value.strength());
                        buf.writeString(value.animationName());
                        buf.writeBoolean(value.hitReaction());
                    },
                    buf -> new EntityHitReactionPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readString(32),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readFloat(),
                            buf.readString(64),
                            buf.readBoolean()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
