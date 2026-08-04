package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Hides head equipment models rendered outside vanilla ArmorFeatureRenderer.
 * Custom armor APIs commonly use a BipedEntityModel with the head enabled and
 * both legs disabled. Detecting that shape keeps this independent of mod IDs,
 * renderer implementations, and armor registration APIs.
 */
@Mixin(Model.class)
public class ModelFirstPersonHeadEquipmentMixin {
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
        if (kingdomcomecombat$isGeckoArmorModel(this.getClass())) {
            // GeckoLib armor renderers (including Iron's Spells 'n Spellbooks)
            // may keep their head bone visible while rendering a chest or arm
            // slot. The shape heuristic below would then discard the entire
            // armor piece. ArmorFeatureRenderer/HumanoidArmorLayer already
            // hides the actual HEAD slot during KCC's first-person body pass.
            if (kingdomcomecombat$isHeadSlot(this)) {
                ci.cancel();
            }
            return;
        }

        boolean headEquipmentShape = (biped.head.visible || biped.hat.visible)
                && !biped.leftLeg.visible
                && !biped.rightLeg.visible;
        if (headEquipmentShape) {
            ci.cancel();
        }
    }

    private static boolean kingdomcomecombat$isGeckoArmorModel(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            String name = current.getName();
            if (name.startsWith("io.redspace.ironsspellbooks.")
                    || name.startsWith("software.bernie.geckolib.")
                    || name.endsWith(".GeoArmorRenderer")) {
                return true;
            }
        }
        return false;
    }

    private static boolean kingdomcomecombat$isHeadSlot(Object model) {
        for (Class<?> type = model.getClass(); type != null; type = type.getSuperclass()) {
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
