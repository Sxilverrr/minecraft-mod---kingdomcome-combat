package com.kingdomcomecombat.injury;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public final class CombatStatusEffect extends StatusEffect {
    public CombatStatusEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }
}
