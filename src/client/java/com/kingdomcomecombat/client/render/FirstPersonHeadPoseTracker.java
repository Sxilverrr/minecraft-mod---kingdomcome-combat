package com.kingdomcomecombat.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Optional;

public final class FirstPersonHeadPoseTracker {
    private static final float HEAD_FACE_CENTER_Y = -4.0F / 16.0F;
    private static final float HEAD_FACE_FRONT_Z = -4.0F / 16.0F;
    private static final ThreadLocal<Capture> ACTIVE = new ThreadLocal<>();
    private static Pose latestPose;
    private static Pose previousPose;
    private static long latestCapturedAtNanos;
    private static long previousCapturedAtNanos;

    private FirstPersonHeadPoseTracker() {
    }

    public static void begin(int entityId, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.world.getEntityById(entityId) == null) {
            return;
        }
        float tickProgress = client.getRenderTickCounter().getTickProgress(true);
        Vec3d entityOrigin = client.world.getEntityById(entityId).getLerpedPos(tickProgress);
        Vec3d renderOrigin = transformedPosition(
                matrices.peek().getPositionMatrix(),
                new Vector3f()
        );
        ACTIVE.set(new Capture(entityId, null, entityOrigin, renderOrigin));
    }

    public static void track(ModelPart head) {
        Capture capture = ACTIVE.get();
        if (capture != null) {
            ACTIVE.set(new Capture(
                    capture.entityId(),
                    head,
                    capture.entityOrigin(),
                    capture.renderOrigin()
            ));
        }
    }

    public static void capture(ModelPart part, MatrixStack matrices) {
        Capture capture = ACTIVE.get();
        MinecraftClient client = MinecraftClient.getInstance();
        if (capture == null || capture.head() != part || client.world == null) {
            return;
        }

        matrices.push();
        part.applyTransform(matrices);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d pivot = capture.entityOrigin().add(
                transformedPosition(matrix, new Vector3f()).subtract(capture.renderOrigin())
        );
        Vec3d faceCenter = capture.entityOrigin().add(transformedPosition(
                matrix,
                new Vector3f(0.0F, HEAD_FACE_CENTER_Y, HEAD_FACE_FRONT_Z)
        ).subtract(capture.renderOrigin()));
        Vec3d forward = transformedDirection(matrix, new Vector3f(0.0F, 0.0F, -1.0F)).normalize();
        Vec3d up = transformedDirection(matrix, new Vector3f(0.0F, -1.0F, 0.0F)).normalize();
        matrices.pop();

        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float pitch = (float) Math.toDegrees(Math.atan2(
                -forward.y,
                Math.sqrt(forward.x * forward.x + forward.z * forward.z)
        ));
        Vec3d right = new Vec3d(
                Math.cos(Math.toRadians(yaw)),
                0.0,
                Math.sin(Math.toRadians(yaw))
        );
        Vec3d unrolledUp = forward.crossProduct(right).normalize();
        float roll = (float) Math.toDegrees(Math.atan2(
                up.dotProduct(right),
                up.dotProduct(unrolledUp)
        ));
        Pose capturedPose = new Pose(
                capture.entityId(), client.world.getTime(), pivot, faceCenter, forward,
                yaw, pitch, roll
        );
        previousPose = latestPose;
        previousCapturedAtNanos = latestCapturedAtNanos;
        latestPose = capturedPose;
        latestCapturedAtNanos = System.nanoTime();
    }

    public static Optional<Pose> get(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null
                || latestPose == null
                || latestPose.entityId() != entityId
                || client.world.getTime() - latestPose.worldTime() > 2L) {
            return Optional.empty();
        }
        if (previousPose == null
                || previousPose.entityId() != entityId
                || previousCapturedAtNanos <= 0L) {
            return Optional.of(latestPose);
        }
        long frameNanos = latestCapturedAtNanos - previousCapturedAtNanos;
        if (frameNanos <= 0L || frameNanos > 100_000_000L) {
            return Optional.of(latestPose);
        }
        long predictionNanos = Math.min(
                Math.max(0L, System.nanoTime() - latestCapturedAtNanos),
                Math.min((long) (frameNanos * 1.25), 50_000_000L)
        );
        double amount = predictionNanos / (double) frameNanos;
        return Optional.of(extrapolate(previousPose, latestPose, amount));
    }

    public static void end() {
        ACTIVE.remove();
    }

    private static Vec3d transformedPosition(Matrix4f matrix, Vector3f point) {
        Vector3f transformed = matrix.transformPosition(point);
        return new Vec3d(transformed.x, transformed.y, transformed.z);
    }

    private static Vec3d transformedDirection(Matrix4f matrix, Vector3f direction) {
        Vector3f transformed = matrix.transformDirection(direction);
        return new Vec3d(transformed.x, transformed.y, transformed.z);
    }

    private static Pose extrapolate(Pose previous, Pose current, double amount) {
        return new Pose(
                current.entityId(),
                current.worldTime(),
                extrapolate(previous.pivot(), current.pivot(), amount),
                extrapolate(previous.faceCenter(), current.faceCenter(), amount),
                extrapolate(previous.forward(), current.forward(), amount).normalize(),
                extrapolateAngle(previous.yaw(), current.yaw(), amount),
                extrapolateAngle(previous.pitch(), current.pitch(), amount),
                extrapolateAngle(previous.roll(), current.roll(), amount)
        );
    }

    private static Vec3d extrapolate(Vec3d previous, Vec3d current, double amount) {
        return current.add(current.subtract(previous).multiply(amount));
    }

    private static float extrapolateAngle(float previous, float current, double amount) {
        float delta = (current - previous + 540.0F) % 360.0F - 180.0F;
        return current + delta * (float) amount;
    }

    private record Capture(
            int entityId,
            ModelPart head,
            Vec3d entityOrigin,
            Vec3d renderOrigin
    ) {
    }

    public record Pose(
            int entityId,
            long worldTime,
            Vec3d pivot,
            Vec3d faceCenter,
            Vec3d forward,
            float yaw,
            float pitch,
            float roll
    ) {
    }
}
