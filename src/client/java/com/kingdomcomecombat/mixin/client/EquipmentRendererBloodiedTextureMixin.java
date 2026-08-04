package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.BloodiedTextureCache;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.render.ArmorEntityRenderContext;
import net.minecraft.client.MinecraftClient;
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
            at = @At("HEAD"),
            cancellable = true
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
        if (kingdomcomecombat$shouldHideFirstPersonHelmet(stack)) {
            ci.cancel();
            return;
        }
        kingdomcomecombat$renderedArmorStack.set(stack);
    }

    private static boolean kingdomcomecombat$shouldHideFirstPersonHelmet(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || !ArmorEntityRenderContext.isRendering(client.player.getId())
                || client.currentScreen != null
                || !client.options.getPerspective().isFirstPerson()
                || !(FirstPersonRenderCompat.isExternalBodyRender()
                || CombatAnimationClient.isKccSpecialFirstPersonActive())) {
            return false;
        }
        ItemStack helmet = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        return !helmet.isEmpty() && stack.getItem() == helmet.getItem();
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getArmorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private Identifier kingdomcomecombat$useBloodiedArmorTexture(Identifier texture) {
        // Inventory and other GUI entity previews render armor through a
        // separate immediate-buffer/glint path. Registering and substituting a
        // dynamic equipment texture there can re-enter armor buffer selection
        // (ItemRenderer#getArmorGlintConsumer on 1.21.8). Keep GUI previews on
        // the vanilla texture; world rendering still receives blood and holes.
        if (MinecraftClient.getInstance().currentScreen != null) {
            return texture;
        }
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
