# Kingdom Come Combat

## English

Kingdom Come Combat is a realism-oriented combat mod for Minecraft 1.21.x. Fabric is the primary platform, with a NeoForge 1.21.1 entry point maintained under `neoforge/`.

This guide explains how to extend the mod with data packs, including new weapons and weapon categories, combos, passive skills, and humanoid combat AI. The examples use the `example_mod` namespace; replace it with your own namespace, or use `kingdom_come_combat` when editing this project directly.

### Quick start

Using a separate data pack is recommended instead of editing the built-in `default.json` files:

```text
my_kcc_pack/
├── pack.mcmeta
└── data/
    └── example_mod/
        └── kingdom_come_combat/
            ├── weapons/weapons.json
            ├── combos/combos.json
            ├── passive_skills/passive_skills.json
            ├── skill_books/my_skill.json
            └── ai/ai.json
```

Set the `pack_format` in `pack.mcmeta` to the value required by your target Minecraft version. Put the pack in the world's `datapacks/` directory and run:

```mcfunction
/reload
```

Every `*.json` file below these directories is loaded. Multiple files may contribute definitions, while definitions with the same semantic ID are overridden according to data-pack priority. Complete built-in examples are available under:

```text
src/main/resources/data/kingdom_come_combat/kingdom_come_combat/
```

### Adding a weapon

Create `data/example_mod/kingdom_come_combat/weapons/weapons.json`:

```json
{
  "items": {
    "example_mod:steel_longsword": {
      "damage_panel": {
        "thrust": 0.9,
        "strike": 0.4,
        "slash": 1.2
      },
      "attack_speed_multiplier": 0.9,
      "base_impact": 28.0,
      "block_impact_mitigation": 0.82,
      "armor_break_multiplier": 0.35,
      "weapon_toughness": 1.0,
      "minimum_durability_panel_multiplier": 0.75,
      "held_movement_speed_multiplier": 0.95,
      "real_hitbox_size_units": { "x": 5.0, "y": 1.5, "z": 24.0 },
      "real_hitbox_offset_units": { "x": 0.0, "y": 0.0, "z": 0.0 },
      "real_hitbox_rotation_degrees": { "x": 0.0, "y": 0.0, "z": -45.0 },
      "attack_moves": {
        "right": "attack_right_longsword",
        "left": "attack_left_longsword",
        "up": "attack_up_longsword",
        "down": "attack_down_longsword"
      },
      "stance_animations": {
        "right": "stance_right_longsword",
        "left": "stance_left_longsword",
        "up": "stance_up_longsword",
        "down": "stance_down_longsword"
      }
    }
  }
}
```

| Field | Meaning |
| --- | --- |
| `damage_panel` | Thrust, strike, and slash damage weights |
| `attack_speed_multiplier` | Attack animation speed multiplier, multiplied with armor and skill modifiers |
| `base_impact` | Base impact used by blocking and stagger calculations |
| `block_impact_mitigation` | Impact reduction while blocking with this weapon, clamped to `0–1` |
| `armor_break_multiplier` | Multiplier applied to armor durability damage |
| `weapon_toughness` | Weapon durability resistance when used to block |
| `minimum_durability_panel_multiplier` | Minimum damage-panel multiplier when the weapon is nearly broken |
| `held_movement_speed_multiplier` | Movement speed multiplier while held, clamped to `0–1` |
| `real_hitbox_*` | Size, offset, and Euler rotation of the real weapon hitbox |
| `attack_moves` | Move IDs for the four attack directions |
| `stance_animations` | Stance animation names for the four directions |
| `executions` | Optional execution move/set IDs for the four directions |

Direction keys are `left`, `right`, `up`, and `down`.

The key inside `items` may also be an item tag. Exact item definitions take priority over tag fallbacks:

```json
{
  "items": {
    "#example_mod:longswords": {
      "damage_panel": { "thrust": 0.8, "strike": 0.4, "slash": 1.1 },
      "attack_speed_multiplier": 0.9
    },
    "example_mod:royal_longsword": {
      "attack_speed_multiplier": 1.05
    }
  }
}
```

Use `aliases` to apply one complete definition to additional item IDs or tags:

```json
{
  "items": {
    "example_mod:steel_longsword": {
      "aliases": [
        "example_mod:ornate_longsword",
        "#example_mod:steel_longsword_variants"
      ],
      "damage_panel": { "thrust": 0.9, "strike": 0.4, "slash": 1.2 }
    }
  }
}
```

