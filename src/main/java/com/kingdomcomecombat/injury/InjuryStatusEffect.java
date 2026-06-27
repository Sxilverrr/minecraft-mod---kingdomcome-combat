package com.kingdomcomecombat.injury;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class InjuryStatusEffect extends StatusEffect {
    public InjuryStatusEffect(int color) {
        super(StatusEffectCategory.HARMFUL, color);
    }
}
