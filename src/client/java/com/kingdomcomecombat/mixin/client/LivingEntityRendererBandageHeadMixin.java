package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.item.ModItems;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererBandageHeadMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void kingdomComeCombat$hideHeadBandage(
            T entity,
            S state,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (entity.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.BANDAGE)) {
            state.headItemRenderState.clear();
        }
    }
}