Combos and some passive skills determine weapon categories from item tags or configured default categories, not from the damage panel. Add the item to the relevant KCC tag, for example `data/kingdom_come_combat/tags/item/longswords.json`:

```json
{
  "replace": false,
  "values": ["example_mod:steel_longsword"]
}
```

Existing combo categories include `swords`, `short_sword`/`short_swords`, `longswords`, `axes`, `pickaxes`, `hammers`, and `heavy_weapons`. Heavy weapons include heavy hammers, axes, and pickaxes.

If a “new weapon type” is merely a new item or a variant of an existing category, tags and weapon JSON are sufficient. A genuinely new semantic category such as `rapiers` requires Java changes:

1. Add category detection to `CombatItemUtil`/`CombatWeaponUtil`.
2. Add the new `required_weapon_tag` case to combo weapon validation.
3. Add a condition such as `condition_weapon_rapier` to `PassiveSkillPerks` if passive skills need it.
4. Add the corresponding item tag under `data/kingdom_come_combat/tags/item/`.

### Adding a combo

Create `data/example_mod/kingdom_come_combat/combos/my_combo.json`:

```json
{
  "combos": {
    "moon_cut": {
      "display_name": "Moon Cut",
      "level": 2,
      "sequence": "下右左",
      "animation": "combo_highspot_cut",
      "required_weapon_tag": "longswords",
      "damage_modifiers": { "thrust": 0.0, "strike": 0.6, "slash": 1.25 },
      "hit_zones": [
        { "hit": "head", "parts": ["neck", "side_head"] }
      ],
      "impact_multiplier": 1.5,
      "stamina_cost": 25.0,
      "horizontal_knockback": 0.7,
      "blade_trail": true,
      "hit_reaction": true,
      "hit_reaction_interrupts_attack": true,
      "hit_reaction_movement_lock_ticks": 20,
      "suction": true,
      "suction_distance": 2.6,
      "suction_fixed_distance": 1.5,
      "lunge": false,
      "use_real_hitbox": false,
      "injury_type": "head",
      "injury_level": 2,
      "attack_chain": [
        {
          "tick": 5,
          "hit_weapon": true,
          "weapon_clash_sound": "sword_hit_sword"
        },
        {
          "tick": 14,
          "damage_modifiers": { "thrust": 0.0, "strike": 0.6, "slash": 1.25 },
          "hit_zones": [{ "hit": "head", "parts": ["neck"] }]
        }
      ]
    }
  }
}
```

Each attack records its direction. When the recent direction history matches `sequence`, the combo is triggered after its level, weapon-category, unlock, and stamina requirements are checked. The current sequence parser requires the Chinese direction characters `左`, `右`, `上`, and `下`; English words are not accepted in this field.

Important fields:

- `animation` is the attacker animation; `victim_animation` optionally selects a victim animation.
- `damage_modifiers` controls thrust, strike, and slash multipliers.
- `hit_zones` restricts the hit to regions and detailed parts. Part IDs include `face`, `neck`, `crown`, `side_head`, `shoulder`, `arm`, `hand`, `chest`, `abdomen`, `thigh`, `knee`, `calf`, and `foot`.
- `suction` and its distance fields control target attraction; `lunge` controls forward movement.
- `use_real_hitbox` enables the configured weapon hitbox.
- `attack_chain` schedules multiple damage or weapon-clash events at animation ticks. `hit_weapon: true` produces a weapon clash rather than a body hit.

An animation name must already exist in the client animation resources/library. A JSON name alone does not create an animation, so reuse an animation from the built-in combo definitions when testing.

### Adding a passive skill

Create `data/example_mod/kingdom_come_combat/passive_skills/my_skills.json`:

```json
{
  "skills": {
    "patient_blade": {
      "name": "Patient Blade",
      "description": "Regenerate stamina 15% faster while wielding a longsword.",
      "translations": {
        "zh_cn": {
          "name": "耐心之刃",
          "description": "使用长剑时，体力恢复速度提高 15%。"
        }
      },
      "icon": "patient_blade",
      "source": "experience",
      "experience_cost": 1200,
      "perks": {
        "condition_weapon_longsword": 1.0,
        "stamina_regen_multiplier": 1.15
      }
    }
  }
}
```

Place the icon at `assets/example_mod/textures/gui/passive_skills/patient_blade.png`. `icon` may contain a complete resource path or a simple file name.

Unlock sources:

