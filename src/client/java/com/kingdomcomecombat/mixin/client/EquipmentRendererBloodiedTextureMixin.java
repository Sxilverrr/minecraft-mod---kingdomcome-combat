package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.BloodiedTextureCache;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentRenderer.class)
public class EquipmentRendererBloodiedTextureMixin {
    private static final ThreadLocal<ItemStack> kingdomcomecombat$renderedArmorStack = new ThreadLocal<>();

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At("HEAD")
    )
    private void kingdomcomecombat$captureArmorStack(
            net.minecraft.client.render.entity.equipment.EquipmentModel.LayerType layerType,
            net.minecraft.registry.RegistryKey<net.minecraft.item.equipment.EquipmentAsset> asset,
            net.minecraft.client.model.Model model,
            ItemStack stack,
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider vertexConsumers,
            int light,
            Identifier texture,
            CallbackInfo ci
    ) {
        kingdomcomecombat$renderedArmorStack.set(stack);
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getArmorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private Identifier kingdomcomecombat$useBloodiedArmorTexture(Identifier texture) {
        ItemStack stack = kingdomcomecombat$renderedArmorStack.get();
        if (!BloodiedTextureCache.shouldUseEquipmentTexture(stack)) {
            return texture;
        }
        return BloodiedTextureCache.getEquipmentTexture(texture, stack);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At("RETURN")
    )
    private void kingdomcomecombat$clearArmorStack(
            net.minecraft.client.render.entity.equipment.EquipmentModel.LayerType layerType,
            net.minecraft.registry.RegistryKey<net.minecraft.item.equipment.EquipmentAsset> asset,
            net.minecraft.client.model.Model model,
            ItemStack stack,
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider vertexConsumers,
            int light,
            Identifier texture,
            CallbackInfo ci
    ) {
        kingdomcomecombat$renderedArmorStack.remove();
    }
}
