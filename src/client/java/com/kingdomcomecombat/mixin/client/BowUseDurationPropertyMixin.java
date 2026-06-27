package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.equipment.BowDrawSpeedState;
import net.minecraft.client.render.item.property.numeric.UseDurationProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(UseDurationProperty.class)
public class BowUseDurationPropertyMixin {
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$syncBowPullModel(
            ItemStack stack,
            ClientWorld world,
            LivingEntity user,
            int seed,
            CallbackInfoReturnable<Float> cir
    ) {
        if (world == null || user == null || !(stack.getItem() instanceof BowItem)) {
            return;
        }
        cir.setReturnValue(cir.getReturnValueF()
                * BowDrawSpeedState.getDrawSpeed(stack, user, world));
    }
}
