package com.kingdomcomecombat.mixin.client;

import net.minecraft.client.MinecraftClient;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/** Covers modded 1.21.1 helmet features that render a head-only biped model directly. */
@Mixin(value = AnimalModel.class, priority = 1100)
public abstract class LegacyAnimalModelFirstPersonHeadEquipmentMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kingdomcomecombat$hideCustomFirstPersonHeadEquipment(
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            int color,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean localFirstPerson = client.player != null
                && client.options.getPerspective().isFirstPerson()
                && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen);
        if (!(FirstPersonBodyRenderOffsetContext.isRenderingLocalFirstPersonBody() || localFirstPerson)
                || FirstPersonRenderCompat.isRenderingShadowPass()
                || !((Object) this instanceof BipedEntityModel<?> biped)) {
            return;
        }

        boolean headEquipmentShape = (biped.head.visible || biped.hat.visible)
                && !biped.leftLeg.visible
                && !biped.rightLeg.visible;
        if (headEquipmentShape) {
            ci.cancel();
            return;
        }
        if (kingdomcomecombat$isGeoArmorHeadSlot(this)) {
            ci.cancel();
        }
    }

    private static boolean kingdomcomecombat$isGeoArmorHeadSlot(Object model) {
        boolean geoArmor = false;
        for (Class<?> type = model.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals("software.bernie.geckolib.renderer.GeoArmorRenderer")) {
                geoArmor = true;
            }
            if (!geoArmor) continue;
            for (String name : new String[]{"currentSlot", "equipmentSlot"}) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(model) == net.minecraft.entity.EquipmentSlot.HEAD;
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }
        return false;
    }
}
