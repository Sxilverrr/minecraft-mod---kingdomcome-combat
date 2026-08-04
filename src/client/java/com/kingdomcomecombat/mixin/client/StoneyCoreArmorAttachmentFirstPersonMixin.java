package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides StoneyCore under-armour and attachment passes omitted by vanilla armour hooks. */
@Pseudo
@Mixin(targets = "banduty.stoneycore.client.render.UnderArmourRenderer", remap = false)
public abstract class StoneyCoreArmorAttachmentFirstPersonMixin {
    @Inject(
            method = {"renderBaseArmor", "renderAttachments"},
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void kingdomcomecombat$hideHiddenPassArmor(
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            ItemStack stack,
            LivingEntity entity,
            int light,
            BipedEntityModel<?> model,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch,
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
        if (!localFirstPerson || FirstPersonRenderCompat.isRenderingShadowPass()) {
            return;
        }

        if (CombatClientConfig.firstPersonWeaponOnly()
                || ItemStack.areEqual(entity.getEquippedStack(EquipmentSlot.HEAD), stack)) {
            ci.cancel();
        }
    }
}
