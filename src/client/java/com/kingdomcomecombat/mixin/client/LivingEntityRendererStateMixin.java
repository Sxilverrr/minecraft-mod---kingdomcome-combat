package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.feedback.CustomHurtOverlaySuppressor;
import com.kingdomcomecombat.client.mixin.EntityRenderStateKccAccess;
import com.kingdomcomecombat.equipment.BloodiedEntityAccess;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererStateMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void kingdomcomecombat$storeEntityId(
            LivingEntity entity,
            LivingEntityRenderState state,
            float tickProgress,
            CallbackInfo ci
    ) {
        ((EntityRenderStateKccAccess) state).kingdomcomecombat$setEntityId(entity.getId());
        ((EntityRenderStateKccAccess) state).kingdomcomecombat$setHandStacks(
                entity.getMainHandStack(),
                entity.getOffHandStack()
        );
        ((EntityRenderStateKccAccess) state).kingdomcomecombat$setArmorStacks(
                entity.getEquippedStack(EquipmentSlot.HEAD),
                entity.getEquippedStack(EquipmentSlot.CHEST)
        );
        ((EntityRenderStateKccAccess) state).kingdomcomecombat$setBodyBloodPercent(
                entity instanceof BloodiedEntityAccess bloodied
                        ? bloodied.kingdomcomecombat$getBodyBloodPercent()
                        : 0.0
        );
        if (CustomHurtOverlaySuppressor.shouldSuppress(entity.getId())) {
            state.hurt = false;
        }
    }
}
