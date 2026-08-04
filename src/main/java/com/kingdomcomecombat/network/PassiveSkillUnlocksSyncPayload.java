package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record PassiveSkillUnlocksSyncPayload(
        List<String> skillIds,
        int combatExperience,
        int killReward,
        int perfectBlockReward,
        int perfectCounterReward,
        int attackReward,
        int masterCounterReward,
        int comboReward,
        double vanillaExperienceMultiplier
) implements CustomPayload {
    public static final Id<PassiveSkillUnlocksSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "passive_skill_unlocks_sync"));

    public static final PacketCodec<RegistryByteBuf, PassiveSkillUnlocksSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.skillIds().size());
                        for (String skillId : value.skillIds()) {
                            buf.writeString(skillId);
                        }
                        buf.writeVarInt(value.combatExperience());
                        buf.writeVarInt(value.killReward());
                        buf.writeVarInt(value.perfectBlockReward());
                        buf.writeVarInt(value.perfectCounterReward());
                        buf.writeVarInt(value.attackReward());
                        buf.writeVarInt(value.masterCounterReward());
                        buf.writeVarInt(value.comboReward());
                        buf.writeDouble(value.vanillaExperienceMultiplier());
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<String> skillIds = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            skillIds.add(buf.readString());
                        }
                        return new PassiveSkillUnlocksSyncPayload(
                                skillIds,
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readDouble()
                        );
                    }
            );

    public PassiveSkillUnlocksSyncPayload {
        skillIds = List.copyOf(skillIds);
        combatExperience = Math.max(0, combatExperience);
        killReward = Math.max(0, killReward);
        perfectBlockReward = Math.max(0, perfectBlockReward);
        perfectCounterReward = Math.max(0, perfectCounterReward);
        attackReward = Math.max(0, attackReward);
        masterCounterReward = Math.max(0, masterCounterReward);
        comboReward = Math.max(0, comboReward);
        vanillaExperienceMultiplier = Math.max(0.0, vanillaExperienceMultiplier);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
