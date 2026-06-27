package com.kingdomcomecombat.client.collision;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Legacy fallback for versions before ModelPart exposed generic vertex traversal.
 */
public final class ClientGenericModelTracker {
    private ClientGenericModelTracker() {
    }

    public static boolean begin(int entityId, EntityModel<?> model, MatrixStack matrices) {
        return false;
    }

    public static void end(MatrixStack matrices) {
    }

    public static void captureIfHead(ModelPart part, MatrixStack matrices) {
    }
}