| `source` | Behavior | Additional field |
| --- | --- | --- |
| `experience` | Hold to learn it in the skill screen, spending combat-experience points rather than levels | `experience_cost` |
| `advancement` | Learned automatically when the advancement is completed | `advancement` |
| `book` | Learned from a skill book | A matching `skill_books/*.json` definition |

Minimal skill-book definition:

```json
{
  "id": "patient_blade",
  "passive_id": "patient_blade",
  "title": "Patient Blade",
  "author": "Swordmaster",
  "first_page": "Notes on patience and distance.",
  "second_page": "Turn to this page to comprehend the skill."
}
```

Passive perks are implemented numeric keys, not arbitrary expressions. Common conditions include `condition_weapon_longsword`, `condition_weapon_sword`, `condition_weapon_short_sword`, `condition_weapon_axe`, `condition_weapon_crossbow`, `condition_combo`, `condition_master_counter`, `condition_mounted`, `condition_offhand_empty`, `condition_head_hit`, and health/armor threshold conditions.

Common effects include `damage_multiplier`, `attack_speed_multiplier`, `stamina_cost_multiplier`, `attack_stamina_cost_multiplier`, `stamina_regen_multiplier`, `max_stamina_bonus`, `dodge_stamina_cost_multiplier`, `armor_penalty_multiplier`, `kill_stamina_restore`, `combo_stamina_restore`, `combo_damage_multiplier`, `combo_injury_levels`, `head_hit_damage_multiplier`, `projectile_spread_multiplier`, and `projectile_speed_multiplier`.

See `passive_skills/default.json` for working combinations. A new trigger or effect must be implemented in `PassiveSkillPerks.java`.

### Adding humanoid combat AI

Create `data/example_mod/kingdom_come_combat/ai/my_mobs.json`:

```json
{
  "entities": {
    "example_mod:mercenary": {
      "min_attack_interval_ticks": [16, 22],
      "attack_desire_per_half_second": [0.40, 0.55],
      "block_chance": 0.75,
      "perfect_block_chance": 0.25,
      "combo_level": 3,
      "combo_plan_chance": 0.55,
      "dodge_chance": 0.06,
      "ai_level": 3,
      "stamina_max": 120.0,
      "stamina_regen_per_tick": 0.34,
      "attack_animation_speed": 0.85,
      "attack_startup_slowdown": 0.4,
      "toughness": 5.0,
      "worn_armor_durability_multiplier": 1.0,
      "follow_up_attack_chance": 0.75,
      "max_follow_up_attacks": 4,
      "keep_distance": false,
      "requires_weapon": true,
      "combat_enter_distance": 7.0,
      "attack_distance": 2.2,
      "approach_distance": 3.2,
      "exhausted_retreat_distance": 4.0
    }
  }
}
```

Numeric values may be fixed or expressed as `[minimum, maximum]`. The AI enters combat after finding a target, moves according to distance and stamina, attacks according to desire and its minimum interval, and uses its level to choose directional changes, follow-ups, and combo plans. A successful `combo_plan_chance` roll selects a combo allowed by `combo_level` and the equipped weapon, then follows its sequence.

The entity ID must exist in the registry. `requires_weapon: true` prevents the profile from activating while unarmed. Automatic compatibility primarily covers skeleton-like mobs, illagers, and server-detectable vanilla Biped inheritance. Custom-model entities may require explicit inclusion. Animals and non-humanoid creatures use the separate `beast_ai/` system.

### Architecture and important source files

