package com.kingdomcomecombat.network;

import com.google.gson.Gson;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.passive.PassiveSkillConfig;
import com.kingdomcomecombat.passive.PassiveSkillConfigs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record PassiveSkillConfigsSyncPayload(String json) implements CustomPayload {
    private static final Gson GSON = new Gson();
    public static final Id<PassiveSkillConfigsSyncPayload> ID =
            new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "passive_skill_configs_sync"));
    public static final PacketCodec<RegistryByteBuf, PassiveSkillConfigsSyncPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeString(value.json(), 1_000_000),
                    buf -> new PassiveSkillConfigsSyncPayload(buf.readString(1_000_000))
            );

    public PassiveSkillConfigsSyncPayload {
        json = json == null ? "[]" : json;
    }

    public static PassiveSkillConfigsSyncPayload current() {
        List<PassiveSkillConfig> skills = PassiveSkillConfigs.all();
        return new PassiveSkillConfigsSyncPayload(GSON.toJson(skills));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
