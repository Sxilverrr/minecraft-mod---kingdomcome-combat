package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LearnSkillBookPayload(String comboId, String passiveId) implements CustomPayload {
    public static final Id<LearnSkillBookPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "learn_skill_book"));

    public static final PacketCodec<RegistryByteBuf, LearnSkillBookPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.comboId());
                        buf.writeString(value.passiveId());
                    },
                    buf -> new LearnSkillBookPayload(buf.readString(), buf.readString())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