| Location | Responsibility |
| --- | --- |
| `src/main/java/com/kingdomcomecombat/data/CombatDataReloadListener.java` | Loads and merges all combat JSON definitions according to data-pack priority |
| `src/main/java/com/kingdomcomecombat/equipment/EquipmentCombatAttributesRegistry.java` | Resolves weapon properties from exact item IDs and tag fallbacks |
| `src/main/java/com/kingdomcomecombat/equipment/WeaponCombatAttributes.java` | Weapon-property record and value constraints |
| `src/main/java/com/kingdomcomecombat/combat/CombatItemUtil.java` | Weapon-category detection |
| `src/main/java/com/kingdomcomecombat/combat/ComboMoveConfigs.java` | Parses direction sequences and stores combo definitions |
| `src/main/java/com/kingdomcomecombat/combat/ServerComboState.java` | Server-side combo matching and execution state |
| `src/main/java/com/kingdomcomecombat/combat/ServerCombatTicker.java` | Main server combat loop for attacks, blocking, stamina, and combos |
| `src/main/java/com/kingdomcomecombat/collision/ServerHitDetectionSystem.java` | Server-side authoritative hit detection |
| `src/main/java/com/kingdomcomecombat/collision/AnimatedAttackHitboxLibrary.java` | Animation-driven weapon hitbox calculation |
| `src/main/java/com/kingdomcomecombat/passive/PassiveSkillConfigs.java` | Passive-skill registry |
| `src/main/java/com/kingdomcomecombat/passive/PassiveSkillPerks.java` | Passive conditions and effect calculations |
| `src/main/java/com/kingdomcomecombat/passive/PlayerPassiveSkillProgress.java` | Persistent player unlock data |
| `src/main/java/com/kingdomcomecombat/ai/HumanoidCombatAiProfiles.java` | Entity-to-profile registration and defaults |
| `src/main/java/com/kingdomcomecombat/ai/HumanoidCombatAiTicker.java` | Humanoid movement, attack, defense, dodge, and combo decisions |
| `src/main/java/com/kingdomcomecombat/ai/AiAttackCoordinator.java` | Connects AI decisions to the combat system |
| `src/main/java/com/kingdomcomecombat/api/KingdomComeCombatApi.java` | Public API for other code |
| `src/client/java/com/kingdomcomecombat/` | Client input, animation, HUD, and rendering |
| `src/main/java/com/kingdomcomecombat/network/` | Client/server input, animation, and state synchronization |

The overall data flow is:

```text
Data-pack JSON
    ↓ /reload
CombatDataReloadListener
    ↓
Weapon / combo / passive-skill / AI registries
    ↓
Client input or AI decision
    ↓ network payload
Server combat state and hit detection
    ↓
Damage, stamina, stagger, injury, and animation synchronization
```

The server is authoritative for combat results. The client collects input and renders feedback, while attack state, hit detection, and damage are advanced by the server.

### Development and building

Build the default Minecraft 1.21.7 target:

```bash
./gradlew build
```

Build another supported target:

```bash
./gradlew -Ptarget_mc=1.21.8 build
./gradlew -Ptarget_mc=1.21.11 build
```

Run the development client:

```bash
./gradlew runClient
```

Build artifacts are written to `build/libs/`. Before committing configuration changes, run `./gradlew processResources` and `./gradlew compileJava compileClientJava`, and validate new JSON with `jq empty path/to/file.json`.

### Troubleshooting

- If `/reload` changes nothing, verify the `data/<namespace>/kingdom_come_combat/<type>/*.json` path, registered item/entity IDs, and JSON errors in the game log.
- If a combo does not trigger, verify its sequence, unlock state, weapon category, required level, stamina, and animation name.
- If a passive skill appears but has no effect, verify that its perk keys are implemented in `PassiveSkillPerks.java`.
- If a third-party mob still uses vanilla attacks, verify that the entity is suitable for humanoid AI, is not excluded, and is explicitly included when it uses a fully custom model.

### License

See [LICENSE](LICENSE).

---

## 中文

一个面向 Minecraft 1.21.x 的写实近战系统模组。项目同时支持 Fabric，并在 `neoforge/` 中维护 NeoForge 1.21.1 入口。

本文主要说明如何通过数据包扩展：

- 新武器与武器类别
- 连招
- 被动技能
- 人形战斗 AI

> 示例均使用命名空间 `example_mod`。如果内容由独立数据包提供，请替换成自己的命名空间；如果直接修改本项目，可以使用 `kingdom_come_combat`。

## 快速开始

推荐把扩展内容做成独立数据包，而不是直接修改 `default.json`。一个最小目录如下：

```text
my_kcc_pack/
├── pack.mcmeta
└── data/
    └── example_mod/
        └── kingdom_come_combat/
            ├── weapons/
            │   └── weapons.json
            ├── combos/
            │   └── combos.json
            ├── passive_skills/
            │   └── passive_skills.json
            ├── skill_books/
            │   └── my_skill.json
            └── ai/
                └── ai.json
```

`pack.mcmeta` 的 `pack_format` 应与目标 Minecraft 版本一致。把数据包放入世界的 `datapacks/` 目录，进入世界后执行：

```mcfunction
/reload
```

所有目录中的 `*.json` 都会被读取。多个文件可以共同添加内容；同名语义 ID 按数据包优先级覆盖，因此不必复制整个内置配置。

项目内置的完整范例位于：

```text
src/main/resources/data/kingdom_come_combat/kingdom_come_combat/
```

