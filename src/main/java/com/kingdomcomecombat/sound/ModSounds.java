package com.kingdomcomecombat.sound;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent BLOCK_WEAPON =
            register("combat.block.weapon");
    public static final SoundEvent BLOCK_PERFECT =
            register("combat.block.perfect");
    public static final SoundEvent BLOCK_SHIELD =
            register("combat.block.shield");
    public static final SoundEvent BLOCK_WOOD =
            register("combat.block.wood");
    public static final SoundEvent WEAPON_HIT_SWORD =
            register("combat.weapon_clash.hit_sword");
    public static final SoundEvent HIT_ARMOR =
            register("combat.hit.armor");
    public static final SoundEvent HIT_ARMOR_PLATE =
            register("combat.hit.armor_plate");
    public static final SoundEvent HIT_HELMET =
            register("combat.hit.helmet");
    public static final SoundEvent HIT_ARMOR_SPARK =
            register("combat.hit.armor_spark");
    public static final SoundEvent HIT_CHAINMAIL_SCRAPE =
            register("combat.hit.chainmail_scrape");
    public static final SoundEvent HIT_POMMEL =
            register("combat.hit.pommel");
    public static final SoundEvent HIT_MACE_BODY =
            register("combat.hit.mace_body");
    public static final SoundEvent HIT_FLESH_1 =
            register("combat.hit.flesh1");
    public static final SoundEvent HIT_FLESH_2 =
            register("combat.hit.flesh2");
    public static final SoundEvent HIT_FLESH_ADD =
            register("combat.hit.flesh_add");
    public static final SoundEvent HIT_STAB_IN =
            register("combat.hit.stab_in");

    private ModSounds() {
    }

    public static void registerAll() {
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
