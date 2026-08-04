package com.kingdomcomecombat.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfile;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfileSpec;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfiles;
import com.kingdomcomecombat.ai.BeastCombatAiProfile;
import com.kingdomcomecombat.ai.BeastCombatAiProfiles;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.CombatAnimationNames;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.CombatMovementConfig;
import com.kingdomcomecombat.combat.CombatTiming;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.DodgeDamageConfig;
import com.kingdomcomecombat.combat.ExecutionMoveConfigs;
import com.kingdomcomecombat.combat.ExecutionTargetConfig;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.equipment.ArmorCombatAttributes;
import com.kingdomcomecombat.equipment.BloodSplashConfig;
import com.kingdomcomecombat.equipment.DamageTypeProfile;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.EquipmentFallbackConfig;
import com.kingdomcomecombat.equipment.MobCombatAttributes;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.MobScaleRegistry;
import com.kingdomcomecombat.equipment.ProjectileCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.RangedWeaponAttributes;
import com.kingdomcomecombat.equipment.RangedWeaponAttributesRegistry;
import com.kingdomcomecombat.equipment.ShieldCombatAttributes;
import com.kingdomcomecombat.equipment.WeaponCombatAttributes;
import com.kingdomcomecombat.item.SkillBookTexts;
import com.kingdomcomecombat.hardship.HardshipConfig;
import com.kingdomcomecombat.hardship.HardshipConfigs;
import com.kingdomcomecombat.passive.PassiveSkillConfig;
import com.kingdomcomecombat.passive.PassiveSkillConfigs;
import com.kingdomcomecombat.passive.CombatExperienceConfig;
import com.kingdomcomecombat.combat.CombatItemUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CombatDataReloadListener implements SimpleSynchronousResourceReloadListener {
    private static final Identifier ID =
            Identifier.of(KingdomComeCombat.MOD_ID, "combat_data");
    private static volatile List<WeaponDefinition> cachedWeaponDefinitions = List.of();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        ComboMoveConfigs.clear();
        ExecutionMoveConfigs.clear();
        AttackMoveConfigs.clear();
        HumanoidCombatAiProfiles.clear();
        BeastCombatAiProfiles.clear();
        HumanoidCombatAiProfiles.registerDefaults();
        AnimatedAttackHitboxLibrary.setRealHitboxSizeUnits(1.0, 1.0, 14.0);
        AnimatedAttackHitboxLibrary.setRealHitboxOffsetUnits(0.0, 0.0, 0.0);
        AnimatedAttackHitboxLibrary.setRealHitboxRotationDegrees(0.0, 0.0, 0.0);
        CombatMovementConfig.reset();
        DodgeDamageConfig.reset();
        EquipmentCombatAttributesRegistry.clear();
        MobCombatAttributesRegistry.clear();
        MobScaleRegistry.clear();
        ProjectileCombatAttributesRegistry.clear();
        RangedWeaponAttributesRegistry.clear();
        BloodSplashConfig.clear();
        SkillBookTexts.clear();
        PassiveSkillConfigs.clear();
        CombatExperienceConfig.reset();
        HardshipConfigs.clear();
        EquipmentFallbackConfig.reset();
        ExecutionTargetConfig.reset();
        loadEquipmentDefaults(manager);
        loadCombos(manager);
        loadExecutions(manager);
        loadExecutionTargets(manager);
        loadMoves(manager);
        loadWeapons(manager);
        loadRangedWeapons(manager);
        loadShields(manager);
        loadArmor(manager);
        loadProjectiles(manager);
        loadMobAttributes(manager);
        loadAiProfiles(manager);
        loadBeastAiProfiles(manager);
        loadBloodSplash(manager);
        loadPassiveSkills(manager);
        loadCombatExperience(manager);
        loadHardships(manager);
        loadSkillBooks(manager);
        CombatMovementConfig.setCombatMinDistance(CombatServerConfig.combatMinDistance());
    }

    /**
     * Returns every matching resource in data-pack priority order (lowest first).
     *
     * <p>{@link ResourceManager#findResources} keeps only the highest-priority
     * resource for each identifier and then orders the result by identifier. That
     * is not sufficient for KCC data: packs are allowed to use their own namespace
     * while overriding semantic ids inside the JSON. In that case an identifier
     * sort can make the bundled namespace overwrite an enabled external pack.
     */
    private static List<Map.Entry<Identifier, Resource>> findJsonResources(
            ResourceManager manager,
            String path
    ) {
        Map<String, Integer> packPriority = new HashMap<>();
        int[] priority = {0};
        manager.streamResourcePacks().forEach(pack ->
                packPriority.put(pack.getId(), priority[0]++));

        List<Map.Entry<Identifier, Resource>> resources = new ArrayList<>();
        manager.findAllResources(path, id -> id.getPath().endsWith(".json"))
                .forEach((id, stack) -> stack.forEach(resource ->
                        resources.add(new AbstractMap.SimpleImmutableEntry<>(id, resource))));
        resources.sort(Comparator
                .comparingInt((Map.Entry<Identifier, Resource> entry) ->
                        packPriority.getOrDefault(entry.getValue().getPackId(), -1))
                .thenComparing(Map.Entry::getKey));
        return resources;
    }

    private static void loadHardships(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/hardships")) {
            JsonObject root = readJson(entry.getKey(), entry.getValue());
            JsonArray debuffs = root.getAsJsonArray("debuffs");
            if (debuffs == null) continue;
            for (JsonElement element : debuffs) {
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                Map<String, HardshipConfig.Translation> translations = new HashMap<>();
                JsonObject translated = json.getAsJsonObject("translations");
                if (translated != null) {
                    for (Map.Entry<String, JsonElement> text : translated.entrySet()) {
                        if (!text.getValue().isJsonObject()) continue;
                        JsonObject value = text.getValue().getAsJsonObject();
                        translations.put(text.getKey().toLowerCase(), new HardshipConfig.Translation(
                                firstString(value, "", "title", "translated_title"),
                                firstString(value, "", "description", "translated_description")
                        ));
                    }
                }
                HardshipConfigs.register(new HardshipConfig(
                        firstString(json, "", "id"),
                        firstString(json, "", "title"),
                        firstString(json, "", "description"),
                        firstString(json, "", "icon"),
                        firstString(json, "", "translated_title", "translation_title"),
                        firstString(json, "", "translated_description", "translation_description"),
                        translations,
                        numberMap(json.getAsJsonObject("preks"))
                ));
            }
        }
    }

    private static void loadPassiveSkills(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/passive_skills")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            if (json.has("skills")) {
                JsonElement skills = json.get("skills");
                if (skills.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> skill : skills.getAsJsonObject().entrySet()) {
                        registerPassiveSkill(skill.getKey(), skill.getValue().getAsJsonObject());
                    }
                } else if (skills.isJsonArray()) {
                    for (JsonElement skill : skills.getAsJsonArray()) {
                        registerPassiveSkill(stripJson(entry.getKey()), skill.getAsJsonObject());
                    }
                }
            } else {
                registerPassiveSkill(stripJson(entry.getKey()), json);
            }
        }
    }

    private static void loadCombatExperience(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/combat_experience")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            CombatExperienceConfig.set(
                    integer(json, "kill", CombatExperienceConfig.kill()),
                    integer(json, "perfect_block", CombatExperienceConfig.perfectBlock()),
                    integer(json, "perfect_counter", CombatExperienceConfig.perfectCounter()),
                    integer(json, "attack", CombatExperienceConfig.attack()),
                    integer(json, "master_counter", CombatExperienceConfig.masterCounter()),
                    integer(json, "combo", CombatExperienceConfig.combo()),
                    number(json, "vanilla_experience_multiplier",
                            CombatExperienceConfig.vanillaExperienceMultiplier())
            );
        }
    }

    private static void registerPassiveSkill(String fallbackId, JsonObject json) {
        String id = firstString(json, fallbackId, "id", "skill_id", "passive_id");
        String source = firstString(json, "book", "source", "unlock_type", "type");
        PassiveSkillConfig.Source unlockSource;
        if (source.equalsIgnoreCase("advancement")
                || source.equalsIgnoreCase("achievement")
                || source.equalsIgnoreCase("criteria")) {
            unlockSource = PassiveSkillConfig.Source.ADVANCEMENT;
        } else if (source.equalsIgnoreCase("experience")
                || source.equalsIgnoreCase("xp")
                || source.equalsIgnoreCase("exp")) {
            unlockSource = PassiveSkillConfig.Source.EXPERIENCE;
        } else {
            unlockSource = PassiveSkillConfig.Source.BOOK;
        }
        PassiveSkillConfigs.register(new PassiveSkillConfig(
                id,
                firstString(json, id, "name", "display_name", "title"),
                firstString(json, "", "description", "desc", "text"),
                firstString(json, "", "icon", "texture"),
                unlockSource,
                firstString(json, "", "advancement", "achievement"),
                integer(json, "experience_cost", integer(json, "xp_cost", integer(json, "cost", 0))),
                passiveTranslations(json),
                numberMap(json.getAsJsonObject("perks"))
        ));
    }

    private static void loadBloodSplash(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/blood_splash")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            BloodSplashConfig.setDefaultMultiplier(number(json, "default_multiplier", 1.0));
            readBloodSplashMultipliers(json.getAsJsonObject("damage_types"), false);
            readBloodSplashMultipliers(json.getAsJsonObject("damage_type_tags"), true);
        }
    }

    private static void readBloodSplashMultipliers(JsonObject object, boolean tag) {
        if (object == null) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) {
                continue;
            }
            double multiplier = entry.getValue().getAsDouble();
            if (tag) {
                BloodSplashConfig.putTag(TagKey.of(RegistryKeys.DAMAGE_TYPE, id), multiplier);
            } else {
                BloodSplashConfig.putType(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id), multiplier);
            }
        }
    }

    private static void loadSkillBooks(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/skill_books")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            String fallbackId = stripJson(entry.getKey());
            String id = firstString(json, fallbackId, "id", "book_id", "combo", "combo_id");
            SkillBookTexts.put(
                    id,
                    new SkillBookTexts.Entry(
                            firstString(json, "技能书", "title", "name"),
                            string(json, "author", "Kingdom Come Combat"),
                            firstString(json, "这本书记录了一门战斗技巧。", "first_page", "page", "text"),
                            firstString(json, "翻到这一页时，你会试着领会书中记载的招式。", "second_page", "second_text", "learn_page"),
                            firstString(json, "", "illustration", "image", "picture"),
                            bookTranslations(json)
                    )
            );
        }
    }

    private static void loadEquipmentDefaults(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/defaults")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            JsonObject earlyGlobal = json.getAsJsonObject("global");
            if (earlyGlobal != null) {
                EquipmentFallbackConfig.setImperfectBlockImpactMitigation(
                        number(earlyGlobal, "imperfect_block_impact_mitigation", EquipmentFallbackConfig.imperfectBlockImpactMitigation())
                );
                EquipmentFallbackConfig.setDefaultWeaponToughness(
                        number(earlyGlobal, "weapon_toughness", EquipmentFallbackConfig.defaultWeaponToughness())
                );
                EquipmentFallbackConfig.setDefaultMinimumDurabilityPanelMultiplier(
                        number(earlyGlobal, "minimum_durability_panel_multiplier", EquipmentFallbackConfig.defaultMinimumDurabilityPanelMultiplier())
                );
                EquipmentFallbackConfig.setDefaultHeldMovementSpeedMultiplier(
                        number(earlyGlobal, "held_movement_speed_multiplier", EquipmentFallbackConfig.defaultHeldMovementSpeedMultiplier())
                );
            }
            JsonObject weapons = json.getAsJsonObject("weapons");
            if (weapons != null) {
                EquipmentFallbackConfig.setConfiguredLongswordItems(identifierSet(weapons.getAsJsonArray("longsword_items")));
                EquipmentFallbackConfig.setConfiguredShortSwordItems(identifierSet(weapons.getAsJsonArray("short_sword_items")));
                EquipmentFallbackConfig.setConfiguredHeavyWeaponItems(identifierSet(weapons.getAsJsonArray("heavy_weapon_items")));
                Set<Identifier> polearmItems = identifierSet(weapons.getAsJsonArray("polearm_items"));
                EquipmentFallbackConfig.setConfiguredPolearmItems(polearmItems);
                JsonObject polearm = weapons.getAsJsonObject("polearm");
                if (polearm != null) {
                    EquipmentFallbackConfig.setPolearm(damageProfile(polearm, 1.0));
                    EquipmentFallbackConfig.setPolearmBaseImpact(
                            firstNumber(polearm, EquipmentFallbackConfig.polearmBaseImpact(), "base_impact", "impact"));
                    EquipmentFallbackConfig.setPolearmBlockImpactMitigation(
                            firstNumber(polearm, EquipmentFallbackConfig.polearmBlockImpactMitigation(), "block_impact_mitigation"));
                    EquipmentFallbackConfig.setPolearmArmorBreakMultiplier(
                            firstNumber(polearm, EquipmentFallbackConfig.polearmArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier"));
                    EquipmentFallbackConfig.setPolearmWeaponToughness(
                            firstNumber(polearm, EquipmentFallbackConfig.defaultWeaponToughness(), "weapon_toughness"));
                    EquipmentFallbackConfig.setPolearmMinimumDurabilityPanelMultiplier(
                            firstNumber(
                                    polearm,
                                    EquipmentFallbackConfig.defaultMinimumDurabilityPanelMultiplier(),
                                    "minimum_durability_panel_multiplier"
                            ));
                    EquipmentFallbackConfig.setPolearmHeldMovementSpeedMultiplier(
                            firstNumber(
                                    polearm,
                                    EquipmentFallbackConfig.defaultHeldMovementSpeedMultiplier(),
                                    "held_movement_speed_multiplier"
                            ));
                    loadDefaultWeaponHitbox(polearm, "polearm");
                }
                JsonObject defaultWeapon = weapons.getAsJsonObject("default");
                EquipmentFallbackConfig.setDefaultWeapon(
                        damageProfile(defaultWeapon, 1.0)
                );
                EquipmentFallbackConfig.setDefaultWeaponBaseImpact(
                        firstNumber(defaultWeapon, EquipmentFallbackConfig.defaultWeaponBaseImpact(), "base_impact", "impact")
                );
                EquipmentFallbackConfig.setDefaultWeaponBlockImpactMitigation(
                        firstNumber(defaultWeapon, EquipmentFallbackConfig.imperfectBlockImpactMitigation(), "block_impact_mitigation")
                );
                EquipmentFallbackConfig.setDefaultArmorBreakMultiplier(
                        firstNumber(defaultWeapon, EquipmentFallbackConfig.defaultArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier")
                );
                if (defaultWeapon != null) {
                    if (defaultWeapon.has("executions")) {
                        EquipmentFallbackConfig.setDefaultWeaponExecutionMoveIds(stringMap(defaultWeapon.getAsJsonObject("executions")));
                    } else if (defaultWeapon.has("execution_moves")) {
                        EquipmentFallbackConfig.setDefaultWeaponExecutionMoveIds(stringMap(defaultWeapon.getAsJsonObject("execution_moves")));
                    }
                }
                loadDefaultWeaponHitbox(defaultWeapon, "default");
                JsonObject sword = weapons.getAsJsonObject("sword");
                EquipmentFallbackConfig.setSword(
                        damageProfile(sword, 1.0)
                );
                EquipmentFallbackConfig.setSwordBaseImpact(
                        firstNumber(sword, EquipmentFallbackConfig.swordBaseImpact(), "base_impact", "impact")
                );
                EquipmentFallbackConfig.setSwordBlockImpactMitigation(
                        firstNumber(sword, EquipmentFallbackConfig.imperfectBlockImpactMitigation(), "block_impact_mitigation")
                );
                EquipmentFallbackConfig.setSwordArmorBreakMultiplier(
                        firstNumber(sword, EquipmentFallbackConfig.swordArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier")
                );
                loadDefaultWeaponHitbox(sword, "sword");
                JsonObject longsword = weapons.getAsJsonObject("longsword");
                EquipmentFallbackConfig.setLongsword(
                        damageProfile(longsword, 1.0)
                );
                EquipmentFallbackConfig.setLongswordBaseImpact(
                        firstNumber(longsword, EquipmentFallbackConfig.longswordBaseImpact(), "base_impact", "impact")
                );
                EquipmentFallbackConfig.setLongswordBlockImpactMitigation(
                        firstNumber(longsword, EquipmentFallbackConfig.imperfectBlockImpactMitigation(), "block_impact_mitigation")
                );
                EquipmentFallbackConfig.setLongswordArmorBreakMultiplier(
                        firstNumber(longsword, EquipmentFallbackConfig.longswordArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier")
                );
                loadDefaultWeaponHitbox(longsword, "longsword");
                JsonObject pickaxe = weapons.getAsJsonObject("pickaxe");
                EquipmentFallbackConfig.setPickaxe(
                        damageProfile(pickaxe, 1.0)
                );
                EquipmentFallbackConfig.setPickaxeBaseImpact(
                        firstNumber(pickaxe, EquipmentFallbackConfig.pickaxeBaseImpact(), "base_impact", "impact")
                );
                EquipmentFallbackConfig.setPickaxeBlockImpactMitigation(
                        firstNumber(pickaxe, EquipmentFallbackConfig.imperfectBlockImpactMitigation(), "block_impact_mitigation")
                );
                EquipmentFallbackConfig.setPickaxeArmorBreakMultiplier(
                        firstNumber(pickaxe, EquipmentFallbackConfig.pickaxeArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier")
                );
                loadDefaultWeaponHitbox(pickaxe, "pickaxe");
                JsonObject axe = weapons.getAsJsonObject("axe");
                EquipmentFallbackConfig.setAxe(
                        damageProfile(axe, 1.0)
                );
                EquipmentFallbackConfig.setAxeBaseImpact(
                        firstNumber(axe, EquipmentFallbackConfig.axeBaseImpact(), "base_impact", "impact")
                );
                EquipmentFallbackConfig.setAxeBlockImpactMitigation(
                        firstNumber(axe, EquipmentFallbackConfig.imperfectBlockImpactMitigation(), "block_impact_mitigation")
                );
                EquipmentFallbackConfig.setAxeArmorBreakMultiplier(
                        firstNumber(axe, EquipmentFallbackConfig.axeArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier")
                );
                loadDefaultWeaponHitbox(axe, "axe");
                JsonObject fightingMace = weapons.getAsJsonObject("fighting_mace");
                EquipmentFallbackConfig.setFightingMace(
                        damageProfile(fightingMace, 1.0)
                );
                EquipmentFallbackConfig.setFightingMaceBaseImpact(
                        firstNumber(fightingMace, EquipmentFallbackConfig.fightingMaceBaseImpact(), "base_impact", "impact")
                );
                EquipmentFallbackConfig.setFightingMaceBlockImpactMitigation(
                        firstNumber(fightingMace, EquipmentFallbackConfig.imperfectBlockImpactMitigation(), "block_impact_mitigation")
                );
                EquipmentFallbackConfig.setFightingMaceArmorBreakMultiplier(
                        firstNumber(fightingMace, EquipmentFallbackConfig.fightingMaceArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier")
                );
                loadDefaultWeaponHitbox(fightingMace, "fighting_mace");
                JsonObject shovel = weapons.getAsJsonObject("shovel");
                EquipmentFallbackConfig.setShovel(
                        damageProfile(shovel, 1.0)
                );
                EquipmentFallbackConfig.setShovelBaseImpact(
                        firstNumber(shovel, EquipmentFallbackConfig.shovelBaseImpact(), "base_impact", "impact")
                );
                EquipmentFallbackConfig.setShovelBlockImpactMitigation(
                        firstNumber(shovel, EquipmentFallbackConfig.imperfectBlockImpactMitigation(), "block_impact_mitigation")
                );
                EquipmentFallbackConfig.setShovelArmorBreakMultiplier(
                        firstNumber(shovel, EquipmentFallbackConfig.shovelArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier")
                );
                loadDefaultWeaponHitbox(shovel, "shovel");
                JsonObject hoe = weapons.getAsJsonObject("hoe");
                EquipmentFallbackConfig.setHoe(
                        damageProfile(hoe, 1.0)
                );
                EquipmentFallbackConfig.setHoeBaseImpact(
                        firstNumber(hoe, EquipmentFallbackConfig.hoeBaseImpact(), "base_impact", "impact")
                );
                EquipmentFallbackConfig.setHoeBlockImpactMitigation(
                        firstNumber(hoe, EquipmentFallbackConfig.imperfectBlockImpactMitigation(), "block_impact_mitigation")
                );
                EquipmentFallbackConfig.setHoeArmorBreakMultiplier(
                        firstNumber(hoe, EquipmentFallbackConfig.hoeArmorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier")
                );
                loadDefaultWeaponHitbox(hoe, "hoe");
            }

            JsonObject armor = json.getAsJsonObject("armor");
            if (armor != null) {
                EquipmentFallbackConfig.setHelmetAndBootsArmorMultiplier(
                        damageProfile(armor.getAsJsonObject("helmet_and_boots_multiplier"), 0.0)
                );
                EquipmentFallbackConfig.setChestplateAndLeggingsArmorMultiplier(
                        damageProfile(armor.getAsJsonObject("chestplate_and_leggings_multiplier"), 0.0)
                );
                EquipmentFallbackConfig.setProtectionPercent(number(armor, "protection", 0.5));
                EquipmentFallbackConfig.setImpactMitigationPercent(number(armor, "impact_mitigation", 0.0));

                JsonObject protectedParts = armor.getAsJsonObject("protected_parts_by_slot");
                if (protectedParts != null) {
                    Map<EquipmentSlot, List<String>> slotParts = new HashMap<>();
                    putSlotParts(slotParts, protectedParts, EquipmentSlot.HEAD, "head");
                    putSlotParts(slotParts, protectedParts, EquipmentSlot.CHEST, "chest");
                    putSlotParts(slotParts, protectedParts, EquipmentSlot.LEGS, "legs");
                    putSlotParts(slotParts, protectedParts, EquipmentSlot.FEET, "feet");
                    putSlotParts(slotParts, protectedParts, EquipmentSlot.BODY, "body");
                    EquipmentFallbackConfig.setSlotParts(slotParts);
                }
            }

            JsonObject partDamageMultipliers = json.getAsJsonObject("part_damage_multipliers");
            if (partDamageMultipliers != null) {
                Map<String, Double> multipliers = new HashMap<>();
                for (Map.Entry<String, JsonElement> multiplier : partDamageMultipliers.entrySet()) {
                    multipliers.put(multiplier.getKey(), multiplier.getValue().getAsDouble());
                }
                EquipmentFallbackConfig.setPartDamageMultipliers(multipliers);
            }

            JsonObject enchantments = json.getAsJsonObject("enchantments");
            if (enchantments != null) {
                JsonObject armorDefense = enchantments.getAsJsonObject("armor_defense_per_level");
                if (armorDefense != null) {
                    Map<Identifier, DamageTypeProfile> profiles = new HashMap<>();
                    for (Map.Entry<String, JsonElement> enchantment : armorDefense.entrySet()) {
                        Identifier id = Identifier.tryParse(enchantment.getKey());
                        if (id != null) {
                            profiles.put(
                                    id,
                                    damageProfile(enchantment.getValue().getAsJsonObject(), 0.0)
                            );
                        }
                    }
                    EquipmentFallbackConfig.setArmorDefenseEnchantments(profiles);
                }

                JsonObject armorDefenseFirstTwoBonus =
                        enchantments.getAsJsonObject("armor_defense_first_two_bonus");
                if (armorDefenseFirstTwoBonus != null) {
                    Map<Identifier, DamageTypeProfile> profiles = new HashMap<>();
                    for (Map.Entry<String, JsonElement> enchantment : armorDefenseFirstTwoBonus.entrySet()) {
                        Identifier id = Identifier.tryParse(enchantment.getKey());
                        if (id != null) {
                            profiles.put(
                                    id,
                                    damageProfile(enchantment.getValue().getAsJsonObject(), 0.0)
                            );
                        }
                    }
                    EquipmentFallbackConfig.setArmorDefenseFirstTwoBonusEnchantments(profiles);
                }

                JsonObject armorImpactMitigation =
                        enchantments.getAsJsonObject("armor_impact_mitigation_per_level");
                if (armorImpactMitigation != null) {
                    Map<Identifier, Double> mitigations = new HashMap<>();
                    for (Map.Entry<String, JsonElement> enchantment : armorImpactMitigation.entrySet()) {
                        Identifier id = Identifier.tryParse(enchantment.getKey());
                        if (id != null) {
                            mitigations.put(id, enchantment.getValue().getAsDouble());
                        }
                    }
                    EquipmentFallbackConfig.setArmorImpactMitigationEnchantments(mitigations);
                }

                JsonObject weaponDamage = enchantments.getAsJsonObject("weapon_damage_per_level");
                if (weaponDamage != null) {
                    Map<Identifier, DamageTypeProfile> profiles = new HashMap<>();
                    for (Map.Entry<String, JsonElement> enchantment : weaponDamage.entrySet()) {
                        Identifier id = Identifier.tryParse(enchantment.getKey());
                        if (id != null) {
                            profiles.put(
                                    id,
                                    damageProfile(enchantment.getValue().getAsJsonObject(), 0.0)
                            );
                        }
                    }
                    EquipmentFallbackConfig.setWeaponDamageEnchantments(profiles);
                }

                JsonObject weaponDamageFirstTwoBonus =
                        enchantments.getAsJsonObject("weapon_damage_first_two_bonus");
                if (weaponDamageFirstTwoBonus != null) {
                    Map<Identifier, DamageTypeProfile> profiles = new HashMap<>();
                    for (Map.Entry<String, JsonElement> enchantment : weaponDamageFirstTwoBonus.entrySet()) {
                        Identifier id = Identifier.tryParse(enchantment.getKey());
                        if (id != null) {
                            profiles.put(
                                    id,
                                    damageProfile(enchantment.getValue().getAsJsonObject(), 0.0)
                            );
                        }
                    }
                    EquipmentFallbackConfig.setWeaponDamageFirstTwoBonusEnchantments(profiles);
                }

                JsonObject durabilityReduction =
                        enchantments.getAsJsonObject("durability_reduction_per_level");
                if (durabilityReduction != null) {
                    Map<Identifier, Double> reductions = new HashMap<>();
                    for (Map.Entry<String, JsonElement> enchantment : durabilityReduction.entrySet()) {
                        Identifier id = Identifier.tryParse(enchantment.getKey());
                        if (id != null) {
                            reductions.put(id, enchantment.getValue().getAsDouble());
                        }
                    }
                    EquipmentFallbackConfig.setDurabilityReductionEnchantments(reductions);
                }

                EquipmentFallbackConfig.setMaxDurabilityReduction(
                        number(enchantments, "max_durability_reduction", 0.9)
                );
            }

            JsonObject global = json.getAsJsonObject("global");
            if (global != null) {
                EquipmentFallbackConfig.setLowStaminaArmorPanelReduction(
                        number(global, "low_stamina_armor_panel_reduction", 0.10)
                );
                EquipmentFallbackConfig.setExhaustedArmorPanelReduction(
                        number(global, "exhausted_armor_panel_reduction", 0.25)
                );

                // 无法击穿的打击伤害转冲击力比例。
                // blocked strike damage * ratio = extra impact
                EquipmentFallbackConfig.setBlockedStrikeToImpactRatio(
                        number(global, "blocked_strike_to_impact_ratio", 0.10)
                );

                EquipmentFallbackConfig.setImperfectBlockImpactMitigation(
                        number(global, "imperfect_block_impact_mitigation", 0.75)
                );
                EquipmentFallbackConfig.setPerfectBlockImpactMitigation(
                        number(global, "perfect_block_impact_mitigation", 0.95)
                );
                EquipmentFallbackConfig.setPerfectBlockImpact(
                        number(global, "perfect_block_impact", 15.0)
                );
                CombatMovementConfig.setCombatMinDistance(
                        number(global, "combat_min_distance", CombatMovementConfig.DEFAULT_COMBAT_MIN_DISTANCE)
                );
                setRealHitboxShape(global);
            }

            JsonObject dodge = json.getAsJsonObject("dodge");
            if (dodge != null) {
                loadDodgeDamageConfig(dodge);
            }
        }
    }

    private static void loadDefaultWeaponHitbox(JsonObject json, String type) {
        if (json == null) {
            return;
        }

        Vec3d size = vector3(json.get("real_hitbox_size_units"), 0.0, 0.0, 0.0);
        Vec3d offset = vector3(json.get("real_hitbox_offset_units"), 0.0, 0.0, 0.0);
        Vec3d rotation = vector3(
                firstPresent(json, "real_hitbox_rotation_degrees", "real_hitbox_rotation"),
                0.0,
                0.0,
                0.0
        );
        Map<String, String> attackMoves = stringMap(json.getAsJsonObject("attack_moves"));
        Map<String, String> stanceAnimations = stringMap(json.getAsJsonObject("stance_animations"));
        double attackSpeedMultiplier = firstNumber(
                json,
                1.0,
                "attack_speed_multiplier",
                "attack_speed",
                "animation_speed_multiplier",
                "animation_speed"
        );

        switch (type) {
            case "sword" -> {
                EquipmentFallbackConfig.setSwordAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setSwordRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setSwordRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setSwordRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setSwordAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setSwordStanceAnimationNames(stanceAnimations);
            }
            case "longsword" -> {
                EquipmentFallbackConfig.setLongswordAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setLongswordRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setLongswordRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setLongswordRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setLongswordAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setLongswordStanceAnimationNames(stanceAnimations);
            }
            case "polearm" -> {
                EquipmentFallbackConfig.setPolearmAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setPolearmRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setPolearmRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setPolearmRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setPolearmAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setPolearmStanceAnimationNames(stanceAnimations);
            }
            case "pickaxe" -> {
                EquipmentFallbackConfig.setPickaxeAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setPickaxeRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setPickaxeRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setPickaxeRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setPickaxeAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setPickaxeStanceAnimationNames(stanceAnimations);
            }
            case "axe" -> {
                EquipmentFallbackConfig.setAxeAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setAxeRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setAxeRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setAxeRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setAxeAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setAxeStanceAnimationNames(stanceAnimations);
            }
            case "fighting_mace" -> {
                EquipmentFallbackConfig.setFightingMaceAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setFightingMaceRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setFightingMaceRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setFightingMaceRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setFightingMaceAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setFightingMaceStanceAnimationNames(stanceAnimations);
            }
            case "shovel" -> {
                EquipmentFallbackConfig.setShovelAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setShovelRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setShovelRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setShovelRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setShovelAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setShovelStanceAnimationNames(stanceAnimations);
            }
            case "hoe" -> {
                EquipmentFallbackConfig.setHoeAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setHoeRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setHoeRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setHoeRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setHoeAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setHoeStanceAnimationNames(stanceAnimations);
            }
            default -> {
                EquipmentFallbackConfig.setDefaultWeaponAttackSpeedMultiplier(attackSpeedMultiplier);
                EquipmentFallbackConfig.setDefaultWeaponRealHitboxSizeUnits(size);
                EquipmentFallbackConfig.setDefaultWeaponRealHitboxOffsetUnits(offset);
                EquipmentFallbackConfig.setDefaultWeaponRealHitboxRotationDegrees(rotation);
                EquipmentFallbackConfig.setDefaultWeaponAttackMoveIds(attackMoves);
                EquipmentFallbackConfig.setDefaultWeaponStanceAnimationNames(stanceAnimations);
            }
        }
    }

    private static void loadDodgeDamageConfig(JsonObject dodge) {
        DodgeDamageConfig.setDodgeDamageWithAttacker(
                bool(dodge, "damage_with_attacker", true)
        );
        addDodgeDamageTypes(dodge.getAsJsonArray("damage_types"));
        addDodgeDamageTags(dodge.getAsJsonArray("damage_type_tags"));
        addExcludedDodgeDamageTypes(dodge.getAsJsonArray("excluded_damage_types"));
    }

    private static void addDodgeDamageTypes(JsonArray array) {
        if (array == null) {
            return;
        }

        for (JsonElement element : array) {
            DodgeDamageConfig.addDodgeableType(element.getAsString());
        }
    }

    private static void addDodgeDamageTags(JsonArray array) {
        if (array == null) {
            return;
        }

        for (JsonElement element : array) {
            DodgeDamageConfig.addDodgeableTag(element.getAsString());
        }
    }

    private static void addExcludedDodgeDamageTypes(JsonArray array) {
        if (array == null) {
            return;
        }

        for (JsonElement element : array) {
            DodgeDamageConfig.addExcludedType(element.getAsString());
        }
    }

    private static Set<Identifier> identifierSet(JsonArray array) {
        Set<Identifier> ids = new HashSet<>();
        if (array == null) {
            return ids;
        }

        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                continue;
            }

            Identifier id = Identifier.tryParse(element.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }

        return ids;
    }

    private static void setRealHitboxShape(JsonObject global) {
        setRealHitboxSize(global);
        setRealHitboxOffset(global);
        setRealHitboxRotation(global);
    }

    private static void setRealHitboxSize(JsonObject global) {
        if (global == null || !global.has("real_hitbox_size_units")) {
            AnimatedAttackHitboxLibrary.setRealHitboxSizeUnits(1.0, 1.0, 14.0);
            return;
        }

        JsonElement element = global.get("real_hitbox_size_units");
        if (element.isJsonObject()) {
            JsonObject size = element.getAsJsonObject();
            AnimatedAttackHitboxLibrary.setRealHitboxSizeUnits(
                    number(size, "x", 1.0),
                    number(size, "y", 1.0),
                    number(size, "z", 14.0)
            );
            return;
        }

        if (element.isJsonArray() && element.getAsJsonArray().size() >= 3) {
            JsonArray size = element.getAsJsonArray();
            AnimatedAttackHitboxLibrary.setRealHitboxSizeUnits(
                    size.get(0).getAsDouble(),
                    size.get(1).getAsDouble(),
                    size.get(2).getAsDouble()
            );
            return;
        }

        AnimatedAttackHitboxLibrary.setRealHitboxSizeUnits(element.getAsDouble());
    }

    private static void setRealHitboxOffset(JsonObject global) {
        if (global == null || !global.has("real_hitbox_offset_units")) {
            AnimatedAttackHitboxLibrary.setRealHitboxOffsetUnits(0.0, 0.0, 0.0);
            return;
        }

        JsonElement element = global.get("real_hitbox_offset_units");
        if (element.isJsonObject()) {
            JsonObject offset = element.getAsJsonObject();
            AnimatedAttackHitboxLibrary.setRealHitboxOffsetUnits(
                    number(offset, "x", 0.0),
                    number(offset, "y", 0.0),
                    number(offset, "z", 0.0)
            );
            return;
        }

        if (element.isJsonArray() && element.getAsJsonArray().size() >= 3) {
            JsonArray offset = element.getAsJsonArray();
            AnimatedAttackHitboxLibrary.setRealHitboxOffsetUnits(
                    offset.get(0).getAsDouble(),
                    offset.get(1).getAsDouble(),
                    offset.get(2).getAsDouble()
            );
        }
    }

    private static void setRealHitboxRotation(JsonObject global) {
        JsonElement rotationElement = firstPresent(
                global,
                "real_hitbox_rotation_degrees",
                "real_hitbox_rotation"
        );
        if (rotationElement == null) {
            AnimatedAttackHitboxLibrary.setRealHitboxRotationDegrees(0.0, 0.0, 0.0);
            return;
        }

        Vec3d rotation = vector3(rotationElement, 0.0, 0.0, 0.0);
        AnimatedAttackHitboxLibrary.setRealHitboxRotationDegrees(rotation.x, rotation.y, rotation.z);
    }

    private static void putSlotParts(
            Map<EquipmentSlot, List<String>> slotParts,
            JsonObject protectedParts,
            EquipmentSlot slot,
            String key
    ) {
        JsonArray array = protectedParts.getAsJsonArray(key);
        if (array == null) {
            return;
        }

        List<String> parts = new ArrayList<>();
        for (JsonElement element : array) {
            parts.add(element.getAsString());
        }
        slotParts.put(slot, parts);
    }

    private static void loadCombos(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/combos")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            if (json.has("combos")) {
                JsonElement combos = json.get("combos");
                if (combos.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> combo : combos.getAsJsonObject().entrySet()) {
                        registerCombo(combo.getKey(), combo.getValue().getAsJsonObject());
                    }
                } else if (combos.isJsonArray()) {
                    for (JsonElement combo : combos.getAsJsonArray()) {
                        registerCombo(stripJson(entry.getKey()), combo.getAsJsonObject());
                    }
                }
            } else {
                registerCombo(stripJson(entry.getKey()), json);
            }
        }
    }

    private static void loadExecutions(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/executions")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            String fallbackSetId = stripJson(entry.getKey());
            if (json.has("executions") && json.get("executions").isJsonObject()) {
                JsonObject executions = json.getAsJsonObject("executions");
                for (Map.Entry<String, JsonElement> execution : executions.entrySet()) {
                    registerExecution(fallbackSetId, execution.getKey(), execution.getValue().getAsJsonObject());
                }
            } else {
                for (Map.Entry<String, JsonElement> execution : json.entrySet()) {
                    if (execution.getValue().isJsonObject() && directionFromKey(execution.getKey()) != null) {
                        registerExecution(fallbackSetId, execution.getKey(), execution.getValue().getAsJsonObject());
                    }
                }
            }
        }
    }

    private static void registerExecution(String fallbackSetId, String directionKey, JsonObject json) {
        CombatDirection direction = directionFromKey(firstString(json, directionKey, "direction", "stance", "trigger"));
        if (direction == null) {
            return;
        }

        String setId = firstString(json, fallbackSetId, "set", "set_id", "execution_set");
        String id = string(json, "id", setId + "_" + direction.name().toLowerCase());
        String animation = string(json, "animation", id);
        ExecutionMoveConfigs.register(
                setId,
                direction,
                new ComboMoveConfig(
                        id,
                        string(json, "display_name", id),
                        List.of(direction),
                        damageProfile(json.getAsJsonObject("damage_modifiers"), 1.0),
                        animation,
                        hitZoneRules(json.getAsJsonArray("hit_zones")),
                        impactMultiplier(json, 20.0),
                        number(json, "stamina_cost", 0.0),
                        number(json, "horizontal_knockback", CombatControlConfig.DEFAULT_ATTACK_HORIZONTAL_KNOCKBACK),
                        bool(json, "blade_trail", true),
                        bool(json, "hit_reaction", false),
                        true,
                        bool(json, "lunge", false),
                        bool(json, "use_real_hitbox", false),
                        string(json, "injury_type", ""),
                        (int) number(json, "injury_level", 0.0),
                        string(json, "required_weapon_tag", ""),
                        string(json, "hit_reaction_animation", ""),
                        bool(json, "hit_reaction_interrupts_attack", false),
                        integer(json, "hit_reaction_movement_lock_ticks", 0),
                        integer(json, "weapon_clash_tick", -1),
                        string(json, "weapon_clash_sound", ""),
                        firstNumber(json, 2.8, "suction_distance", "absorb_distance"),
                        firstNumber(json, 0.0, "suction_min_distance", "required_suction_distance"),
                        firstNumber(json, 1.15, "suction_fixed_distance", "suction_after_distance", "fixed_distance"),
                        firstString(json, "", "victim_animation", "target_animation", "hit_animation"),
                        comboAttackChain(json.getAsJsonArray("attack_chain")),
                        integer(json, "level", integer(json, "required_ai_level", integer(json, "combo_level", 0)))
                )
        );
    }

    private static void loadExecutionTargets(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/execution_targets")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            addExecutionTargetIds(json.getAsJsonArray("entity_types"));
            addExecutionTargetIds(json.getAsJsonArray("entities"));
            addExecutionTargetIds(json.getAsJsonArray("values"));
        }
    }

    private static void addExecutionTargetIds(JsonArray array) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                continue;
            }
            Identifier id = Identifier.tryParse(element.getAsString());
            if (id != null && Registries.ENTITY_TYPE.containsId(id)) {
                ExecutionTargetConfig.add(id);
            }
        }
    }

    private static CombatDirection directionFromKey(String key) {
        if (key == null) {
            return null;
        }
        return switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "left", "左", "left_down", "leftdown", "ld" -> CombatDirection.LEFT;
            case "right", "右", "right_up", "rightup", "ru" -> CombatDirection.RIGHT;
            case "up", "上", "left_up", "leftup", "lu" -> CombatDirection.UP;
            case "down", "下", "right_down", "rightdown", "rd" -> CombatDirection.DOWN;
            default -> null;
        };
    }

    private static void registerCombo(String fallbackId, JsonObject json) {
        String sequence = string(json, "sequence", "");
        if (sequence.isBlank()) {
            return;
        }

        String id = string(json, "id", fallbackId);
        String animation = string(json, "animation", id);
        ComboMoveConfigs.register(
                sequence,
                new ComboMoveConfig(
                        id,
                        string(json, "display_name", id),
                        ComboMoveConfigs.parseSequence(sequence),
                        damageProfile(json.getAsJsonObject("damage_modifiers"), 1.0),
                        animation,
                        hitZoneRules(json.getAsJsonArray("hit_zones")),
                        impactMultiplier(json, 20.0),
                        number(json, "stamina_cost", 8.0),
                        number(json, "horizontal_knockback", CombatControlConfig.DEFAULT_ATTACK_HORIZONTAL_KNOCKBACK),
                        bool(json, "blade_trail", false),
                        bool(json, "hit_reaction", true),
                        bool(json, "suction", false),
                        bool(json, "lunge", true),
                        bool(json, "use_real_hitbox", false),
                        string(json, "injury_type", ""),
                        (int) number(json, "injury_level", 0.0),
                        string(json, "required_weapon_tag", ""),
                        string(json, "hit_reaction_animation", ""),
                        bool(json, "hit_reaction_interrupts_attack", false),
                        integer(json, "hit_reaction_movement_lock_ticks", 0),
                        integer(json, "weapon_clash_tick", -1),
                        string(json, "weapon_clash_sound", ""),
                        firstNumber(json, 0.0, "suction_distance", "absorb_distance"),
                        firstNumber(json, 0.0, "suction_min_distance", "required_suction_distance"),
                        firstNumber(json, 1.15, "suction_fixed_distance", "suction_after_distance", "fixed_distance"),
                        firstString(json, "", "victim_animation", "target_animation", "hit_animation"),
                        comboAttackChain(json.getAsJsonArray("attack_chain")),
                        integer(json, "level", integer(json, "required_ai_level", integer(json, "combo_level", 0)))
                )
        );
    }

    private static List<ComboMoveConfig.AttackChainEvent> comboAttackChain(JsonArray array) {
        List<ComboMoveConfig.AttackChainEvent> events = new ArrayList<>();
        if (array == null) {
            return events;
        }

        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject event = element.getAsJsonObject();
            boolean weaponHit = bool(event, "hit_weapon", false)
                    || bool(event, "weapon_hit", false)
                    || "weapon".equalsIgnoreCase(string(event, "target", ""));
            events.add(new ComboMoveConfig.AttackChainEvent(
                    integer(event, "tick", 0),
                    hitZoneRules(event.getAsJsonArray("hit_zones")),
                    damageProfile(event.getAsJsonObject("damage_modifiers"), 1.0),
                    weaponHit,
                    string(event, "weapon_clash_sound", "")
            ));
        }

        return events;
    }

    private static void loadMoves(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/moves")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            if (json.has("moves")) {
                JsonObject moves = json.getAsJsonObject("moves");
                for (Map.Entry<String, JsonElement> move : moves.entrySet()) {
                    registerMove(move.getKey(), move.getValue().getAsJsonObject());
                }
            }
            if (json.has("named_moves")) {
                JsonObject moves = json.getAsJsonObject("named_moves");
                for (Map.Entry<String, JsonElement> move : moves.entrySet()) {
                    registerNamedMove(move.getKey(), move.getValue().getAsJsonObject());
                }
            } else {
                String direction = string(json, "direction", "");
                if (!direction.isBlank()) {
                    registerMove(direction, json);
                }
            }
        }
    }

    private static void registerMove(String directionText, JsonObject json) {
        CombatDirection direction = direction(directionText);
        if (direction == null) {
            return;
        }

        DamageTypeProfile modifiers = damageProfile(
                json.getAsJsonObject("damage_modifiers"),
                1.0
        );
        String id = string(json, "id", direction.name().toLowerCase());
        AttackMoveConfigs.register(
                direction,
                new AttackMoveConfig(
                        id,
                        modifiers.strike(),
                        modifiers.slash(),
                        modifiers.thrust(),
                        hitZoneRules(json.getAsJsonArray("hit_zones")),
                        impactMultiplier(json, 20.0),
                        number(json, "stamina_cost", 6.0),
                        bool(json, "use_real_hitbox", false),
                        string(json, "animation", ""),
                        integer(json, "transition_ticks", CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS),
                        integer(json, "direct_hit_tick", -1),
                        integer(json, "weapon_clash_tick", -1),
                        string(json, "weapon_clash_sound", ""),
                        number(json, "master_counter_spacing", 1.4),
                        number(json, "horizontal_knockback", CombatControlConfig.DEFAULT_ATTACK_HORIZONTAL_KNOCKBACK),
                        bool(json, "hit_reaction", true),
                        heightPartRules(json.getAsJsonArray("direct_hit_height_parts")),
                        moveDefenseDirection(json),
                        bool(json, "classic_directional_block", false),
                        bool(json, "blockable", true),
                        bool(json, "dodgeable", true),
                        bool(json, "jump_dodge_legs", false)
                )
        );
    }

    private static void registerNamedMove(String fallbackId, JsonObject json) {
        String id = string(json, "id", fallbackId);
        DamageTypeProfile modifiers = damageProfile(
                json.getAsJsonObject("damage_modifiers"),
                1.0
        );
        AttackMoveConfigs.registerNamed(
                id,
                new AttackMoveConfig(
                        id,
                        modifiers.strike(),
                        modifiers.slash(),
                        modifiers.thrust(),
                        hitZoneRules(json.getAsJsonArray("hit_zones")),
                        impactMultiplier(json, 12.0),
                        number(json, "stamina_cost", 4.0),
                        bool(json, "use_real_hitbox", false),
                        string(json, "animation", id),
                        integer(json, "transition_ticks", defaultNamedMoveTransitionTicks(id, json)),
                        integer(json, "direct_hit_tick", -1),
                        integer(json, "weapon_clash_tick", -1),
                        string(json, "weapon_clash_sound", ""),
                        number(json, "master_counter_spacing", 1.4),
                        number(json, "horizontal_knockback", CombatControlConfig.DEFAULT_ATTACK_HORIZONTAL_KNOCKBACK),
                        bool(json, "hit_reaction", true),
                        heightPartRules(json.getAsJsonArray("direct_hit_height_parts")),
                        moveDefenseDirection(json),
                        bool(json, "classic_directional_block", false),
                        bool(json, "blockable", true),
                        bool(json, "dodgeable", true),
                        bool(json, "jump_dodge_legs", false)
                )
        );
    }

    private static int defaultNamedMoveTransitionTicks(String id, JsonObject json) {
        String animation = string(json, "animation", id);
        return isHeavyMove(id) || isHeavyMove(animation)
                ? CombatTiming.HEAVY_ATTACK_TRANSITION_TICKS
                : CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS;
    }

    private static boolean isHeavyMove(String id) {
        return CombatAnimationNames.isHeavyAttack(id);
    }

    private static CombatDirection moveDefenseDirection(JsonObject json) {
        String value = firstString(json, "", "defense_direction", "block_direction", "direction_to_block");
        return value.isBlank() ? null : direction(value);
    }

    private static CombatDirection direction(String text) {
        return switch (text.toLowerCase()) {
            case "left", "左" -> CombatDirection.LEFT;
            case "right", "右" -> CombatDirection.RIGHT;
            case "up", "上" -> CombatDirection.UP;
            case "down", "下" -> CombatDirection.DOWN;
            default -> null;
        };
    }

    private static void loadWeapons(ResourceManager manager) {
        List<WeaponDefinition> definitions = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/weapons")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            JsonObject items = json.getAsJsonObject("items");
            if (items != null) {
                for (Map.Entry<String, JsonElement> item : items.entrySet()) {
                    definitions.add(new WeaponDefinition(
                            item.getKey(), item.getValue().getAsJsonObject()));
                }
            } else {
                String selector = string(json, "item", "");
                if (selector.isBlank()) {
                    String tag = string(json, "tag", "");
                    selector = tag.isBlank() ? "" : (tag.startsWith("#") ? tag : "#" + tag);
                }
                definitions.add(new WeaponDefinition(selector, json));
            }
        }

        cachedWeaponDefinitions = List.copyOf(definitions);
        applyWeaponDefinitions(definitions);
    }

    private static void applyWeaponDefinitions(List<WeaponDefinition> definitions) {
        // Resolve broad fallbacks before exact entries so every omitted field
        // inherits deterministically: global defaults -> first matching tag -> item id.
        definitions.stream().filter(WeaponDefinition::isTag)
                .forEach(definition -> registerWeaponSelector(definition.selector(), definition.json()));
        definitions.stream().filter(definition -> !definition.isTag())
                .forEach(definition -> registerWeaponSelector(definition.selector(), definition.json()));
    }

    /**
     * Initial server-data parsing can run before the new item-tag bindings are
     * installed. Re-expand the cached selectors from the now-bound registries
     * before players receive the data-driven weapon snapshot.
     */
    public static void reapplyWeaponDefinitionsAfterTagsBound() {
        List<WeaponDefinition> definitions = cachedWeaponDefinitions;
        if (definitions.isEmpty()) {
            return;
        }
        EquipmentCombatAttributesRegistry.clearWeapons();
        applyWeaponDefinitions(definitions);
    }

    private static void loadRangedWeapons(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/ranged_weapons")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            JsonObject items = json.getAsJsonObject("items");
            if (items != null) {
                for (Map.Entry<String, JsonElement> item : items.entrySet()) {
                    registerRangedWeapon(item.getKey(), item.getValue().getAsJsonObject());
                }
            } else {
                registerRangedWeapon(string(json, "item", ""), json);
            }
        }
    }

    private static void registerRangedWeapon(String itemName, JsonObject json) {
        Identifier itemId = Identifier.tryParse(itemName);
        if (itemId == null || Registries.ITEM.get(itemId) == null) {
            return;
        }

        RangedWeaponAttributesRegistry.register(
                itemId,
                new RangedWeaponAttributes(
                        firstNumber(json, 1.0, "speed", "draw_speed"),
                        firstNumber(json, 1.0, "hardness", "stamina_per_second"),
                        firstNumber(json, 1.0, "power", "projectile_speed")
                )
        );
    }

    private static void registerWeapon(String itemName, JsonObject json) {
        Identifier itemId = Identifier.tryParse(itemName);
        if (itemId == null) {
            return;
        }

        Item item = Registries.ITEM.get(itemId);
        if (item == null) {
            return;
        }

        WeaponCombatAttributes defaults = EquipmentCombatAttributesRegistry.getWeapon(item.getDefaultStack());
        WeaponCombatAttributes attributes = weaponAttributes(json, defaults);
        EquipmentCombatAttributesRegistry.registerWeapon(itemId, attributes);
        JsonArray aliases = json.getAsJsonArray("aliases");
        if (aliases != null) {
            for (JsonElement alias : aliases) {
                registerWeaponAlias(alias.getAsString(), attributes);
            }
        }
    }

    private static void registerWeaponSelector(String selector, JsonObject json) {
        if (selector != null && selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId == null) return;
            TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, tagId);
            // Materialize the tag per item. A polearm, longsword, axe, etc. has
            // a different category fallback, so one attributes object built
            // from ItemStack.EMPTY cannot provide field-by-field inheritance.
            // Exact item definitions are loaded in the second pass and replace
            // these expanded entries, preserving default -> tag -> item id.
            int matchedItems = 0;
            for (var itemEntry : Registries.ITEM.iterateEntries(tag)) {
                Item item = itemEntry.value();
                Identifier itemId = Registries.ITEM.getId(item);
                WeaponCombatAttributes inherited =
                        EquipmentCombatAttributesRegistry.getWeapon(item.getDefaultStack());
                EquipmentCombatAttributesRegistry.registerWeapon(
                        itemId, weaponAttributes(json, inherited));
                matchedItems++;
            }
            KingdomComeCombat.LOGGER.info("Expanded weapon datapack tag #{} to {} items", tagId, matchedItems);
            return;
        }
        registerWeapon(selector, json);
    }

    private record WeaponDefinition(String selector, JsonObject json) {
        private boolean isTag() {
            return selector != null && selector.startsWith("#");
        }
    }

    private static void registerWeaponAlias(String selector, WeaponCombatAttributes attributes) {
        if (selector != null && selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId != null) {
                EquipmentCombatAttributesRegistry.registerWeapon(
                        TagKey.of(RegistryKeys.ITEM, tagId), attributes);
            }
            return;
        }
        Identifier aliasId = Identifier.tryParse(selector);
        if (aliasId != null) EquipmentCombatAttributesRegistry.registerWeapon(aliasId, attributes);
    }

    private static WeaponCombatAttributes weaponAttributes(
            JsonObject json,
            WeaponCombatAttributes defaults
    ) {
        JsonObject damagePanel = json.getAsJsonObject("damage_panel");

        return new WeaponCombatAttributes(
                        damagePanel == null ? defaults.damagePanel() : new DamageTypeProfile(
                                number(damagePanel, "thrust", defaults.damagePanel().thrust()),
                                number(damagePanel, "strike", defaults.damagePanel().strike()),
                                number(damagePanel, "slash", defaults.damagePanel().slash())
                        ),
                        firstNumber(json, defaults.blockImpactMitigation(), "block_impact_mitigation"),
                        firstNumber(json, defaults.baseImpact(), "base_impact", "impact"),
                        firstNumber(json, defaults.armorBreakMultiplier(), "armor_break_multiplier", "armor_break_coefficient", "armor_durability_multiplier"),
                        firstNumber(
                                json,
                                defaults.attackSpeedMultiplier(),
                                "attack_speed_multiplier",
                                "attack_speed",
                                "animation_speed_multiplier",
                                "animation_speed"
                        ),
                        json.has("real_hitbox_size_units")
                                ? vector3(json.get("real_hitbox_size_units"),
                                        defaults.realHitboxSizeUnits().x,
                                        defaults.realHitboxSizeUnits().y,
                                        defaults.realHitboxSizeUnits().z)
                                : defaults.realHitboxSizeUnits(),
                        json.has("real_hitbox_offset_units")
                                ? vector3(json.get("real_hitbox_offset_units"),
                                        defaults.realHitboxOffsetUnits().x,
                                        defaults.realHitboxOffsetUnits().y,
                                        defaults.realHitboxOffsetUnits().z)
                                : defaults.realHitboxOffsetUnits(),
                        firstPresent(json, "real_hitbox_rotation_degrees", "real_hitbox_rotation") != null
                                ? vector3(firstPresent(json, "real_hitbox_rotation_degrees", "real_hitbox_rotation"),
                                        defaults.realHitboxRotationDegrees().x,
                                        defaults.realHitboxRotationDegrees().y,
                                        defaults.realHitboxRotationDegrees().z)
                                : defaults.realHitboxRotationDegrees(),
                        json.has("attack_moves")
                                ? mergeStringMap(defaults.attackMoveIds(), json.getAsJsonObject("attack_moves"))
                                : defaults.attackMoveIds(),
                        json.has("stance_animations")
                                ? mergeStringMap(defaults.stanceAnimationNames(), json.getAsJsonObject("stance_animations"))
                                : defaults.stanceAnimationNames(),
                        json.has("executions")
                                ? mergeStringMap(defaults.executionMoveIds(), json.getAsJsonObject("executions"))
                                : json.has("execution_moves")
                                ? mergeStringMap(defaults.executionMoveIds(), json.getAsJsonObject("execution_moves"))
                                : defaults.executionMoveIds(),
                        firstNumber(json, defaults.weaponToughness(), "weapon_toughness"),
                        firstNumber(
                                json,
                                defaults.minimumDurabilityPanelMultiplier(),
                                "minimum_durability_panel_multiplier"
                        ),
                        firstNumber(
                                json,
                                defaults.heldMovementSpeedMultiplier(),
                                "held_movement_speed_multiplier"
                        )
                );
    }

    private static void loadArmor(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/armor")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            JsonObject items = json.getAsJsonObject("items");
            if (items != null) {
                for (Map.Entry<String, JsonElement> item : items.entrySet()) {
                    registerArmor(item.getKey(), item.getValue().getAsJsonObject());
                }
            } else {
                registerArmor(string(json, "item", ""), json);
            }
        }
    }

    private static void loadShields(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/shields")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            JsonObject items = json.getAsJsonObject("items");
            if (items != null) {
                for (Map.Entry<String, JsonElement> item : items.entrySet()) {
                    registerShield(item.getKey(), item.getValue().getAsJsonObject());
                }
            } else {
                registerShield(string(json, "item", ""), json);
            }
        }
    }

    private static void registerShield(String itemName, JsonObject json) {
        Identifier itemId = Identifier.tryParse(itemName);
        if (itemId == null) {
            return;
        }

        Item item = Registries.ITEM.get(itemId);
        if (item == null) {
            return;
        }

        EquipmentCombatAttributesRegistry.registerShield(
                itemId,
                new ShieldCombatAttributes(
                        shieldSize(firstString(json, "small", "type", "size", "shield_type")),
                        number(json, "block_impact_mitigation", EquipmentFallbackConfig.imperfectBlockImpactMitigation()),
                        number(json, "attack_stamina_cost_multiplier", 1.0),
                        number(json, "dodge_stamina_cost_multiplier", 1.0),
                        number(json, "locked_movement_speed_multiplier", 1.0),
                        number(json, "attack_speed_multiplier", 1.0),
                        number(json, "attack_lunge_multiplier", 1.0),
                        integer(json, "exhausted_disable_ticks", 0),
                        number(json, "blocking_angle_multiplier", 1.0),
                        number(json, "perfect_window_multiplier", 1.0),
                        integer(json, "perfect_attacker_disable_bonus_ticks", 0)
                )
        );
    }

    private static void loadProjectiles(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/projectiles")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            loadAutomaticHumanoidAiCompatibility(json.getAsJsonObject("automatic_compatibility"));
            JsonObject entities = json.getAsJsonObject("entities");
            if (entities != null) {
                for (Map.Entry<String, JsonElement> projectile : entities.entrySet()) {
                    registerProjectile(projectile.getKey(), projectile.getValue().getAsJsonObject());
                }
            } else {
                registerProjectile(string(json, "entity", ""), json);
            }
        }
    }

    private static void loadAutomaticHumanoidAiCompatibility(JsonObject automatic) {
        if (automatic == null) {
            return;
        }

        JsonObject bands = automatic.getAsJsonObject("health_bands");
        if (bands != null) {
            registerAutomaticHealthBand(bands, "under_10",
                    HumanoidCombatAiProfiles.AutoHealthBand.UNDER_10);
            registerAutomaticHealthBand(bands, "10_to_20",
                    HumanoidCombatAiProfiles.AutoHealthBand.FROM_10_TO_20);
            registerAutomaticHealthBand(bands, "20_to_30",
                    HumanoidCombatAiProfiles.AutoHealthBand.FROM_20_TO_30);
            registerAutomaticHealthBand(bands, "30_to_40",
                    HumanoidCombatAiProfiles.AutoHealthBand.FROM_30_TO_40);
            registerAutomaticHealthBand(bands, "over_40",
                    HumanoidCombatAiProfiles.AutoHealthBand.OVER_40);
        }

        readAutomaticEntityTypes(automatic.getAsJsonArray("include_entities"), true);
        readAutomaticEntityTypes(automatic.getAsJsonArray("exclude_entities"), false);
    }

    private static void registerAutomaticHealthBand(
            JsonObject bands,
            String key,
            HumanoidCombatAiProfiles.AutoHealthBand band
    ) {
        JsonObject profile = bands.getAsJsonObject(key);
        if (profile != null) {
            HumanoidCombatAiProfiles.setAutomaticProfileSpec(
                    band,
                    HumanoidCombatAiProfileSpec.fromJson(
                            profile,
                            HumanoidCombatAiProfiles.defaultHumanoid()
                    )
            );
        }
    }

    private static void readAutomaticEntityTypes(JsonArray values, boolean include) {
        if (values == null) {
            return;
        }
        for (JsonElement value : values) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                continue;
            }
            Identifier id = Identifier.tryParse(value.getAsString());
            if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
                continue;
            }
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            if (include) {
                HumanoidCombatAiProfiles.includeAutomaticType(type);
            } else {
                HumanoidCombatAiProfiles.excludeAutomaticType(type);
            }
        }
    }

    private static void loadMobAttributes(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/mobs")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            JsonObject entities = json.getAsJsonObject("entities");
            if (entities != null) {
                for (Map.Entry<String, JsonElement> entity : entities.entrySet()) {
                    registerMobAttributes(entity.getKey(), entity.getValue().getAsJsonObject());
                }
            } else {
                registerMobAttributes(string(json, "entity", ""), json);
            }
        }
    }

    private static void registerMobAttributes(String entityName, JsonObject json) {
        Identifier entityId = Identifier.tryParse(entityName);
        if (entityId == null) {
            return;
        }
        if ("minecraft:ender_dragon".equals(entityName)
                && !com.kingdomcomecombat.config.CombatServerConfig.enderDragonOverhaulEnabled()) {
            return;
        }
        MobScaleRegistry.register(entityId, number(json, "scale", 1.0));

        JsonObject meleeAttack = json.getAsJsonObject("melee_attack");
        MobCombatAttributes.NaturalArmor fallbackArmor = naturalArmor(json, json);
        JsonObject armor = json.getAsJsonObject("armor");
        MobCombatAttributesRegistry.register(
                entityId,
                new MobCombatAttributes(
                        naturalArmor(armor == null ? null : armor.getAsJsonObject("head"), json, fallbackArmor),
                        naturalArmor(armor == null ? null : armor.getAsJsonObject("body"), json, fallbackArmor),
                        firstNumber(json, 0.0, "health", "max_health"),
                        firstNumber(json, 0.0, "stamina", "max_stamina", "stamina_max"),
                        firstNumber(json, 0.0, "stamina_regen_per_tick", "stamina_regen", "regen_per_tick"),
                        meleeAttack == null
                                ? number(json, "melee_impact", 0.0)
                                : number(meleeAttack, "impact", number(json, "melee_impact", 0.0)),
                        meleeAttack == null
                                ? number(json, "melee_knockback", 0.4)
                                : number(meleeAttack, "knockback", number(json, "melee_knockback", 0.4)),
                        meleeAttack == null
                                ? number(json, "melee_vertical_knockback", 0.0)
                                : firstNumber(meleeAttack, number(json, "melee_vertical_knockback", 0.0), "vertical_knockback", "launch"),
                        meleeDefenseTier(json, meleeAttack),
                        meleeAttack == null ? number(json, "true_stamina_damage", 0.0)
                                : number(meleeAttack, "true_stamina_damage", number(json, "true_stamina_damage", 0.0)),
                        meleeAttack == null ? number(json, "blocked_attacker_knockback", 0.0)
                                : number(meleeAttack, "blocked_attacker_knockback", number(json, "blocked_attacker_knockback", 0.0)),
                        mobAttackBehavior(meleeAttack, meleeDefenseTier(json, meleeAttack)),
                        damageProfile(
                                meleeAttack == null
                                        ? json.getAsJsonObject("melee_damage_modifiers")
                                        : meleeAttack.getAsJsonObject("damage_modifiers"),
                                0.0
                        ),
                        meleeAttack == null
                                ? hitZoneRules(json.getAsJsonArray("melee_hit_zones"))
                                : hitZoneRules(meleeAttack.getAsJsonArray("hit_zones"))
                )
        );
    }

    private static MobCombatAttributes.AttackBehavior mobAttackBehavior(
            JsonObject meleeAttack, MobCombatAttributes.MeleeDefenseTier fallbackDefenseTier) {
        if (meleeAttack == null) {
            return MobCombatAttributes.AttackBehavior.vanilla();
        }
        String raw = firstString(meleeAttack, "vanilla", "attack_type", "mode", "movement");
        MobCombatAttributes.AttackBehavior.Mode mode = switch (raw.toLowerCase()) {
            case "lunge", "rush", "突进" -> MobCombatAttributes.AttackBehavior.Mode.LUNGE;
            case "jump", "leap", "跳跃" -> MobCombatAttributes.AttackBehavior.Mode.JUMP;
            case "mixed", "lunge_or_jump", "rush_or_leap", "混合" -> MobCombatAttributes.AttackBehavior.Mode.MIXED;
            default -> MobCombatAttributes.AttackBehavior.Mode.VANILLA;
        };
        return new MobCombatAttributes.AttackBehavior(
                mode,
                integer(meleeAttack, "windup_ticks", 0),
                integer(meleeAttack, "cooldown_ticks", 20),
                integer(meleeAttack, "active_ticks", 12),
                number(meleeAttack, "start_distance", 4.0),
                number(meleeAttack, "movement_speed", mode == MobCombatAttributes.AttackBehavior.Mode.JUMP ? 0.42 : 0.55),
                number(meleeAttack, "jump_velocity", 0.42),
                bool(meleeAttack, "airborne_lunge", false),
                defenseTierValue(firstString(meleeAttack, "", "lunge_defense_tier"), fallbackDefenseTier),
                defenseTierValue(firstString(meleeAttack, "", "jump_defense_tier"), fallbackDefenseTier)
        );
    }

    private static MobCombatAttributes.MeleeDefenseTier defenseTierValue(
            String value, MobCombatAttributes.MeleeDefenseTier fallback) {
        if (value == null || value.isBlank()) return fallback;
        return switch (value.toLowerCase()) {
            case "1", "block", "blockable", "weapon", "weapon_block", "normal" ->
                    MobCombatAttributes.MeleeDefenseTier.BLOCKABLE;
            case "2", "perfect", "perfect_block", "perfect_block_only" ->
                    MobCombatAttributes.MeleeDefenseTier.PERFECT_BLOCK_ONLY;
            case "3", "shield", "shield_block", "shield_blockable" ->
                    MobCombatAttributes.MeleeDefenseTier.SHIELD_BLOCKABLE;
            case "4", "shield_perfect", "shield_perfect_block", "shield_perfect_block_only" ->
                    MobCombatAttributes.MeleeDefenseTier.SHIELD_PERFECT_BLOCK_ONLY;
            case "5", "none", "unblockable", "dodge", "dodgeable", "evade", "evasion" ->
                    MobCombatAttributes.MeleeDefenseTier.UNBLOCKABLE;
            default -> fallback;
        };
    }

    private static MobCombatAttributes.MeleeDefenseTier meleeDefenseTier(
            JsonObject root,
            JsonObject meleeAttack
    ) {
        JsonObject source = meleeAttack == null ? root : meleeAttack;
        String value = firstString(source, "", "defense_tier", "defense", "defense_mode");
        if (!value.isBlank()) {
            return switch (value.toLowerCase()) {
                case "1", "block", "blockable", "weapon", "weapon_block", "normal" ->
                        MobCombatAttributes.MeleeDefenseTier.BLOCKABLE;
                case "2", "perfect", "perfect_block", "perfect_block_only" ->
                        MobCombatAttributes.MeleeDefenseTier.PERFECT_BLOCK_ONLY;
                case "3", "shield", "shield_block", "shield_blockable" ->
                        MobCombatAttributes.MeleeDefenseTier.SHIELD_BLOCKABLE;
                case "4", "shield_perfect", "shield_perfect_block", "shield_perfect_block_only" ->
                        MobCombatAttributes.MeleeDefenseTier.SHIELD_PERFECT_BLOCK_ONLY;
                case "5", "none", "unblockable", "dodge", "dodgeable", "evade", "evasion" ->
                        MobCombatAttributes.MeleeDefenseTier.UNBLOCKABLE;
                default -> MobCombatAttributes.MeleeDefenseTier.BLOCKABLE;
            };
        }

        if (source.has("tier")) {
            int tier = integer(source, "tier", 1);
            return switch (Math.max(1, Math.min(5, tier))) {
                case 2 -> MobCombatAttributes.MeleeDefenseTier.PERFECT_BLOCK_ONLY;
                case 3 -> MobCombatAttributes.MeleeDefenseTier.SHIELD_BLOCKABLE;
                case 4 -> MobCombatAttributes.MeleeDefenseTier.SHIELD_PERFECT_BLOCK_ONLY;
                case 5 -> MobCombatAttributes.MeleeDefenseTier.UNBLOCKABLE;
                default -> MobCombatAttributes.MeleeDefenseTier.BLOCKABLE;
            };
        }

        boolean blockable = meleeAttack == null
                ? bool(root, "melee_blockable", true)
                : bool(meleeAttack, "blockable", bool(root, "melee_blockable", true));
        return blockable
                ? MobCombatAttributes.MeleeDefenseTier.BLOCKABLE
                : MobCombatAttributes.MeleeDefenseTier.UNBLOCKABLE;
    }

    private static MobCombatAttributes.NaturalArmor naturalArmor(
            JsonObject armor,
            JsonObject fallbackRoot
    ) {
        return new MobCombatAttributes.NaturalArmor(
                damageProfile(armor == null ? null : armor.getAsJsonObject("defense"), 0.0),
                firstNumber(armor, 0.0, "impact_mitigation", "impact_reduction"),
                integer(armor, "durability", integer(armor, "armor_durability", 0))
        );
    }

    private static MobCombatAttributes.NaturalArmor naturalArmor(
            JsonObject armor,
            JsonObject fallbackRoot,
            MobCombatAttributes.NaturalArmor fallback
    ) {
        if (armor == null) {
            return fallback;
        }

        JsonObject defense = armor.getAsJsonObject("defense");
        return new MobCombatAttributes.NaturalArmor(
                defense == null ? fallback.defense() : damageProfile(defense, 0.0),
                firstNumber(armor, fallback.impactMitigation(), "impact_mitigation", "impact_reduction"),
                integer(armor, "durability", integer(armor, "armor_durability", fallback.durability()))
        );
    }

    private static void loadAiProfiles(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/ai")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            JsonObject entities = json.getAsJsonObject("entities");
            if (entities == null) {
                continue;
            }

            for (Map.Entry<String, JsonElement> entity : entities.entrySet()) {
                Identifier entityId = Identifier.tryParse(entity.getKey());
                if (entityId == null) {
                    continue;
                }

                if (!Registries.ENTITY_TYPE.containsId(entityId)) {
                    continue;
                }
                var entityType = Registries.ENTITY_TYPE.get(entityId);

                JsonObject profile = entity.getValue().getAsJsonObject();
                HumanoidCombatAiProfiles.setProfileSpec(
                        entityType,
                        HumanoidCombatAiProfileSpec.fromJson(
                                profile,
                                HumanoidCombatAiProfiles.baseProfileFor(entityType)
                        )
                );
                JsonObject leaderProfile = firstObject(profile, "zombie_leader", "leader", "boss");
                if (entityType == EntityType.ZOMBIE && leaderProfile != null) {
                    HumanoidCombatAiProfiles.setZombieLeaderProfileSpec(
                            HumanoidCombatAiProfileSpec.fromJson(
                                    leaderProfile,
                                    HumanoidCombatAiProfile.zombieLeaderDefault()
                            )
                    );
                }
            }
        }
    }

    private static void loadBeastAiProfiles(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> entry : findJsonResources(
                manager, "kingdom_come_combat/beast_ai")) {
            JsonObject json = readJson(entry.getKey(), entry.getValue());
            JsonObject entities = json.getAsJsonObject("entities");
            if (entities == null) {
                continue;
            }

            for (Map.Entry<String, JsonElement> entity : entities.entrySet()) {
                Identifier entityId = Identifier.tryParse(entity.getKey());
                if (entityId == null || !Registries.ENTITY_TYPE.containsId(entityId)) {
                    continue;
                }

                BeastCombatAiProfiles.register(entityId, beastAiProfile(entity.getValue().getAsJsonObject()));
                if (hasInlineMobAttributes(entity.getValue().getAsJsonObject())) {
                    registerMobAttributes(entity.getKey(), entity.getValue().getAsJsonObject());
                }
            }
        }
    }

    private static boolean hasInlineMobAttributes(JsonObject json) {
        return json != null
                && (json.has("health")
                || json.has("max_health")
                || json.has("stamina")
                || json.has("max_stamina")
                || json.has("stamina_regen_per_tick")
                || json.has("melee_attack")
                || json.has("armor"));
    }

    private static HumanoidCombatAiProfile aiProfile(JsonObject profile) {
        return new HumanoidCombatAiProfile(
                (int) rangedNumber(profile, "min_attack_interval_ticks", 18.0),
                rangedNumber(profile, "attack_desire_per_half_second", 0.25),
                rangedNumber(profile, "block_chance", 0.25),
                rangedNumber(profile, "perfect_block_chance", 0.15),
                (int) rangedNumber(profile, "combo_level", 0.0),
                rangedNumber(profile, "combo_plan_chance", 0.0),
                rangedNumber(profile, "dodge_chance", 0.05),
                (int) rangedNumber(profile, "ai_level", 1.0),
                rangedNumber(profile, "stamina_max", 100.0),
                rangedNumber(profile, "stamina_regen_per_tick", 0.3),
                rangedNumber(profile, "attack_animation_speed", 0.6),
                rangedNumber(profile, "attack_startup_slowdown", 0.0),
                rangedNumber(profile, "toughness", 3.0),
                rangedNumber(profile, "worn_armor_durability_multiplier", 1.0),
                rangedNumber(profile, "follow_up_attack_chance", 0.70),
                (int) rangedNumber(profile, "max_follow_up_attacks", 3.0),
                bool(profile, "keep_distance", true),
                bool(profile, "requires_weapon", false),
                rangedNumber(profile, "combat_enter_distance", 6.0),
                rangedNumber(profile, "attack_distance", 3.25),
                CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                rangedNumber(profile, "approach_distance", 3.0),
                rangedNumber(profile, "exhausted_retreat_distance", 5.0)
        );
    }

    private static BeastCombatAiProfile beastAiProfile(JsonObject profile) {
        BeastCombatAiProfile.Mode mode = beastMode(firstString(profile, "circle_lunge", "mode", "behavior", "behavior_mode"));
        JsonObject circle = firstObject(profile, "circle", "circling", "circle_lunge");
        JsonObject hold = firstObject(profile, "hold_and_rush", "rush", "charge");
        JsonObject attack = firstObject(profile, "attack", "custom_attack", "melee");
        JsonObject source = mode == BeastCombatAiProfile.Mode.CIRCLE_LUNGE
                ? mergeFallback(profile, circle)
                : mergeFallback(profile, hold);
        JsonObject attackSource = mergeFallback(source, attack);

        return new BeastCombatAiProfile(
                mode,
                rangedNumber(profile, "combat_enter_distance", 7.0),
                rangedNumber(attackSource, "attack_distance", 2.3),
                (int) firstRangedNumber(attackSource, 14.0, "attack_interval_ticks", "attack_cooldown_ticks", "min_attack_interval_ticks"),
                rangedNumber(attackSource, "attack_animation_speed", 1.0),
                firstString(attackSource, "", "attack_move", "attack_move_id", "move", "move_id"),
                firstString(attackSource, "", "attack_animation", "attack_animation_name", "animation", "animation_name"),
                Math.max(
                        CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE,
                        rangedNumber(source, "min_distance", CombatMovementConfig.LOCKED_MIN_TARGET_DISTANCE)
                ),
                rangedNumber(source, "attack_min_distance", 1.2),
                rangedNumber(source, "circle_distance", rangedNumber(source, "distance", 3.0)),
                rangedNumber(source, "circle_speed_min", 0.08),
                rangedNumber(source, "circle_speed_max", 0.22),
                (int) rangedNumber(source, "circle_min_ticks", 35.0),
                (int) rangedNumber(source, "circle_max_ticks", 80.0),
                rangedNumber(source, "lunge_speed", 0.58),
                rangedNumber(source, "chase_distance", 6.0),
                (int) rangedNumber(source, "hold_distance_ticks", 30.0),
                bool(source, "can_lunge", true),
                bool(source, "jump_attack", bool(source, "leaping_attack", false)),
                rangedNumber(source, "jump_velocity", 0.42),
                bool(source, "always_approach_until_close", false),
                rangedNumber(source, "approach_speed", 0.24),
                rangedNumber(source, "pursue_speed", 0.34),
                rangedNumber(source, "break_off_distance", 5.0),
                (int) rangedNumber(attackSource, "max_chain_attacks", 6.0)
        );
    }

    private static BeastCombatAiProfile.Mode beastMode(String value) {
        return switch (value.toLowerCase()) {
            case "2", "hold", "hold_and_rush", "rush", "charge", "stationary_charge", "distance_rush" ->
                    BeastCombatAiProfile.Mode.HOLD_AND_RUSH;
            default -> BeastCombatAiProfile.Mode.CIRCLE_LUNGE;
        };
    }

    private static JsonObject mergeFallback(JsonObject fallback, JsonObject override) {
        if (fallback == null) {
            fallback = new JsonObject();
        }
        if (override == null) {
            return fallback;
        }
        JsonObject merged = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : fallback.entrySet()) {
            merged.add(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, JsonElement> entry : override.entrySet()) {
            merged.add(entry.getKey(), entry.getValue());
        }
        return merged;
    }

    private static void registerProjectile(String entityName, JsonObject json) {
        Identifier entityId = Identifier.tryParse(entityName);
        if (entityId == null) {
            return;
        }

        ProjectileCombatAttributesRegistry.register(
                entityId,
                damageProfile(json.getAsJsonObject("damage_panel"), 0.0)
        );
    }

    private static void registerArmor(String itemName, JsonObject json) {
        Identifier itemId = Identifier.tryParse(itemName);
        if (itemId == null) {
            return;
        }

        Item item = Registries.ITEM.get(itemId);
        if (item == null) {
            return;
        }

        Map<String, ArmorCombatAttributes.PartProtection> parts = new HashMap<>();
        JsonObject protectedParts = json.getAsJsonObject("protected_parts");
        if (protectedParts != null) {
            for (Map.Entry<String, JsonElement> part : protectedParts.entrySet()) {
                JsonObject protection = part.getValue().getAsJsonObject();
                parts.put(
                        part.getKey(),
                        new ArmorCombatAttributes.PartProtection(
                                number(protection, "protection", 0.0),
                                number(protection, "impact_mitigation", 0.0)
                        )
                );
            }
        }

        ArmorCombatAttributes attributes = new ArmorCombatAttributes(
                damageProfile(json.getAsJsonObject("defense"), 0.0),
                parts,
                number(json, "stamina_cost_increase", 0.0),
                number(json, "attack_speed_penalty", 0.0),
                number(json, "movement_speed_penalty", 0.0)
        );
        EquipmentCombatAttributesRegistry.registerArmor(itemId, attributes);

        JsonArray aliases = json.getAsJsonArray("aliases");
        if (aliases != null) {
            for (JsonElement alias : aliases) {
                Identifier aliasId = Identifier.tryParse(alias.getAsString());
                if (aliasId != null && Registries.ITEM.get(aliasId) != null) {
                    EquipmentCombatAttributesRegistry.registerArmor(aliasId, attributes);
                }
            }
        }
    }

    private static JsonObject readJson(Identifier id, Resource resource) {
        try (InputStreamReader reader = new InputStreamReader(
                resource.getInputStream(),
                StandardCharsets.UTF_8
        )) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            KingdomComeCombat.LOGGER.warn("Failed to load combat data {}", id, e);
            return new JsonObject();
        }
    }

    private static DamageTypeProfile damageProfile(JsonObject json, double fallback) {
        if (json == null) {
            return DamageTypeProfile.even(fallback);
        }

        return new DamageTypeProfile(
                number(json, "thrust", fallback),
                number(json, "strike", fallback),
                number(json, "slash", fallback)
        );
    }

    private static List<AttackMoveConfig.HitZoneRule> hitZoneRules(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<AttackMoveConfig.HitZoneRule> rules = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject rule = element.getAsJsonObject();
            JsonArray partsArray = rule.getAsJsonArray("parts");
            List<String> parts = new ArrayList<>();
            if (partsArray != null) {
                for (JsonElement part : partsArray) {
                    parts.add(part.getAsString());
                }
            }

            rules.add(new AttackMoveConfig.HitZoneRule(
                    hitZone(firstString(rule, "any", "hit", "zone")),
                    parts
            ));
        }
        return rules;
    }

    private static List<AttackMoveConfig.HeightPartRule> heightPartRules(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<AttackMoveConfig.HeightPartRule> rules = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject rule = element.getAsJsonObject();
            rules.add(new AttackMoveConfig.HeightPartRule(
                    firstNumber(rule, 1.0, "max_height", "height", "up_to"),
                    firstString(rule, "", "part", "detailed_part")
            ));
        }
        return rules;
    }

    private static AttackMoveConfig.HitZone hitZone(String text) {
        return switch (text.toLowerCase()) {
            case "upper", "上", "上部", "头" -> AttackMoveConfig.HitZone.UPPER;
            case "middle", "中", "中部", "身" -> AttackMoveConfig.HitZone.MIDDLE;
            case "lower", "下", "下部", "腿" -> AttackMoveConfig.HitZone.LOWER;
            case "head", "头部" -> AttackMoveConfig.HitZone.HEAD;
            case "shoulder", "shoulders", "肩", "肩膀" -> AttackMoveConfig.HitZone.SHOULDERS;
            case "body", "torso", "chest", "躯干", "身体", "胸" -> AttackMoveConfig.HitZone.BODY;
            case "left_arm", "leftarm", "左臂", "左手臂" -> AttackMoveConfig.HitZone.LEFT_ARM;
            case "right_arm", "rightarm", "右臂", "右手臂" -> AttackMoveConfig.HitZone.RIGHT_ARM;
            case "arms", "arm", "双臂", "手臂" -> AttackMoveConfig.HitZone.ARMS;
            case "left_leg", "leftleg", "左腿" -> AttackMoveConfig.HitZone.LEFT_LEG;
            case "right_leg", "rightleg", "右腿" -> AttackMoveConfig.HitZone.RIGHT_LEG;
            case "legs", "leg", "双腿", "腿部" -> AttackMoveConfig.HitZone.LEGS;
            default -> AttackMoveConfig.HitZone.ANY;
        };
    }

    private static ShieldCombatAttributes.Size shieldSize(String value) {
        if (value == null) {
            return ShieldCombatAttributes.Size.SMALL;
        }

        return switch (value.toLowerCase()) {
            case "large", "big", "tower", "大", "大盾" -> ShieldCombatAttributes.Size.LARGE;
            default -> ShieldCombatAttributes.Size.SMALL;
        };
    }

    private static double defaultWeaponBaseImpact(Item item) {
        net.minecraft.item.ItemStack stack = item.getDefaultStack();
        if (CombatItemUtil.isLongsword(stack)) {
            return EquipmentFallbackConfig.longswordBaseImpact();
        }
        if (stack.isIn(ItemTags.SWORDS)) {
            return EquipmentFallbackConfig.swordBaseImpact();
        }
        if (CombatItemUtil.isFightingMace(stack)) {
            return EquipmentFallbackConfig.fightingMaceBaseImpact();
        }
        if (stack.isIn(ItemTags.PICKAXES)) {
            return EquipmentFallbackConfig.pickaxeBaseImpact();
        }
        if (stack.isIn(ItemTags.AXES)) {
            return EquipmentFallbackConfig.axeBaseImpact();
        }
        if (stack.isIn(ItemTags.SHOVELS)) {
            return EquipmentFallbackConfig.shovelBaseImpact();
        }
        if (stack.isIn(ItemTags.HOES)) {
            return EquipmentFallbackConfig.hoeBaseImpact();
        }
        return EquipmentFallbackConfig.defaultWeaponBaseImpact();
    }

    private static double defaultBlockImpactMitigation(Item item) {
        net.minecraft.item.ItemStack stack = item.getDefaultStack();
        if (CombatItemUtil.isLongsword(stack)) {
            return EquipmentFallbackConfig.longswordBlockImpactMitigation();
        }
        if (stack.isIn(ItemTags.SWORDS)) {
            return EquipmentFallbackConfig.swordBlockImpactMitigation();
        }
        if (CombatItemUtil.isFightingMace(stack)) {
            return EquipmentFallbackConfig.fightingMaceBlockImpactMitigation();
        }
        if (stack.isIn(ItemTags.PICKAXES)) {
            return EquipmentFallbackConfig.pickaxeBlockImpactMitigation();
        }
        if (stack.isIn(ItemTags.AXES)) {
            return EquipmentFallbackConfig.axeBlockImpactMitigation();
        }
        if (stack.isIn(ItemTags.SHOVELS)) {
            return EquipmentFallbackConfig.shovelBlockImpactMitigation();
        }
        if (stack.isIn(ItemTags.HOES)) {
            return EquipmentFallbackConfig.hoeBlockImpactMitigation();
        }
        return EquipmentFallbackConfig.defaultWeaponBlockImpactMitigation();
    }

    private static double defaultArmorBreakMultiplier(Item item) {
        net.minecraft.item.ItemStack stack = item.getDefaultStack();
        if (CombatItemUtil.isLongsword(stack)) {
            return EquipmentFallbackConfig.longswordArmorBreakMultiplier();
        }
        if (stack.isIn(ItemTags.SWORDS)) {
            return EquipmentFallbackConfig.swordArmorBreakMultiplier();
        }
        if (CombatItemUtil.isFightingMace(stack)) {
            return EquipmentFallbackConfig.fightingMaceArmorBreakMultiplier();
        }
        if (stack.isIn(ItemTags.PICKAXES)) {
            return EquipmentFallbackConfig.pickaxeArmorBreakMultiplier();
        }
        if (stack.isIn(ItemTags.AXES)) {
            return EquipmentFallbackConfig.axeArmorBreakMultiplier();
        }
        if (stack.isIn(ItemTags.SHOVELS)) {
            return EquipmentFallbackConfig.shovelArmorBreakMultiplier();
        }
        if (stack.isIn(ItemTags.HOES)) {
            return EquipmentFallbackConfig.hoeArmorBreakMultiplier();
        }
        return EquipmentFallbackConfig.defaultArmorBreakMultiplier();
    }

    private static double defaultWeaponAttackSpeedMultiplier(Item item) {
        net.minecraft.item.ItemStack stack = item.getDefaultStack();
        if (CombatItemUtil.isLongsword(stack)) {
            return EquipmentFallbackConfig.longswordAttackSpeedMultiplier();
        }
        if (stack.isIn(ItemTags.SWORDS)) {
            return EquipmentFallbackConfig.swordAttackSpeedMultiplier();
        }
        if (CombatItemUtil.isFightingMace(stack)) {
            return EquipmentFallbackConfig.fightingMaceAttackSpeedMultiplier();
        }
        if (stack.isIn(ItemTags.PICKAXES)) {
            return EquipmentFallbackConfig.pickaxeAttackSpeedMultiplier();
        }
        if (stack.isIn(ItemTags.AXES)) {
            return EquipmentFallbackConfig.axeAttackSpeedMultiplier();
        }
        if (stack.isIn(ItemTags.SHOVELS)) {
            return EquipmentFallbackConfig.shovelAttackSpeedMultiplier();
        }
        if (stack.isIn(ItemTags.HOES)) {
            return EquipmentFallbackConfig.hoeAttackSpeedMultiplier();
        }
        return EquipmentFallbackConfig.defaultWeaponAttackSpeedMultiplier();
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json != null && json.has(key) ? localizedString(json.get(key), fallback) : fallback;
    }

    private static String firstString(JsonObject json, String fallback, String... keys) {
        if (json == null) {
            return fallback;
        }

        for (String key : keys) {
            if (json.has(key)) {
                return localizedString(json.get(key), fallback);
            }
        }

        return fallback;
    }

    private static String localizedString(JsonElement element, String fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : List.of("default", "zh_cn", "zh", "en_us", "en")) {
                if (object.has(key)) {
                    return object.get(key).getAsString();
                }
            }
        }
        return fallback;
    }

    private static Map<String, PassiveSkillConfig.Text> passiveTranslations(JsonObject json) {
        JsonObject translations = json == null ? null : firstObject(json, "translations", "lang", "languages", "localized");
        if (translations == null) {
            translations = inlineTranslations(json, "name", "description", "desc", "text");
        }
        if (translations == null) {
            return Map.of();
        }

        Map<String, PassiveSkillConfig.Text> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : translations.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject value = entry.getValue().getAsJsonObject();
            result.put(
                    entry.getKey().toLowerCase(),
                    new PassiveSkillConfig.Text(
                            firstString(value, "", "name", "display_name", "title"),
                            firstString(value, "", "description", "desc", "text")
                    )
            );
        }
        return Map.copyOf(result);
    }

    private static Map<String, SkillBookTexts.EntryText> bookTranslations(JsonObject json) {
        JsonObject translations = json == null ? null : firstObject(json, "translations", "lang", "languages", "localized");
        if (translations == null) {
            translations = inlineTranslations(json, "title", "first_page", "page", "text", "second_page", "second_text", "learn_page", "illustration", "image", "picture");
        }
        if (translations == null) {
            return Map.of();
        }

        Map<String, SkillBookTexts.EntryText> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : translations.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject value = entry.getValue().getAsJsonObject();
            result.put(
                    entry.getKey().toLowerCase(),
                    new SkillBookTexts.EntryText(
                            firstString(value, "", "title", "name"),
                            firstString(value, "", "first_page", "page", "text"),
                            firstString(value, "", "second_page", "second_text", "learn_page"),
                            firstString(value, "", "illustration", "image", "picture")
                    )
            );
        }
        return Map.copyOf(result);
    }

    private static JsonObject inlineTranslations(JsonObject json, String... keys) {
        if (json == null) {
            return null;
        }
        Map<String, JsonObject> byLanguage = new HashMap<>();
        for (String key : keys) {
            if (!json.has(key) || !json.get(key).isJsonObject()) {
                continue;
            }
            JsonObject values = json.getAsJsonObject(key);
            for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                JsonObject language = byLanguage.computeIfAbsent(entry.getKey().toLowerCase(), ignored -> new JsonObject());
                language.addProperty(key, entry.getValue().getAsString());
            }
        }
        if (byLanguage.isEmpty()) {
            return null;
        }
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : byLanguage.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Map<String, Double> numberMap(JsonObject json) {
        if (json == null) {
            return Map.of();
        }
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                result.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        }
        return Map.copyOf(result);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json != null && json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static double firstNumber(JsonObject json, double fallback, String... keys) {
        if (json == null) {
            return fallback;
        }

        for (String key : keys) {
            if (json.has(key)) {
                return json.get(key).getAsDouble();
            }
        }

        return fallback;
    }

    private static double impactMultiplier(JsonObject json, double fallbackImpact) {
        if (json == null) {
            return fallbackImpact / 20.0;
        }

        if (json.has("impact_multiplier")) {
            return Math.max(0.0, json.get("impact_multiplier").getAsDouble());
        }

        if (json.has("impact")) {
            return Math.max(0.0, json.get("impact").getAsDouble() / 20.0);
        }

        return Math.max(0.0, fallbackImpact / 20.0);
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json != null && json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static double rangedNumber(JsonObject json, String key, double fallback) {
        if (json == null || !json.has(key)) {
            return fallback;
        }

        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            JsonArray range = element.getAsJsonArray();
            if (range.size() >= 2) {
                double min = range.get(0).getAsDouble();
                double max = range.get(1).getAsDouble();
                if (max < min) {
                    double temp = min;
                    min = max;
                    max = temp;
                }
                return min + Math.random() * (max - min);
            }
        }

        return element.getAsDouble();
    }

    private static double firstRangedNumber(JsonObject json, double fallback, String... keys) {
        if (json == null) {
            return fallback;
        }

        for (String key : keys) {
            if (json.has(key)) {
                return rangedNumber(json, key, fallback);
            }
        }
        return fallback;
    }

    private static Vec3d vector3(JsonElement element, double fallbackX, double fallbackY, double fallbackZ) {
        if (element == null || element.isJsonNull()) {
            return new Vec3d(fallbackX, fallbackY, fallbackZ);
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            return new Vec3d(
                    number(object, "x", fallbackX),
                    number(object, "y", fallbackY),
                    number(object, "z", fallbackZ)
            );
        }

        if (element.isJsonArray() && element.getAsJsonArray().size() >= 3) {
            JsonArray array = element.getAsJsonArray();
            return new Vec3d(
                    array.get(0).getAsDouble(),
                    array.get(1).getAsDouble(),
                    array.get(2).getAsDouble()
            );
        }

        double value = element.getAsDouble();
        return new Vec3d(value, value, value);
    }

    private static JsonElement firstPresent(JsonObject json, String... keys) {
        if (json == null) {
            return null;
        }

        for (String key : keys) {
            if (json.has(key)) {
                return json.get(key);
            }
        }

        return null;
    }

    private static JsonObject firstObject(JsonObject json, String... keys) {
        JsonElement element = firstPresent(json, keys);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json != null && json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static Map<String, String> stringMap(JsonObject json) {
        if (json == null) {
            return Map.of();
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            result.put(entry.getKey().toLowerCase(), entry.getValue().getAsString());
        }
        return result;
    }

    private static Map<String, String> mergeStringMap(
            Map<String, String> inherited,
            JsonObject overrides
    ) {
        Map<String, String> result = new HashMap<>();
        if (inherited != null) result.putAll(inherited);
        result.putAll(stringMap(overrides));
        return Map.copyOf(result);
    }

    private static String stripJson(Identifier id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        return fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length())
                : fileName;
    }
}