## 添加新武器

### 为单个物品添加战斗属性

创建 `data/example_mod/kingdom_come_combat/weapons/weapons.json`：

```json
{
  "items": {
    "example_mod:steel_longsword": {
      "damage_panel": {
        "thrust": 0.9,
        "strike": 0.4,
        "slash": 1.2
      },
      "attack_speed_multiplier": 0.9,
      "base_impact": 28.0,
      "block_impact_mitigation": 0.82,
      "armor_break_multiplier": 0.35,
      "weapon_toughness": 1.0,
      "minimum_durability_panel_multiplier": 0.75,
      "held_movement_speed_multiplier": 0.95,
      "real_hitbox_size_units": { "x": 5.0, "y": 1.5, "z": 24.0 },
      "real_hitbox_offset_units": { "x": 0.0, "y": 0.0, "z": 0.0 },
      "real_hitbox_rotation_degrees": { "x": 0.0, "y": 0.0, "z": -45.0 },
      "attack_moves": {
        "right": "attack_right_longsword",
        "left": "attack_left_longsword",
        "up": "attack_up_longsword",
        "down": "attack_down_longsword"
      },
      "stance_animations": {
        "right": "stance_right_longsword",
        "left": "stance_left_longsword",
        "up": "stance_up_longsword",
        "down": "stance_down_longsword"
      }
    }
  }
}
```

主要字段：

| 字段 | 含义 |
| --- | --- |
| `damage_panel` | 刺击、打击、斩击的伤害权重 |
| `attack_speed_multiplier` | 攻击动画速度倍率，会与护甲、技能等倍率乘算 |
| `base_impact` | 基础冲击力，影响格挡与硬直 |
| `block_impact_mitigation` | 使用该武器格挡时的冲击减免，范围 `0~1` |
| `armor_break_multiplier` | 对护甲耐久损耗的倍率 |
| `weapon_toughness` | 武器格挡时自身的耐久承受能力 |
| `minimum_durability_panel_multiplier` | 武器接近损坏时，伤害面板的最低倍率 |
| `held_movement_speed_multiplier` | 手持武器时的移动速度倍率，范围 `0~1` |
| `real_hitbox_*` | 真实武器碰撞盒的尺寸、偏移和欧拉角；单位尺寸按模型单位配置 |
| `attack_moves` | 四个攻击方向对应的动作 ID |
| `stance_animations` | 四个方向对应的架势动画名 |
| `executions` | 可选；四个方向对应的处决动作组/动作 ID |

方向键统一为 `left`、`right`、`up`、`down`。

### 一次适配一组武器

`items` 的键可以是物品标签：

```json
{
  "items": {
    "#example_mod:longswords": {
      "damage_panel": { "thrust": 0.8, "strike": 0.4, "slash": 1.1 },
      "attack_speed_multiplier": 0.9
    },
    "example_mod:royal_longsword": {
      "attack_speed_multiplier": 1.05
    }
  }
}
```

精确物品 ID 的配置优先于标签配置。配置中还可使用 `aliases` 复用整套属性：

```json
{
  "items": {
    "example_mod:steel_longsword": {
      "aliases": [
        "example_mod:ornate_longsword",
        "#example_mod:steel_longsword_variants"
      ],
      "damage_panel": { "thrust": 0.9, "strike": 0.4, "slash": 1.2 }
    }
  }
}
```

### 让武器属于某个类别

连招和部分被动技能不是根据伤害面板判断武器类别，而是读取物品标签或默认分类。请把物品加入对应标签，例如：

```text
data/kingdom_come_combat/tags/item/longswords.json
data/kingdom_come_combat/tags/item/polearms.json
data/kingdom_come_combat/tags/item/fighting_maces.json
```

示例 `longswords.json`：

```json
{
  "replace": false,
  "values": ["example_mod:steel_longsword"]
}
```

现有连招类别包括 `swords`、`short_swords`/`short_sword`、`longswords`、`axes`、`pickaxes`、`hammers` 和 `heavy_weapons`。重武器集合包含重锤、斧和镐。

如果“新武器类型”只是新物品或现有类别的变体，使用标签与武器 JSON 即可。如果需要一个全新的语义类别，例如 `rapiers`，并希望它能出现在 `required_weapon_tag` 或 `condition_weapon_rapier` 中，则还需要修改 Java：

