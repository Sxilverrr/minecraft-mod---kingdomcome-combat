package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.BloodiedTextureCache;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class LegacyArmorFeatureRendererBloodiedTextureMixin {
    @Unique
    private final ThreadLocal<ItemStack> kingdomcomecombat$armorStack = new ThreadLocal<>();

    @Inject(method = "renderArmor", at = @At("HEAD"))
    private void kingdomcomecombat$captureArmor(
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider vertices,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            net.minecraft.client.render.entity.model.BipedEntityModel<?> model,
            CallbackInfo ci
    ) {
        kingdomcomecombat$armorStack.set(entity.getEquippedStack(slot));
    }

    @ModifyArg(
            method = "renderArmorParts",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getArmorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;")
    )
    private Identifier kingdomcomecombat$useDamagedArmorTexture(Identifier texture) {
        if (net.minecraft.client.MinecraftClient.getInstance().currentScreen != null) {
            return texture;
        }
        ItemStack stack = kingdomcomecombat$armorStack.get();
        return BloodiedTextureCache.shouldUseEquipmentTexture(stack)
                ? BloodiedTextureCache.getEquipmentTexture(texture, stack) : texture;
    }

    @Inject(method = "renderArmor", at = @At("RETURN"))
    private void kingdomcomecombat$clearArmor(
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider vertices,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            net.minecraft.client.render.entity.model.BipedEntityModel<?> model,
            CallbackInfo ci
    ) {
        kingdomcomecombat$armorStack.remove();
    }
}
