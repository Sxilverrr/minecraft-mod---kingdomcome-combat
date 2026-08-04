package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides vanilla and vanilla-compatible helmet features in the 1.21.1 entity renderer. */
@Mixin(value = ArmorFeatureRenderer.class, priority = 1100)
public abstract class LegacyArmorFeatureRendererFirstPersonBodyMixin {
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$hideFirstPersonHelmet(
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            BipedEntityModel<?> model,
            CallbackInfo ci
    ) {
        if (CollisionOnlyRenderContext.isActive()) {
            ci.cancel();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        boolean localFirstPerson = FirstPersonBodyRenderOffsetContext.isRenderingLocalFirstPersonBody()
                || (entity == client.player
                && client.currentScreen == null
                && client.options.getPerspective().isFirstPerson());
        if ((slot == EquipmentSlot.HEAD || CombatClientConfig.firstPersonWeaponOnly())
                && localFirstPerson
                && !FirstPersonRenderCompat.isRenderingShadowPass()) {
            ci.cancel();
        }
    }
}
