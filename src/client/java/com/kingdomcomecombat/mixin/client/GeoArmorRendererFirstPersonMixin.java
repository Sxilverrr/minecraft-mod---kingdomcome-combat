package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Cancels head-slot GeckoLib armor at GeckoArmorRenderer's own render override. */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoArmorRenderer", remap = false)
public abstract class GeoArmorRendererFirstPersonMixin {
    @Inject(method = {"method_2828", "renderToBuffer", "render"}, at = @At("HEAD"),
            cancellable = true, require = 0, remap = false)
    private void kingdomcomecombat$hideFirstPersonGeoHelmet(
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            int color,
            CallbackInfo ci
    ) {
        if (CollisionOnlyRenderContext.isActive()) {
            ci.cancel();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || !client.options.getPerspective().isFirstPerson()
                || FirstPersonRenderCompat.isRenderingShadowPass()
                || client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
            return;
        }
        Object renderer = this;
        if (kingdomcomecombat$get(renderer, "getCurrentEntity", "currentEntity") != client.player
                || kingdomcomecombat$get(renderer, "getCurrentSlot", "currentSlot") != EquipmentSlot.HEAD) {
            return;
        }
        ci.cancel();
    }

    private static Object kingdomcomecombat$get(Object target, String getterName, String fieldName) {
        try {
            Method getter = target.getClass().getMethod(getterName);
            return getter.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }
}
