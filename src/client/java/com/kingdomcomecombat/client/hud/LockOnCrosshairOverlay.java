package com.kingdomcomecombat.client.hud;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.combat.ClientExecutionState;
import com.kingdomcomecombat.client.game.ClientGameRuleState;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.stamina.ClientEntityStaminaState;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class LockOnCrosshairOverlay {
    private static final Identifier CROSS_BASE =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_base.png");

    private static final Identifier CROSS_LEFT =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_left.png");

    private static final Identifier CROSS_RIGHT =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_right.png");

    private static final Identifier CROSS_UP =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_up.png");

    private static final Identifier CROSS_DOWN =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_down.png");
    private static final Identifier CROSS_WITHOUT_UP =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_without_up.png");
    private static final Identifier ATTACK_WARNING =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/defend.png");
    private static final Identifier DIRECTIONAL_BLOCK_WARNING =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/direction_block.png");
    private static final Identifier CANNOT_BLOCK_WARNING =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cant_block.png");
    private static final Identifier PERFECT_BLOCK_WARNING =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/perfect_block.png");
    private static final Identifier PERFECT_COUNTER_ATTACK =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/attack.png");
    private static final Identifier EXECUTE_READY =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/execute.png");
    private static final Identifier HARDCORE_LOCK_POINT =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/hardcore_lock_point.png");

    private static final int CROSS_SIZE = 32;
    private static final float LOCKED_BASE_CROSS_SCALE = 0.60F;
    private static final int TARGET_STAMINA_RING_COLOR = 0xFFFFD94A;
    private static final double DISTANCE_SCALE_START = 4.0;
    private static final double DISTANCE_SCALE_RANGE = 12.0;
    private static final float MIN_DISTANCE_CROSS_SCALE = 0.32F;

    private static boolean hasScreenPosition = false;
    private static float screenX = 0.0F;
    private static float screenY = 0.0F;
    private static float crossScale = LOCKED_BASE_CROSS_SCALE;

    public static void register() {
        WorldRenderEvents.LAST.register(context -> updateTargetScreenPosition());
        HudRenderCallback.EVENT.register(LockOnCrosshairOverlay::renderHud);
    }

    private static void updateTargetScreenPosition() {
        MinecraftClient client = MinecraftClient.getInstance();

        hasScreenPosition = false;

        if (client.world == null || client.player == null) {
            return;
        }

        if (!LockOnState.locked) {
            return;
        }

        if (LockOnState.isSoftLocked()) {
            screenX = client.getWindow().getScaledWidth() * 0.5F;
            screenY = client.getWindow().getScaledHeight() * 0.5F;
            crossScale = LOCKED_BASE_CROSS_SCALE;
            hasScreenPosition = true;
            return;
        }

        if (LockOnState.targetEntityId < 0) {
            return;
        }

        Entity target = client.world.getEntityById(LockOnState.targetEntityId);

        if (target == null || !target.isAlive()) {
            return;
        }

        updateCrossScale(client, target);

        float tickDelta = client.getRenderTickCounter().getTickProgress(true);

        Vec3d targetPos = target.getLerpedPos(tickDelta)
                .add(0.0, target.getHeight() * 0.68, 0.0);

        projectWorldToScreen(client, targetPos);
    }

    private static void projectWorldToScreen(MinecraftClient client, Vec3d worldPos) {
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d relative = worldPos.subtract(cameraPos);

        // Use Minecraft's yaw/pitch convention directly. Camera quaternion
        // orientation changed between the legacy and render-state pipelines,
        // while yaw 0 always looks south (+Z) and positive yaw turns west.
        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());
        double sinYaw = Math.sin(yaw);
        double cosYaw = Math.cos(yaw);
        double sinPitch = Math.sin(pitch);
        double cosPitch = Math.cos(pitch);

        float cameraX = (float) (-cosYaw * relative.x - sinYaw * relative.z);
        float cameraY = (float) (
                -sinYaw * sinPitch * relative.x
                        + cosPitch * relative.y
                        + cosYaw * sinPitch * relative.z);
        float depth = (float) (
                -sinYaw * cosPitch * relative.x
                        - sinPitch * relative.y
                        + cosYaw * cosPitch * relative.z);
        if (depth <= 0.01F) {
            hasScreenPosition = false;
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float tanHalfFov = (float) Math.tan(Math.toRadians(
                client.options.getFov().getValue() * 0.5));
        float aspect = width / (float) Math.max(1, height);
        float ndcX = cameraX / (depth * tanHalfFov * aspect);
        float ndcY = cameraY / (depth * tanHalfFov);

        if (ndcX < -1.2F || ndcX > 1.2F || ndcY < -1.2F || ndcY > 1.2F) {
            hasScreenPosition = false;
            return;
        }

        screenX = (ndcX * 0.5F + 0.5F) * width;
        screenY = (1.0F - (ndcY * 0.5F + 0.5F)) * height;

        hasScreenPosition = true;
    }

    private static void updateCrossScale(MinecraftClient client, Entity target) {
        double distance = client.player.distanceTo(target);

        if (distance <= DISTANCE_SCALE_START) {
            crossScale = LOCKED_BASE_CROSS_SCALE;
            return;
        }

        double distanceProgress = (distance - DISTANCE_SCALE_START) / DISTANCE_SCALE_RANGE;
        float distanceScale = (float) (1.0 - Math.min(1.0, distanceProgress) * 0.45);

        crossScale = Math.max(
                MIN_DISTANCE_CROSS_SCALE,
                LOCKED_BASE_CROSS_SCALE * distanceScale
        );
    }

    private static void renderHud(
            DrawContext context,
            RenderTickCounter tickCounter
    ) {
        updateTargetScreenPosition();
        if (!LockOnState.locked) {
            return;
        }

        if (ClientGameRuleState.hardcoreMode()) {
            if (!ClientExecutionState.shouldHideCrosshair()) {
                int x = (context.getScaledWindowWidth() - CROSS_SIZE) / 2;
                int y = (context.getScaledWindowHeight() - CROSS_SIZE) / 2;
                drawTexture(context, HARDCORE_LOCK_POINT, x, y, CROSS_SIZE);
            }
            return;
        }

        if (!CombatClientConfig.showLockOnCrosshair()) {
            return;
        }

        if (!hasScreenPosition) {
            return;
        }

        if (ClientExecutionState.shouldHideCrosshair()) {
            return;
        }

        int drawSize = Math.max(4, Math.round(
                CROSS_SIZE * crossScale * (float) CombatClientConfig.lockOnCrosshairScale()));
        int x = Math.round(screenX - drawSize / 2.0F);
        int y = Math.round(screenY - drawSize / 2.0F);

        MinecraftClient client = MinecraftClient.getInstance();
        drawTargetStaminaRing(context, x, y, drawSize);
        boolean polearm = client.player != null
                && CombatItemUtil.isPolearm(client.player.getMainHandStack());
        drawTexture(context, polearm ? CROSS_WITHOUT_UP : CROSS_BASE, x, y, drawSize);

        Identifier stanceTexture = getStanceTexture(CombatClientState.currentDirection);
        if (stanceTexture != null && !(polearm && CombatClientState.currentDirection == CombatDirection.UP)) {
            drawTexture(context, stanceTexture, x, y, drawSize);
        }

        if (IncomingAttackWarningState.active()
                && !CombatClientState.isPerfectCounterIndicatorActive()) {
            int warningSize = Math.round(drawSize * 1.35F);
            Identifier warningTexture = switch (IncomingAttackWarningState.warningType()) {
                case 1 -> CANNOT_BLOCK_WARNING;
                case 2 -> PERFECT_BLOCK_WARNING;
                case 3 -> ATTACK_WARNING;
                default -> DIRECTIONAL_BLOCK_WARNING;
            };
            drawTexture(
                    context,
                    warningTexture,
                    Math.round(screenX - warningSize / 2.0F),
                    Math.round(screenY - warningSize / 2.0F),
                    warningSize
            );
        }
        if (CombatClientState.isPerfectCounterIndicatorActive()) {
            int indicatorSize = Math.round(drawSize * 1.35F);
            drawTexture(
                    context,
                    PERFECT_COUNTER_ATTACK,
                    Math.round(screenX - indicatorSize / 2.0F),
                    Math.round(screenY - indicatorSize / 2.0F),
                    indicatorSize
            );
        }
        if (ClientExecutionState.canExecuteTarget(LockOnState.targetEntityId)) {
            int indicatorSize = Math.round(drawSize * 1.35F);
            drawTexture(
                    context,
                    EXECUTE_READY,
                    Math.round(screenX - indicatorSize / 2.0F),
                    Math.round(screenY - indicatorSize / 2.0F),
                    indicatorSize
            );
        }
    }

    private static void drawTargetStaminaRing(
            DrawContext context,
            int crossX,
            int crossY,
            int crossDrawSize
    ) {
        if (!CombatClientConfig.showTargetStaminaRing()
                || LockOnState.isSoftLocked()
                || LockOnState.targetEntityId < 0) {
            return;
        }

        float progress = ClientEntityStaminaState.progressOrFull(LockOnState.targetEntityId);
        if (progress <= 0.01F) {
            return;
        }

        float textureCenter = (CROSS_SIZE - 1.0F) * 0.5F;
        float radius = (CROSS_SIZE * (float) CombatClientConfig.targetStaminaRingScale() - 1.0F) * 0.5F;
        float minDistance = Math.max(0.0F, radius - 0.55F);
        float maxDistance = radius + 0.55F;
        float minDistanceSq = minDistance * minDistance;
        float maxDistanceSq = maxDistance * maxDistance;
        int minPixel = (int) Math.floor(textureCenter - maxDistance);
        int maxPixel = (int) Math.ceil(textureCenter + maxDistance);

        for (int texturePixelY = minPixel; texturePixelY <= maxPixel; texturePixelY++) {
            for (int texturePixelX = minPixel; texturePixelX <= maxPixel; texturePixelX++) {
                float dx = texturePixelX - textureCenter;
                float dy = texturePixelY - textureCenter;
                float distanceSq = dx * dx + dy * dy;
                if (distanceSq < minDistanceSq || distanceSq > maxDistanceSq) {
                    continue;
                }

                double angle = Math.atan2(dy, dx);
                float normalized = (float) ((angle + Math.PI * 0.5 + Math.PI * 2.0) % (Math.PI * 2.0)
                        / (Math.PI * 2.0));
                if (normalized > progress) {
                    continue;
                }

                int x0 = texturePixelToScreen(crossX, crossDrawSize, texturePixelX);
                int y0 = texturePixelToScreen(crossY, crossDrawSize, texturePixelY);
                int x1 = texturePixelToScreen(crossX, crossDrawSize, texturePixelX + 1);
                int y1 = texturePixelToScreen(crossY, crossDrawSize, texturePixelY + 1);
                context.fill(x0, y0, Math.max(x0 + 1, x1), Math.max(y0 + 1, y1), TARGET_STAMINA_RING_COLOR);
            }
        }
    }

    private static int texturePixelToScreen(int origin, int drawSize, int texturePixel) {
        return origin + Math.round(texturePixel * drawSize / (float) CROSS_SIZE);
    }

    private static Identifier getStanceTexture(CombatDirection direction) {
        return switch (direction) {
            case LEFT -> CROSS_LEFT;
            case RIGHT -> CROSS_RIGHT;
            case UP -> CROSS_UP;
            case DOWN -> CROSS_DOWN;
        };
    }

    private static void drawTexture(
            DrawContext context,
            Identifier texture,
            int x,
            int y,
            int drawSize
    ) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                0.0F,
                0.0F,
                drawSize,
                drawSize,
                CROSS_SIZE,
                CROSS_SIZE,
                CROSS_SIZE,
                CROSS_SIZE
        );
    }
}
