package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenSkillBookPayload(
        String comboId,
        String passiveId,
        String title,
        String firstPage,
        String secondPage,
        String illustration,
        boolean learned
) implements CustomPayload {
    public static final Id<OpenSkillBookPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "open_skill_book"));

    public static final PacketCodec<RegistryByteBuf, OpenSkillBookPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.comboId());
                        buf.writeString(value.passiveId());
                        buf.writeString(value.title());
                        buf.writeString(value.firstPage());
                        buf.writeString(value.secondPage());
                        buf.writeString(value.illustration());
                        buf.writeBoolean(value.learned());
                    },
                    buf -> new OpenSkillBookPayload(
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
