package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LearnExperiencePassiveSkillPayload(String passiveId) implements CustomPayload {
    public static final Id<LearnExperiencePassiveSkillPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "learn_experience_passive_skill"));

    public static final PacketCodec<RegistryByteBuf, LearnExperiencePassiveSkillPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeString(value.passiveId()),
                    buf -> new LearnExperiencePassiveSkillPayload(buf.readString())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
