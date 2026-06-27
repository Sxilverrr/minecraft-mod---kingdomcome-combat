package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.equipment.DamageTypeProfile;
import com.kingdomcomecombat.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

public class ComboMoveConfigs {
    private static final Map<String, List<ComboMoveConfig>> BY_SEQUENCE = new ConcurrentHashMap<>();

    static {
        register(
                "上左右",
                new ComboMoveConfig(
                        "smash_attack",
                        "粉碎打击",
                        parseSequence("上左右"),
                        new DamageTypeProfile(0.20, 1.20, 0.0),
                        "attack_smash",
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.LEGS,
                                        List.of("thigh")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.ANY,
                                        List.of("face")
                                )
                        ),
                        30.0,
                        8.0,
                        0.0,
                        false,
                        false,
                        false,
                        true,
                        true,
                        "head",
                        1,
                        "short_swords",
                        "smash_hit",
                        true,
                        15,
                        -1,
                        "",
                        0.0,
                        0.0,
                        1.15,
                        "",
                        List.of(),
                        2
                )
        );
        register(
                "下下右",
                new ComboMoveConfig(
                        "pommel_strike",
                        "剑柄打击",
                        parseSequence("下下右"),
                        new DamageTypeProfile(0.20, 1.20, 0.0),
                        "attack_smash",
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.LEGS,
                                        List.of("thigh")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.ANY,
                                        List.of("face")
                                )
                        ),
                        30.0,
                        8.0,
                        0.0,
                        false,
                        true,
                        false,
                        true,
                        true,
                        "head",
                        1,
                        "longswords",
                        "smash_hit",
                        true,
                        15,
                        -1,
                        "",
                        0.0,
                        0.0,
                        1.15,
                        "",
                        List.of(),
                        2
                )
        );
        register(
                "左右左",
                new ComboMoveConfig(
                        "comboleft",
                        "comboleft",
                        parseSequence("左右左"),
                        new DamageTypeProfile(0.0, 0.15, 1.40),
                        "combo_left",
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.ANY,
                                        List.of("abdomen")
                                )
                        ),
                        24.0,
                        8.0,
                        0.4,
                        true,
                        true,
                        false,
                        false,
                        true,
                        "torso",
                        1
                )
        );
    }

    private ComboMoveConfigs() {
    }

    public static void register(String sequenceText, ComboMoveConfig config) {
        BY_SEQUENCE.compute(sequenceKey(parseSequence(sequenceText)), (ignored, existing) -> {
            List<ComboMoveConfig> configs = new ArrayList<>();
            if (existing != null) {
                configs.addAll(existing);
            }
            configs.add(config);
            return List.copyOf(configs);
        });
    }

    public static Optional<ComboMoveConfig> findMatchingTail(List<CombatDirection> recent) {
        return findMatchingTail(recent, ItemStack.EMPTY);
    }

    public static Optional<ComboMoveConfig> findMatchingTail(
            List<CombatDirection> recent,
            ItemStack weapon
    ) {
        return findMatchingTail(recent, weapon, ignored -> true);
    }

    public static Optional<ComboMoveConfig> findMatchingTail(
            List<CombatDirection> recent,
            ItemStack weapon,
            Predicate<ComboMoveConfig> validator
    ) {
        return findMatchingTail(recent, weapon, false, validator);
    }

    public static Optional<ComboMoveConfig> findMatchingTail(
            List<CombatDirection> recent,
            ItemStack weapon,
            boolean longswordAsShortSword,
            Predicate<ComboMoveConfig> validator
    ) {
        ComboMoveConfig best = null;
        int bestLength = 0;

        for (ComboMoveConfig config : configs()) {
            if (!canUseWith(config, weapon, longswordAsShortSword) || !validator.test(config)) {
                continue;
            }

            List<CombatDirection> sequence = config.sequence();
            if (sequence.size() > recent.size() || sequence.size() <= bestLength) {
                continue;
            }

            int offset = recent.size() - sequence.size();
            boolean matches = true;
            for (int i = 0; i < sequence.size(); i++) {
                if (recent.get(offset + i) != sequence.get(i)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                best = config;
                bestLength = sequence.size();
            }
        }

        return Optional.ofNullable(best);
    }

    public static Optional<ComboMoveConfig> findExact(List<CombatDirection> sequence) {
        return findExact(sequence, ItemStack.EMPTY);
    }

    public static Optional<ComboMoveConfig> findExact(
            List<CombatDirection> sequence,
            ItemStack weapon
    ) {
        List<ComboMoveConfig> configs = BY_SEQUENCE.get(sequenceKey(sequence));
        if (configs == null) {
            return Optional.empty();
        }

        return configs.stream()
                .filter(config -> canUseWith(config, weapon))
                .findFirst();
    }

    public static List<ComboMoveConfig> findByPrefix(
            List<CombatDirection> prefix,
            ItemStack weapon,
            Predicate<ComboMoveConfig> validator
    ) {
        List<ComboMoveConfig> matches = new ArrayList<>();
        if (prefix.isEmpty()) {
            return matches;
        }

        for (ComboMoveConfig config : configs()) {
            if (!canUseWith(config, weapon) || !validator.test(config)) {
                continue;
            }

            List<CombatDirection> sequence = config.sequence();
            if (sequence.size() <= prefix.size()) {
                continue;
            }

            boolean matchesPrefix = true;
            for (int i = 0; i < prefix.size(); i++) {
                if (sequence.get(i) != prefix.get(i)) {
                    matchesPrefix = false;
                    break;
                }
            }

            if (matchesPrefix) {
                matches.add(config);
            }
        }

        return matches;
    }

    public static List<ComboMoveConfig> all() {
        return configs();
    }

    public static Optional<ComboMoveConfig> findByAnimationName(String animationName) {
        return configs().stream()
                .filter(config -> config.animationName().equals(animationName))
                .findFirst();
    }

    public static void clear() {
        BY_SEQUENCE.clear();
    }

    public static boolean isEmpty() {
        return BY_SEQUENCE.isEmpty();
    }

    public static int maxSequenceLength() {
        int max = 0;
        for (ComboMoveConfig config : configs()) {
            max = Math.max(max, config.sequence().size());
        }
        return max;
    }

    public static boolean canUseWith(ComboMoveConfig config, ItemStack weapon) {
        return canUseWith(config, weapon, false);
    }

    public static boolean canUseWith(
            ComboMoveConfig config,
            ItemStack weapon,
            boolean longswordAsShortSword
    ) {
        if (config == null) {
            return false;
        }

        String tag = config.requiredWeaponTag();
        if (tag.isBlank()) {
            return true;
        }

        if (weapon == null || weapon.isEmpty()) {
            return false;
        }

        return switch (tag) {
            case "sword", "swords", "minecraft:swords" -> CombatItemUtil.isSword(weapon);
            case "short_sword", "short_swords" -> CombatItemUtil.isShortSword(weapon)
                    || (longswordAsShortSword && isLongsword(weapon));
            case "longsword", "longswords" -> isLongsword(weapon) && !longswordAsShortSword;
            case "axe", "axes", "minecraft:axes" -> weapon.isIn(ItemTags.AXES);
            case "pickaxe", "pickaxes", "minecraft:pickaxes" -> weapon.isIn(ItemTags.PICKAXES);
            case "fighting_mace", "fighting_maces", "mace", "maces", "hammer", "hammers" ->
                    CombatItemUtil.isFightingMace(weapon);
            case "heavy_weapon", "heavy_weapons", "heavy", "重武器" ->
                    CombatItemUtil.isHeavyWeapon(weapon);
            case "hoe", "hoes", "minecraft:hoes" -> weapon.isIn(ItemTags.HOES);
            case "shovel", "shovels", "minecraft:shovels" -> weapon.isIn(ItemTags.SHOVELS);
            default -> true;
        };
    }

    private static boolean isLongsword(ItemStack weapon) {
        return CombatItemUtil.isLongsword(weapon);
    }

    private static List<ComboMoveConfig> configs() {
        List<ComboMoveConfig> configs = new ArrayList<>();
        for (List<ComboMoveConfig> sequenceConfigs : BY_SEQUENCE.values()) {
            configs.addAll(sequenceConfigs);
        }
        return List.copyOf(configs);
    }

    public static List<CombatDirection> parseSequence(String text) {
        List<CombatDirection> directions = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            directions.add(parseDirection(text.charAt(i)));
        }
        return directions;
    }

    private static CombatDirection parseDirection(char direction) {
        return switch (direction) {
            case '左' -> CombatDirection.LEFT;
            case '右' -> CombatDirection.RIGHT;
            case '上' -> CombatDirection.UP;
            case '下' -> CombatDirection.DOWN;
            default -> throw new IllegalArgumentException("Unknown combo direction: " + direction);
        };
    }

    private static String sequenceKey(List<CombatDirection> sequence) {
        StringBuilder builder = new StringBuilder();
        for (CombatDirection direction : sequence) {
            if (!builder.isEmpty()) {
                builder.append(',');
            }
            builder.append(direction.name());
        }
        return builder.toString();
    }
}
