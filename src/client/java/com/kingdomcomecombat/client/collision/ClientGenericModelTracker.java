package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.client.animation.ClientHitReactionState;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public final class ClientGenericModelTracker {
    private static final float MODEL_UNIT_TO_BLOCK = 1.0F / 16.0F;
    private static final String[] HEAD_PART_NAMES = {
            "head", "head_parts", "headparts", "head_part", "skull"
    };
    private static RenderContext context;

    private ClientGenericModelTracker() {
    }

    public static boolean begin(int entityId, EntityModel<?> model, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null
                || !(client.world.getEntityById(entityId) instanceof net.minecraft.entity.LivingEntity entity)
                || HumanoidHurtboxLibrary.isHumanoidTarget(entity)) {
            context = null;
            return false;
        }

        ModelPart head = findHead(model);
        context = new RenderContext(entityId, head);
        matrices.push();
        applyBodyReaction(matrices, ClientHitReactionState.getTorsoDrivenDelta(entityId));
        return true;
    }

    public static void end(MatrixStack matrices) {
        context = null;
        matrices.pop();
    }

    public static void captureIfHead(ModelPart part, MatrixStack matrices) {
        RenderContext current = context;
        if (current == null || current.head() != part) {
            return;
        }

        Set<Vector3f> vertices = new HashSet<>();
        part.collectVertices(matrices, vertices);
        if (vertices.isEmpty()) {
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (Vector3f vertex : vertices) {
            minX = Math.min(minX, vertex.x);
            minY = Math.min(minY, vertex.y);
            minZ = Math.min(minZ, vertex.z);
            maxX = Math.max(maxX, vertex.x);
            maxY = Math.max(maxY, vertex.y);
            maxZ = Math.max(maxZ, vertex.z);
        }

        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        Vec3d min = camera.add(minX, minY, minZ);
        Vec3d max = camera.add(maxX, maxY, maxZ);
        ClientModelHurtboxCache.updateGenericHead(current.entityId(), min, max);
    }

    private static ModelPart findHead(EntityModel<?> model) {
        if (model instanceof ModelWithHead modelWithHead) {
            return modelWithHead.getHead();
        }

        Function<String, ModelPart> parts = model.getRootPart().createPartGetter();
        for (String name : HEAD_PART_NAMES) {
            try {
                ModelPart part = parts.apply(name);
                if (part != null && part != model.getRootPart()) {
                    return part;
                }
            } catch (RuntimeException ignored) {
                // Models without a separately named head keep the generic fallback.
            }
        }
        return null;
    }

    private static void applyBodyReaction(
            MatrixStack matrices,
            ClientHitReactionState.BoneDelta delta
    ) {
        matrices.translate(
                delta.positionX() * MODEL_UNIT_TO_BLOCK,
                -delta.positionY() * MODEL_UNIT_TO_BLOCK,
                delta.positionZ() * MODEL_UNIT_TO_BLOCK
        );
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(delta.rotationZ()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(delta.rotationY()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(delta.rotationX()));
    }

    private record RenderContext(int entityId, ModelPart head) {
    }
}