1. 在 `CombatItemUtil`/`CombatWeaponUtil` 中增加类别识别。
2. 在连招武器要求判断中接入新的 `required_weapon_tag` 值。
3. 若被动技能也要识别它，在 `PassiveSkillPerks` 中增加对应条件键。
4. 在 `data/kingdom_come_combat/tags/item/` 增加该类别的标签文件。

## 添加连招

创建 `data/example_mod/kingdom_come_combat/combos/my_combo.json`。一个文件既可以只描述一招，也可以在 `combos` 中放置多招：

```json
{
  "combos": {
    "moon_cut": {
      "display_name": "弦月斩",
      "level": 2,
      "sequence": "下右左",
      "animation": "combo_highspot_cut",
      "required_weapon_tag": "longswords",
      "damage_modifiers": {
        "thrust": 0.0,
        "strike": 0.6,
        "slash": 1.25
      },
      "hit_zones": [
        { "hit": "head", "parts": ["neck", "side_head"] }
      ],
      "impact_multiplier": 1.5,
      "stamina_cost": 25.0,
      "horizontal_knockback": 0.7,
      "blade_trail": true,
      "hit_reaction": true,
      "hit_reaction_interrupts_attack": true,
      "hit_reaction_movement_lock_ticks": 20,
      "suction": true,
      "suction_distance": 2.6,
      "suction_fixed_distance": 1.5,
      "lunge": false,
      "use_real_hitbox": false,
      "injury_type": "head",
      "injury_level": 2,
      "attack_chain": [
        {
          "tick": 5,
          "hit_weapon": true,
          "weapon_clash_sound": "sword_hit_sword"
        },
        {
          "tick": 14,
          "damage_modifiers": { "thrust": 0.0, "strike": 0.6, "slash": 1.25 },
          "hit_zones": [{ "hit": "head", "parts": ["neck"] }]
        }
      ]
    }
  }
}
```

工作方式：玩家每次攻击会记录方向，`sequence` 与方向历史匹配后触发连招。中文方向字符 `左/右/上/下` 可直接使用；动作能否使用还会检查 `level`、武器类别和体力。

常用字段：

- `animation`：攻击者动画名；`victim_animation` 可指定受击者动画。
- `damage_modifiers`：本招的三类伤害倍率。
- `hit_zones`：允许命中的大区域与细分部位。细分 ID 包括 `face`、`neck`、`crown`、`side_head`、`shoulder`、`arm`、`hand`、`chest`、`abdomen`、`thigh`、`knee`、`calf`、`foot`。
- `impact_multiplier`：冲击倍率；兼容代码也接受 `impact`。
- `suction`：是否吸附到目标；距离由 `suction_distance`、`suction_min_distance`、`suction_fixed_distance` 控制。
- `lunge`：是否突进；`use_real_hitbox`：是否使用武器真实碰撞盒。
- `attack_chain`：按动画 tick 安排多段伤害或兵器碰撞。`hit_weapon: true` 只产生武器交击；`weapon_clash_sound` 可使用 `sword_hit_sword`，留空时使用默认交击声。

动画名必须已经存在于客户端动画资源/动画库中。只写一个新的名字不会自动生成动画；可优先复用内置 `default.json` 中已有动画。

## 添加被动技能

创建 `data/example_mod/kingdom_come_combat/passive_skills/my_skills.json`：

```json
{
  "skills": {
    "patient_blade": {
      "name": "耐心之刃",
      "description": "使用长剑时，体力恢复速度提高 15%。",
      "translations": {
        "en_us": {
          "name": "Patient Blade",
          "description": "Regenerate stamina 15% faster while wielding a longsword."
        }
      },
      "icon": "patient_blade",
      "source": "experience",
      "experience_cost": 1200,
      "perks": {
        "condition_weapon_longsword": 1.0,
        "stamina_regen_multiplier": 1.15
      }
    }
  }
}
```

图标放在资源包的：

```text
assets/example_mod/textures/gui/passive_skills/patient_blade.png
```

`icon` 可以写完整资源路径，也可以只写文件名。

### 解锁来源

| `source` | 行为 | 额外字段 |
| --- | --- | --- |
| `experience` | 在技能界面长按学习，消耗战斗经验点数而非等级 | `experience_cost` |
| `advancement` | 完成指定进度后自动学习 | `advancement`，如 `minecraft:adventure/kill_a_mob` |
| `book` | 通过技能书学习 | 需要同 ID 的 `skill_books/*.json` |

技能书最小示例：

