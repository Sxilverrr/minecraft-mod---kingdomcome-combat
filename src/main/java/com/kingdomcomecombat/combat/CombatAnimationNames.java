package com.kingdomcomecombat.combat;

public class CombatAnimationNames {
    private CombatAnimationNames() {
    }

    public static String fileName(String animationName) {
        return switch (animationName) {
            case "attack_right_heavy" -> "attack_right";
            case "attack_left_heavy" -> "attack_left";
            case "attack_up_heavy" -> "attack_up";
            case "attack_down_heavy" -> "attack_down";

            case "attack_right_longsword" -> "attack_right";
            case "attack_left_longsword" -> "attack_left";
            case "attack_up_longsword" -> "attack_up";
            case "attack_down_longsword" -> "attack_down";

            case "attack_right_longweapon" -> "attack_right";
            case "attack_left_longweapon" -> "attack_left";
            case "attack_down_longweapon" -> "attack_down";

            case "stance_right_longsword" -> "stance_right";
            case "stance_left_longsword" -> "stance_left";
            case "stance_up_longsword" -> "stance_up";
            case "stance_down_longsword" -> "stance_down";

            case "stance_right_longweapon" -> "stance_right";
            case "stance_left_longweapon" -> "stance_left";
            case "stance_down_longweapon" -> "stance_down";

            case "attack_smash" -> "combo_smash";
            case "smash_hit" -> "combo_smash";

            case "master_counter_right",
                 "master_strike_right",
                 "master_counter_left",
                 "master_strike_left",
                 "master_counter_up",
                 "master_strike_up",
                 "master_counter_down",
                 "master_strike_down" -> "master_strike";

            case "master_counter_right_victim",
                 "master_strike_hit_right",
                 "master_counter_left_victim",
                 "master_strike_hit_left",
                 "master_counter_up_victim",
                 "master_strike_hit_up",
                 "master_counter_down_victim",
                 "master_strike_hit_down" -> "master_strike_hit";

            default -> animationName;
        };
    }

    public static String animationKey(String animationName) {
        return switch (animationName) {
            case "attack_smash" -> "combo_smash";

            case "master_counter_right", "master_strike_right" -> "master_strike_right";
            case "master_counter_left", "master_strike_left" -> "master_strike_left";
            case "master_counter_up", "master_strike_up" -> "master_strike_up";
            case "master_counter_down", "master_strike_down" -> "master_strike_down";

            case "master_counter_right_victim", "master_strike_hit_right" -> "master_strike_hit_right";
            case "master_counter_left_victim", "master_strike_hit_left" -> "master_strike_hit_left";
            case "master_counter_up_victim", "master_strike_hit_up" -> "master_strike_hit_up";
            case "master_counter_down_victim", "master_strike_hit_down" -> "master_strike_hit_down";

            default -> animationName;
        };
    }

    public static String fallbackAnimationKey(String animationName) {
        return isHeavyAttack(animationName) ? "attack heavy" : fileName(animationName);
    }

    public static boolean isHeavyAttack(String animationName) {
        return switch (animationName) {
            case "attack_right_heavy",
                 "attack_left_heavy",
                 "attack_up_heavy",
                 "attack_down_heavy" -> true;
            default -> false;
        };
    }
}
