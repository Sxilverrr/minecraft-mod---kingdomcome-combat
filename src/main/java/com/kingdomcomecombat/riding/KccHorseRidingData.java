package com.kingdomcomecombat.riding;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public interface KccHorseRidingData {
    double kingdomcomecombat$getHorseMaxStamina();

    double kingdomcomecombat$getHorseStamina();

    double kingdomcomecombat$getHorseCurrentSpeed();

    Vec2f kingdomcomecombat$getControlledRotation(LivingEntity passenger);

    Vec3d kingdomcomecombat$getControlledMovementInput(PlayerEntity player);

    float kingdomcomecombat$modifySaddledSpeed(PlayerEntity player, float vanillaSpeed);
}