```json
{
  "id": "patient_blade",
  "passive_id": "patient_blade",
  "title": "耐心之刃",
  "author": "Swordmaster",
  "first_page": "关于耐心与距离的笔记。",
  "second_page": "翻到此页以领悟技能。"
}
```

被动效果不是任意表达式，而是 `PassiveSkillPerks` 已实现的数值键。常用键包括：

- 条件：`condition_weapon_longsword`、`condition_weapon_sword`、`condition_weapon_short_sword`、`condition_weapon_axe`、`condition_weapon_crossbow`、`condition_combo`、`condition_master_counter`、`condition_mounted`、`condition_offhand_empty`、`condition_offhand_occupied`、`condition_head_hit`、`condition_health_below`、`condition_health_above`、`condition_armor_below`、`condition_armor_above`。
- 通用效果：`damage_multiplier`、`attack_speed_multiplier`、`stamina_cost_multiplier`、`attack_stamina_cost_multiplier`、`stamina_regen_multiplier`、`max_stamina_bonus`、`dodge_stamina_cost_multiplier`、`armor_penalty_multiplier`。
- 事件效果：`kill_stamina_restore`、`combo_stamina_restore`、`combo_damage_multiplier`、`combo_injury_levels`、`master_counter_damage_multiplier`、`master_counter_stamina_restore`。
- 远程效果：`head_hit_damage_multiplier`、`projectile_spread_multiplier`、`projectile_speed_multiplier`。

完整可用组合请参考内置 `passive_skills/default.json`；如果需要全新的触发事件或效果，必须在 `PassiveSkillPerks.java` 中实现其读取和结算逻辑。

## 添加人形战斗 AI

创建 `data/example_mod/kingdom_come_combat/ai/my_mobs.json`：

```json
{
  "entities": {
    "example_mod:mercenary": {
      "min_attack_interval_ticks": [16, 22],
      "attack_desire_per_half_second": [0.40, 0.55],
      "block_chance": 0.75,
      "perfect_block_chance": 0.25,
      "combo_level": 3,
      "combo_plan_chance": 0.55,
      "dodge_chance": 0.06,
      "ai_level": 3,
      "stamina_max": 120.0,
      "stamina_regen_per_tick": 0.34,
      "attack_animation_speed": 0.85,
      "attack_startup_slowdown": 0.4,
      "toughness": 5.0,
      "worn_armor_durability_multiplier": 1.0,
      "follow_up_attack_chance": 0.75,
      "max_follow_up_attacks": 4,
      "keep_distance": false,
      "requires_weapon": true,
      "combat_enter_distance": 7.0,
      "attack_distance": 2.2,
      "approach_distance": 3.2,
      "exhausted_retreat_distance": 4.0
    }
  }
}
```

数值既可以写固定值，也可以写 `[最小值, 最大值]`；区间值会在加载配置时为该配置规格生成随机值。

AI 的核心决策流程是：发现目标并进入战斗距离 → 根据体力和距离移动 → 按攻击欲望与最小间隔发起攻击 → 根据等级决定变向、追击和连招规划 → 按概率格挡、完美格挡或闪避。`combo_plan_chance` 命中时，AI 会从自身 `combo_level` 和当前武器允许的连招中选择一套，并严格执行其 `sequence`。

注意：

- 实体 ID 必须在注册表中存在，否则加载器会忽略该条目。
- `requires_weapon: true` 会让空手实体不启用这套战斗 AI。
- 自动兼容主要覆盖骷髅系、灾厄村民系和服务端可识别的原版 Biped 继承结构；自定义模型实体可能需要在内置 AI 配置的 `automatic_compatibility.include_entities` 中显式加入。
- 动物或异形生物使用独立的 `beast_ai/` 系统，不应直接套用人形 AI；参考 `beast_ai/default.json`、`BeastCombatAiProfiles` 与 `BeastCombatAiTicker`。

## 主要代码原理与位置

