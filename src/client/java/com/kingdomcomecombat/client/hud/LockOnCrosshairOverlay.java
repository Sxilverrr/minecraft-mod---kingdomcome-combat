package com.kingdomcomecombat.client.hud;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.game.ClientGameRuleState;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.combat.CombatDirection;
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
import org.joml.Matrix4f;
import org.joml.Vector4f;

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
    private static final Identifier ATTACK_WARNING =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/defend.png");
    private static final Identifier CANNOT_BLOCK_WARNING =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cant_block.png");
    private static final Identifier PERFECT_BLOCK_WARNING =
            Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/perfect_block.png");

    private static final int CROSS_SIZE = 32;
    private static final float LOCKED_BASE_CROSS_SCALE = 0.60F;
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

        Matrix4f projectionMatrix = client.gameRenderer.getBasicProjectionMatrix(
                client.options.getFov().getValue()
        );

        Matrix4f viewMatrix = new Matrix4f()
                .rotateX((float) Math.toRadians(camera.getPitch()))
                .rotateY((float) Math.toRadians(camera.getYaw() + 180.0F));

        Vector4f clip = new Vector4f(
                (float) relative.x,
                (float) relative.y,
                (float) relative.z,
                1.0F
        );

        clip.mul(viewMatrix);
        clip.mul(projectionMatrix);

        if (clip.w <= 0.0F) {
            hasScreenPosition = false;
            return;
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        if (ndcX < -1.2F || ndcX > 1.2F || ndcY < -1.2F || ndcY > 1.2F) {
            hasScreenPosition = false;
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

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
        if (!LockOnState.locked || ClientGameRuleState.hardcoreMode()) {
            return;
        }

        if (!CombatClientConfig.showLockOnCrosshair()) {
            return;
        }

        if (!hasScreenPosition) {
            return;
        }

        int drawSize = Math.max(8, Math.round(CROSS_SIZE * crossScale));
        int x = Math.round(screenX - drawSize / 2.0F);
        int y = Math.round(screenY - drawSize / 2.0F);

        drawTexture(context, CROSS_BASE, x, y, drawSize);

        Identifier stanceTexture = getStanceTexture(CombatClientState.currentDirection);

        if (stanceTexture != null) {
            drawTexture(context, stanceTexture, x, y, drawSize);
        }

        if (IncomingAttackWarningState.active()) {
            int warningSize = Math.round(drawSize * 1.35F);
            Identifier warningTexture = switch (IncomingAttackWarningState.warningType()) {
                case 1 -> CANNOT_BLOCK_WARNING;
                case 2 -> PERFECT_BLOCK_WARNING;
                default -> ATTACK_WARNING;
            };
            drawTexture(
                    context,
                    warningTexture,
                    Math.round(screenX - warningSize / 2.0F),
                    Math.round(screenY - warningSize / 2.0F),
                    warningSize
            );
        }
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
