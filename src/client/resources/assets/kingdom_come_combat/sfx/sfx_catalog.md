# SFX Catalog

本目录当前有 360 个 `.wav`。命名只差尾号的文件按同一用途的随机差分处理，例如 `sword_hit_armor_01` 到 `sword_hit_armor_04` 是同一类音效的 4 个变体。

此文件只整理用途，不移动或重命名原素材。

## 优先接入

### 攻击挥动 / 破风

用于轻击、重击、连招出手阶段。可以按武器重量分层。

- `sword_hit_target_woosh_01` - `06`: 明确的攻击破风，优先用于挥砍判定前后。
- `用所选项目新建的文件夹/whoosh_01` - `03`: 通用破风，适合闪避、快速动作或轻武器。
- `sword_01` - `12`: 泛用剑动作音，可能包含挥动、短摩擦、轻金属声。
- `swordST_01` - `06`: 疑似 stab/thrust 或短促剑动作，可试听后分给刺击或短挥。
- `swordM_01` - `02`: 疑似中等剑动作，可作为重击起手或补层。
- `SFX_04_sword`, `SFX_09_sword`, `SFX_36_sword`, `SFX_37_sword`, `SFX_38_sword`, `SFX_B_sword_19`: 泛用剑类强调音，适合特效层。

### 剑与剑 / 格挡 / 完美格挡

用于武器相撞、完美防御、普通防御、连招被挡。

- `sword_sword_clash43` - `64`: 剑剑撞击主库，适合普通格挡和完美格挡随机播放。
- `sword_hit_sword_01` - `03`: 明确剑击剑，适合轻一些的格挡。
- `sword_sword_clinch3_01` - `09`: 缠剑/贴剑，适合大师反、近距离架住、clinch。
- `sword_sword_clinch_loop03` - `11`: 缠剑循环，适合持续僵持。
- `sword_sword_clinch-out1`, `out3`: 缠剑脱离。
- `sword_sword_unclinch3_01` - `05`: 解缠/脱离，适合格挡回收或推开。
- `sword_sword_slipping3`, `slipping4`: 滑刃，适合非完美格挡、擦开。
- `sword_scrape05`, `07`, `08`, `11`, `14` 和 `sfx_swordscrp_01` - `03`: 金属刮擦，适合刀剑擦过盔甲或挡偏。
- `sword_soft-clink2`, `soft-clink3`: 小金属轻碰，可作 UI/轻触/收招。

### 命中盔甲 / 未穿透 / 火星

用于没有穿透盔甲、打在金属上、盔甲硬直。

- `sword_hit_armor_01` - `04`: 剑击盔甲，优先用于未穿透。
- `sword_plate1_01` - `07`: 剑/金属击板甲，适合重一些的盔甲命中。
- `hammer_hit_helmet_01` - `03`: 钝器击头盔。
- `hammer_hit_helmet_hard_01` - `02`: 更硬的头盔重击。
- `hammer_hit_helmet_soft_01` - `02`: 稍软或轻一点的头盔命中。
- `hammer_hit_helmet_softest_01` - `02`: 最轻的头盔/金属敲击。
- `stone_hit_iron_02`, `03`, `05`: 石/硬物打铁，适合火星或环境金属撞击。
- `sword_iron1_03` - `07`, `sword_iron2_05`: 金属类命中/刮碰，适合补层。

### 命中肉体 / 穿透伤害

用于穿透、真实伤害、血液粒子同步。

- `sword_hit_flesh_01` - `04`: 明确剑击肉体，优先用于斩/刺穿透。
- `sword_flesh1_01` - `05`: 第一组肉体命中。
- `sword_flesh2_01`, `03`, `04`, `06`, `07`: 第二组肉体命中。
- `skin_01` - `03`: 皮肤/肉体轻触，适合轻伤或拳脚。
- `mace_hit_body_01` - `02`: 钝器击身体。
- `SFX_hit_01` - `08`, `SFX_01_hit`, `SFX_22_hit`, `SFX_hit_face`, `SFX_hit_Musa`, `SFX_hit_horse`: 泛用命中或角色受击，可按强度试听筛选。
- `hit_add_01` - `06`: 命中补层/甜味层，适合叠在主要命中音上。
- `soldier_1_hit_01` - `04`, `soldier_2_hit_01` - `04`, `Voice_hit_B`, `erik_hit_01`, `runner_hit_01`: 人声受击，适合实体受击叫声，不建议和每次武器命中强绑定。

### 盾牌 / 木盾 / 木质格挡

用于盾牌、木武器、木质防具。

- `sword_shield_01` - `07`: 剑击盾。
- `sword_short_shield_07` - `13`: 短剑/短促击盾。
- `shield_hit_01` - `03`, `shield_hit_05b`: 盾牌命中。
- `hit_shield-wood1` - `6`: 木盾命中。
- `wooden_sword_wooden_sword_clash21` - `23`: 木剑互击。
- `wooden_sword_wooden_sword_clinch_loop03`, `04`, `10`: 木剑缠斗循环。
- `wooden_sword_wooden_sword_clinch_out1` - `5`: 木剑缠斗脱离。

### 武器抽出 / 收起 / 掉落 / 拿取

用于进入战斗、退出战斗、装备切换、掉落。