| 位置 | 作用 |
| --- | --- |
| `src/main/java/com/kingdomcomecombat/data/CombatDataReloadListener.java` | 数据驱动入口。按数据包优先级扫描 JSON，清空并重建各注册表；负责解析武器、连招、被动技能和 AI |
| `src/main/java/com/kingdomcomecombat/equipment/EquipmentCombatAttributesRegistry.java` | 按物品 ID/标签查询最终装备属性；精确 ID 优先于标签兜底 |
| `src/main/java/com/kingdomcomecombat/equipment/WeaponCombatAttributes.java` | 武器属性数据结构与数值约束 |
| `src/main/java/com/kingdomcomecombat/combat/CombatItemUtil.java` | 剑、长剑、短剑等武器类别判断 |
| `src/main/java/com/kingdomcomecombat/combat/ComboMoveConfigs.java` | 解析方向序列并保存连招定义 |
| `src/main/java/com/kingdomcomecombat/combat/PlayerComboProgress.java` | 玩家连招解锁与进度接口 |
| `src/main/java/com/kingdomcomecombat/combat/ServerComboState.java` | 服务端连招匹配和执行状态 |
| `src/main/java/com/kingdomcomecombat/combat/ServerCombatTicker.java` | 服务端战斗主循环，推进攻击、格挡、体力和连招 |
| `src/main/java/com/kingdomcomecombat/collision/ServerHitDetectionSystem.java` | 服务端命中判定，避免仅依赖客户端结果 |
| `src/main/java/com/kingdomcomecombat/collision/AnimatedAttackHitboxLibrary.java` | 动画驱动的武器碰撞盒配置与计算 |
| `src/main/java/com/kingdomcomecombat/passive/PassiveSkillConfigs.java` | 被动技能注册与查询 |
| `src/main/java/com/kingdomcomecombat/passive/PassiveSkillPerks.java` | 被动技能条件判断和实际效果结算；新增 perk 键的主要入口 |
| `src/main/java/com/kingdomcomecombat/passive/PlayerPassiveSkillProgress.java` | 玩家已解锁被动技能的持久化数据 |
| `src/main/java/com/kingdomcomecombat/ai/HumanoidCombatAiProfiles.java` | 实体类型到人形 AI 配置的注册与默认值 |
| `src/main/java/com/kingdomcomecombat/ai/HumanoidCombatAiTicker.java` | 人形 AI 的移动、攻击、格挡、闪避和连招决策循环 |
| `src/main/java/com/kingdomcomecombat/ai/AiAttackCoordinator.java` | AI 攻击发起与战斗系统之间的协调 |
| `src/main/java/com/kingdomcomecombat/api/KingdomComeCombatApi.java` | 提供给其他代码调用的公共 API |
| `src/client/java/com/kingdomcomecombat/` | 客户端输入、动画、HUD 和渲染；不应承担权威伤害判定 |
| `src/main/java/com/kingdomcomecombat/network/` | 客户端与服务端的攻击输入、动画和状态同步 |

整体数据流如下：

```text
数据包 JSON
    ↓ /reload
CombatDataReloadListener
    ↓
武器 / 连招 / 被动技能 / AI 注册表
    ↓
客户端输入或 AI 决策
    ↓ 网络包
服务端战斗状态与命中判定
    ↓
伤害、体力、硬直、受伤与动画同步
```

服务端是战斗结果的权威端：客户端负责采集输入和播放反馈，实际攻击状态、命中与伤害由服务端推进。扩展 JSON 时通常无需修改网络代码。

## 开发与构建

默认目标为 Minecraft 1.21.7：

```bash
./gradlew build
```

指定其他受支持版本：

```bash
./gradlew -Ptarget_mc=1.21.8 build
./gradlew -Ptarget_mc=1.21.11 build
```

开发客户端：

```bash
./gradlew runClient
```

构建产物位于 `build/libs/`。提交配置前建议至少执行：

```bash
./gradlew processResources
./gradlew compileJava compileClientJava
```

并用 JSON 工具检查新增文件的语法，例如：

```bash
jq empty path/to/file.json
```

## 常见问题

### 执行 `/reload` 后没有生效

检查目录是否为 `data/<namespace>/kingdom_come_combat/<type>/*.json`，物品/实体 ID 是否真实存在，以及游戏日志中是否有 JSON 解析错误。资源包中的图标和动画属于客户端资源，单纯执行服务端 `/reload` 不会凭空添加缺失资源。

### 连招无法触发

依次检查 `sequence`、玩家是否已解锁该连招、武器是否属于 `required_weapon_tag`、连招等级、剩余体力以及动画名。可先复用内置动画排除资源问题。

### 被动技能显示但没有效果

确认 `perks` 键已在 `PassiveSkillPerks.java` 实现。未知键会被保存为数值，但不会自动产生游戏逻辑。

### 第三方生物仍使用原版攻击

确认实体适合人形 AI、实体 ID 已配置、没有被自动兼容排除，并检查服务端的自动人形 AI 兼容选项。完全自定义模型通常需要额外适配。

## 许可证

许可证见 [LICENSE](LICENSE)。
