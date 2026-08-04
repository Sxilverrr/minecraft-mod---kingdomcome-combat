package com.kingdomcomecombat.network;

import com.google.gson.Gson;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.ExecutionMoveConfigs;
import com.kingdomcomecombat.combat.ExecutionTargetConfig;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.item.SkillBookTexts;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.RangedWeaponAttributesRegistry;
import com.kingdomcomecombat.equipment.EquipmentClientDefaults;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfiles;
import com.kingdomcomecombat.ai.BeastCombatAiProfiles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SkillUiDataSyncPayload(String combosJson, String textsJson, String shieldsJson, String weaponsJson,
                                     String equipmentDefaultsJson,
                                     String armorJson, String rangedWeaponsJson,
                                     String executionsJson, String executionTargetsJson,
                                     String attackMoveDirectionsJson, String attackMovesJson,
                                     double realHitboxSizeX, double realHitboxSizeY, double realHitboxSizeZ,
                                     double realHitboxOffsetX, double realHitboxOffsetY, double realHitboxOffsetZ,
                                     double realHitboxRotationX, double realHitboxRotationY, double realHitboxRotationZ,
                                     String humanoidAiEntityIdsJson, String aiEntityIdsJson) implements CustomPayload {
    private static final Gson GSON = new Gson();
    public static final Id<SkillUiDataSyncPayload> ID = new Id<>(Identifier.of(KingdomComeCombat.MOD_ID, "skill_ui_data_sync"));
    public static final PacketCodec<RegistryByteBuf, SkillUiDataSyncPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.combosJson(), 2_000_000);
                buf.writeString(value.textsJson(), 2_000_000);
                buf.writeString(value.shieldsJson(), 1_000_000);
                buf.writeString(value.weaponsJson(), 2_000_000);
                buf.writeString(value.equipmentDefaultsJson(), 1_000_000);
                buf.writeString(value.armorJson(), 2_000_000);
                buf.writeString(value.rangedWeaponsJson(), 1_000_000);
                buf.writeString(value.executionsJson(), 2_000_000);
                buf.writeString(value.executionTargetsJson(), 1_000_000);
                buf.writeString(value.attackMoveDirectionsJson(), 2_000_000);
                buf.writeString(value.attackMovesJson(), 2_000_000);
                buf.writeDouble(value.realHitboxSizeX());
                buf.writeDouble(value.realHitboxSizeY());
                buf.writeDouble(value.realHitboxSizeZ());
                buf.writeDouble(value.realHitboxOffsetX());
                buf.writeDouble(value.realHitboxOffsetY());
                buf.writeDouble(value.realHitboxOffsetZ());
                buf.writeDouble(value.realHitboxRotationX());
                buf.writeDouble(value.realHitboxRotationY());
                buf.writeDouble(value.realHitboxRotationZ());
                buf.writeString(value.humanoidAiEntityIdsJson(), 1_000_000);
                buf.writeString(value.aiEntityIdsJson(), 1_000_000);
            },
            buf -> new SkillUiDataSyncPayload(
                    buf.readString(2_000_000), buf.readString(2_000_000),
                    buf.readString(1_000_000), buf.readString(2_000_000),
                    buf.readString(1_000_000),
                    buf.readString(2_000_000), buf.readString(1_000_000),
                    buf.readString(2_000_000), buf.readString(1_000_000),
                    buf.readString(2_000_000), buf.readString(2_000_000),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readString(1_000_000), buf.readString(1_000_000)));

    public static SkillUiDataSyncPayload current() {
        var size = AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits();
        var offset = AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits();
        var rotation = AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees();
        return new SkillUiDataSyncPayload(GSON.toJson(ComboMoveConfigs.all()), GSON.toJson(SkillBookTexts.all()),
                GSON.toJson(EquipmentCombatAttributesRegistry.shieldSnapshot()),
                GSON.toJson(EquipmentCombatAttributesRegistry.clientWeaponSnapshot()),
                GSON.toJson(EquipmentClientDefaults.current()),
                GSON.toJson(EquipmentCombatAttributesRegistry.clientArmorSnapshot()),
                GSON.toJson(RangedWeaponAttributesRegistry.snapshot()),
                GSON.toJson(ExecutionMoveConfigs.snapshot()),
                GSON.toJson(ExecutionTargetConfig.snapshot()),
                GSON.toJson(AttackMoveConfigs.directionSnapshot()),
                GSON.toJson(AttackMoveConfigs.namedSnapshot()),
                size.x, size.y, size.z,
                offset.x, offset.y, offset.z,
                rotation.x, rotation.y, rotation.z,
                GSON.toJson(HumanoidCombatAiProfiles.configuredEntityIds()),
                GSON.toJson(java.util.stream.Stream.concat(HumanoidCombatAiProfiles.configuredEntityIds().stream(),
                        BeastCombatAiProfiles.configuredEntityIds().stream()).collect(java.util.stream.Collectors.toSet())));
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