- `sword_draw1` - `3`, `SFX_draw_sword_01`, `sword_draw_dramatic`: 拔剑。
- `fight_swordraw_01` - `02`: 可能是快速拔剑/战斗拔刀。
- `sword_take_01` - `05`, `Henry_sword_takev2`, `hands_take_01`: 拿起/握住武器。
- `sword_holster1` - `3`, `sword2_in_01`, `sword2_out_01`: 入鞘/出鞘。
- `sfx_SwordPut_01`, `q_sword_wall_put_03/05/06`, `q_sword_wall_take_03/04`: 武器放置/从墙上取放。
- `sfx_sworddrop_01`, `sword_drop_01`, `sword_fall_01`, `sword_fall_grass_01`: 武器掉落。
- `sword_touch_01`, `sword_soft-clink2/3`: 轻触/小碰撞。
- `SFX_sword_equip_kubenka_01`: 装备剑。

## 可选接入

### 箭矢 / 弩

- `arrow_hit_body_01`: 箭中身体。
- `arrow_hit_wood_01` - `02`: 箭中木头。
- `SFX_crossbow_hit`: 弩命中。

### 钝器 / 拳脚 / 杂击

- `SFX_32_mace_hit`: 锤/钝器命中。
- `hammer_finger_hit_01`: 小钝击或手指/手部击打。
- `stone_hit_kick_01` - `07`: 石/踢击，适合踢门、踢石、脚踢命中。
- `stone_hit_generic_01`, `02`, `04`, `06`, `08`: 石质通用撞击。
- `SFX_Ball_hit`: 球/钝物命中，当前战斗未必需要。

### 环境材质命中

- `sword_wood_20` - `38`: 剑击木。
- `sword_wood_clinch10`, `11`, `13`: 剑木缠/卡住。
- `sword_wood_clinch-out12`, `13`: 剑木脱离。
- `wood_hit_generic_01`, `03`, `05`, `06`, `07`, `09`: 木头通用命中。
- `wood_kick_hit_01` - `02`: 踢木头。
- `sfx_table_hit_01` - `08`: 打桌子/木家具。
- `SFX_door_hit_01` - `02`, `door_hit10`: 门被击中。
- `sword_wicker01` - `04`: 剑击藤编/篮筐。
- `sword_rock1` - `4`: 剑击石头。
- `sword_ceram01` - `06`: 剑击陶瓷。

### 修理 / 磨刀

在子目录 `用所选项目新建的文件夹/` 中，建议之后重命名目录为 `whetstone/` 或把文件提到 `sfx/utility/`。

- `whetstone_sword_loop1` - `4`: 磨刀循环。
- `whetstone_sword_bad_01` - `13`: 磨刀失败/刺耳刮擦。

## 可能不用或暂缓

这些更像通用 foley、人声、动物或旧项目残留，当前战斗核心里可以先不接。

- `cloth_tap_01` - `04`: 布料轻拍。
- `hands_01` - `03`: 手部 foley。
- `plate_tap_01` - `02`: 盘子/板材轻敲。
- `raven_wings_hitreaction_01`: 乌鸦翅膀/受击反应，像杂项。
- `foley_StUpGrass_01`: 草地起身/脚步。
- `mose_foley_sword_01`, `Mose_sword_01`, `cert_SwordPick_01`, `Soldier_SwordAttach_01`, `Soldier_SwordAttFol_01`: 文件名带角色/临时动作，需试听后再定。
- `.DS_Store`: macOS 元数据，应该删除。
- `kcd.assets[0].txt`: 原始素材清单，不是游戏音效。

## 建议事件映射

- `kcc.attack.swing.light`: `sword_hit_target_woosh_*`, `whoosh_*`, `sword_01-12`
- `kcc.attack.swing.heavy`: `swordM_*`, `SFX_*_sword`, `sword_hit_target_woosh_*`
- `kcc.hit.flesh`: `sword_hit_flesh_*`, `sword_flesh1_*`, `sword_flesh2_*`
- `kcc.hit.armor.blocked`: `sword_hit_armor_*`, `sword_plate1_*`, `stone_hit_iron_*`
- `kcc.hit.armor.spark`: 同 `kcc.hit.armor.blocked`，可叠火星粒子。
- `kcc.block.weapon`: `sword_sword_clash*`, `sword_hit_sword_*`
- `kcc.block.perfect`: 更响亮的 `sword_sword_clash58-64`，再叠 `sword_scrape*`
- `kcc.block.shield`: `sword_shield_*`, `sword_short_shield_*`, `shield_hit_*`
- `kcc.clinch.start`: `sword_sword_clinch3_*`
- `kcc.clinch.loop`: `sword_sword_clinch_loop*`
- `kcc.clinch.end`: `sword_sword_unclinch3_*`, `sword_sword_clinch-out*`
- `kcc.weapon.draw`: `sword_draw*`, `SFX_draw_sword_01`, `fight_swordraw_*`
- `kcc.weapon.sheath`: `sword_holster*`, `sword2_in_01`
- `kcc.weapon.drop`: `sword_drop_01`, `sfx_sworddrop_01`, `sword_fall_*`
- `kcc.arrow.hit.body`: `arrow_hit_body_01`
- `kcc.arrow.hit.wood`: `arrow_hit_wood_*`

