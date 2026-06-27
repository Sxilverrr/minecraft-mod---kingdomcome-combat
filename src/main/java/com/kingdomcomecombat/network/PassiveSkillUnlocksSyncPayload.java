package com.kingdomcomecombat.network;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record PassiveSkillUnlocksSyncPayload(List<String> skillIds) implements CustomPayload {
    public static final Id<PassiveSkillUnlocksSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "passive_skill_unlocks_sync"));

    public static final PacketCodec<RegistryByteBuf, PassiveSkillUnlocksSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeVarInt(value.skillIds().size());
                        for (String skillId : value.skillIds()) {
                            buf.writeString(skillId);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<String> skillIds = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            skillIds.add(buf.readString());
                        }
                        return new PassiveSkillUnlocksSyncPayload(skillIds);
                    }
            );

    public PassiveSkillUnlocksSyncPayload {
        skillIds = List.copyOf(skillIds);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
