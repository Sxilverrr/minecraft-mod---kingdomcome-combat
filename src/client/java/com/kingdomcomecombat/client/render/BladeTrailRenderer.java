package com.kingdomcomecombat.client.render;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.collision.ClientItemHitboxCache;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

public class BladeTrailRenderer {
    private static final int MAX_POINTS = 10;
    private static final double PIXEL_TO_BLOCK = 1.0 / 16.0;
    private static final double TRAIL_WIDTH = 0.18;
    private static final Deque<Vec3d> POINTS = new ArrayDeque<>();

    private BladeTrailRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(BladeTrailRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || !CombatClientConfig.bladeTrailsEnabled()
                || !CombatAnimationClient.isBladeTrailActive()) {
            POINTS.clear();
            return;
        }

        Vec3d tip = sampleWeaponTip(client);
        if (POINTS.isEmpty() || POINTS.getLast().squaredDistanceTo(tip) > 0.0025) {
            POINTS.addLast(tip);
            while (POINTS.size() > MAX_POINTS) {
                POINTS.removeFirst();
            }
        }

        if (POINTS.size() < 2 || context.consumers() == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        Vec3d camera = context.camera().getPos();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getDebugQuads());
        Vec3d[] points = POINTS.toArray(Vec3d[]::new);
        for (int i = 1; i < points.length; i++) {
            float fromProgress = (i - 1) / (float) (points.length - 1);
            float toProgress = i / (float) (points.length - 1);
            drawSegment(matrices, consumer, points[i - 1], points[i], camera, fromProgress, toProgress);
        }

        matrices.pop();
    }

    private static Vec3d sampleWeaponTip(MinecraftClient client) {
        return sampleRenderedWeaponTip(client).orElseGet(() -> sampleAnimatedWeaponTip(client));
    }

    private static java.util.Optional<Vec3d> sampleRenderedWeaponTip(MinecraftClient client) {
        return ClientItemHitboxCache.get(client.player.getId()).map(box -> renderedBladeTip(client, box));
    }

    private static Vec3d renderedBladeTip(
            MinecraftClient client,
            AnimatedAttackHitboxLibrary.OrientedBox box
    ) {
        Vec3d[] axes = new Vec3d[] {box.axisX(), box.axisY(), box.axisZ()};
        double[] extents = new double[] {
                box.halfExtents().x,
                box.halfExtents().y,
                box.halfExtents().z
        };

        int bladeAxis = 0;
        for (int i = 1; i < extents.length; i++) {
            if (extents[i] > extents[bladeAxis]) {
                bladeAxis = i;
            }
        }

        Vec3d positiveTip = box.center().add(axes[bladeAxis].multiply(extents[bladeAxis]));
        Vec3d negativeTip = box.center().subtract(axes[bladeAxis].multiply(extents[bladeAxis]));
        Vec3d shoulder = client.player.getPos().add(0.0, client.player.getHeight() * 0.72, 0.0);
        return positiveTip.squaredDistanceTo(shoulder) >= negativeTip.squaredDistanceTo(shoulder)
                ? positiveTip
                : negativeTip;
    }

    private static Vec3d sampleAnimatedWeaponTip(MinecraftClient client) {
        Vec3d base = client.player.getPos().add(0.0, client.player.getHeight() * 0.68, 0.0);
        Vec3d tipLocal = new Vec3d(0.0, 0.0, -18.0);
        Vec3d itemPosition = new Vec3d(0.0, 0.0, 0.0);
        GeckoLikeAnimationLibrary.BonePose itemRotation = GeckoLikeAnimationLibrary.BonePose.ZERO;
        var transform = CombatAnimationClient.getLocalPlayerItemTransform();
        if (transform.isPresent()) {
            GeckoLikeAnimationLibrary.BoneTransform item = transform.get();
            if (item.position().isPresent()) {
                GeckoLikeAnimationLibrary.BonePose position = item.position().get();
                itemPosition = new Vec3d(position.x(), position.y(), position.z());
            }

            if (item.rotation().isPresent()) {
                itemRotation = item.rotation().get();
            }
        }

        Vec3d local = rotateLocal(tipLocal, itemRotation).add(itemPosition);
        return base.add(modelToWorld(local.multiply(PIXEL_TO_BLOCK), client.player.getYaw()));
    }

    private static Vec3d rotateLocal(
            Vec3d local,
            GeckoLikeAnimationLibrary.BonePose rotationDegrees
    ) {
        double pitch = Math.toRadians(-rotationDegrees.x());
        double yaw = Math.toRadians(-rotationDegrees.y());
        double roll = Math.toRadians(-rotationDegrees.z());

        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosRoll = Math.cos(roll);
        double sinRoll = Math.sin(roll);

        double rollX = local.x * cosRoll - local.y * sinRoll;
        double rollY = local.x * sinRoll + local.y * cosRoll;
        double rollZ = local.z;

        double pitchX = rollX;
        double pitchY = rollY * cosPitch - rollZ * sinPitch;
        double pitchZ = rollY * sinPitch + rollZ * cosPitch;

        double yawX = pitchX * cosYaw + pitchZ * sinYaw;
        double yawY = pitchY;
        double yawZ = -pitchX * sinYaw + pitchZ * cosYaw;

        return new Vec3d(yawX, yawY, yawZ);
    }

    private static void drawSegment(
            MatrixStack matrices,
            VertexConsumer consumer,
            Vec3d from,
            Vec3d to,
            Vec3d camera,
            float fromProgress,
            float toProgress
    ) {
        Vec3d segment = to.subtract(from);
        if (segment.lengthSquared() <= 0.000001) {
            return;
        }

        Vec3d mid = from.add(to).multiply(0.5);
        Vec3d toCamera = camera.subtract(mid).normalize();
        Vec3d side = segment.normalize().crossProduct(toCamera).normalize().multiply(TRAIL_WIDTH);
        MatrixStack.Entry entry = matrices.peek();

        vertex(consumer, entry, from.add(side), fromProgress);
        vertex(consumer, entry, from.subtract(side), fromProgress);
        vertex(consumer, entry, to.subtract(side), toProgress);
        vertex(consumer, entry, to.add(side), toProgress);
    }

    private static void vertex(
            VertexConsumer consumer,
            MatrixStack.Entry entry,
            Vec3d point,
            float progress
    ) {
        float red = lerp(0.65F, 0.85F, progress);
        float green = lerp(0.90F, 0.98F, progress);
        float blue = 1.0F;
        float alpha = lerp(0.03F, 0.46F, progress);
        consumer.vertex(entry, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .normal(entry, 0.0F, 1.0F, 0.0F);
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static Vec3d modelToWorld(Vec3d local, float yawDegrees) {
        double yawRadians = Math.toRadians(yawDegrees);
        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);
        return new Vec3d(
                local.x * cosYaw + local.z * sinYaw,
                local.y,
                local.x * sinYaw - local.z * cosYaw
        );
    }
}
