package com.kingdomcomecombat.client.debug;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.collision.ClientItemHitboxCache;
import com.kingdomcomecombat.client.collision.ClientModelHurtboxCache;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.collision.HumanoidAnimationPoseLibrary;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class CombatCollisionDebugRenderer {
    private static final double HURTBOX_RENDER_DISTANCE = 48.0;
    private static final double HURTBOX_RENDER_DISTANCE_SQUARED =
            HURTBOX_RENDER_DISTANCE * HURTBOX_RENDER_DISTANCE;
    private static final double ATTACK_BOX_RENDER_DISTANCE = 64.0;
    private static final double ATTACK_BOX_RENDER_DISTANCE_SQUARED =
            ATTACK_BOX_RENDER_DISTANCE * ATTACK_BOX_RENDER_DISTANCE;

    private CombatCollisionDebugRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(CombatCollisionDebugRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null
                || client.player == null
                || !client.getEntityRenderDispatcher().shouldRenderHitboxes()
                || context.matrixStack() == null
                || context.consumers() == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        Vec3d cameraPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Box debugBounds = Box.of(
                cameraPos,
                ATTACK_BOX_RENDER_DISTANCE * 2.0,
                ATTACK_BOX_RENDER_DISTANCE * 2.0,
                ATTACK_BOX_RENDER_DISTANCE * 2.0
        );
        for (Entity entity : client.world.getEntitiesByClass(
                Entity.class,
                debugBounds,
                entity -> entity instanceof LivingEntity
        )) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            double distanceSquared = livingEntity.squaredDistanceTo(cameraPos);
            if (distanceSquared <= HURTBOX_RENDER_DISTANCE_SQUARED
                    && context.frustum().isVisible(livingEntity.getBoundingBox().expand(0.75))) {
                renderHurtboxes(matrices, lines, livingEntity);
            }
            if (distanceSquared <= ATTACK_BOX_RENDER_DISTANCE_SQUARED) {
                renderAttackHitbox(matrices, lines, livingEntity);
            }
        }

        matrices.pop();
    }

    private static void renderHurtboxes(
            MatrixStack matrices,
            VertexConsumer lines,
            LivingEntity entity
    ) {
        Iterable<HumanoidHurtboxLibrary.PartBox> hurtboxes =
                animatedHurtboxes(entity);
        for (HumanoidHurtboxLibrary.PartBox hurtbox : hurtboxes) {
            drawOrientedBox(matrices, lines, hurtbox.box(), 1.0F, 0.9F, 0.0F, 1.0F);
        }
    }

    private static Iterable<HumanoidHurtboxLibrary.PartBox> animatedHurtboxes(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == client.player) {
            return ClientModelHurtboxCache.simulateLocalPlayer(entity);
        }

        Optional<java.util.List<HumanoidHurtboxLibrary.PartBox>> modelBoxes =
                ClientModelHurtboxCache.get(entity);
        if (modelBoxes.isPresent()) {
            return modelBoxes.get();
        }

        ClientEntityGeckoAnimationState.ActiveAnimation animation =
                ClientEntityGeckoAnimationState.get(entity.getId());
        if (animation == null) {
            return HumanoidHurtboxLibrary.getHurtboxes(entity);
        }

        if (animation.kind() == GeckoLikeAnimationLibrary.Kind.ATTACK) {
            return HumanoidHurtboxLibrary.getAnimatedHurtboxes(
                    entity,
                    HumanoidAnimationPoseLibrary.Kind.ATTACK,
                    animation.direction(),
                    animation.elapsedSeconds()
            );
        }

        if (animation.kind() == GeckoLikeAnimationLibrary.Kind.STANCE) {
            return HumanoidHurtboxLibrary.getAnimatedHurtboxes(
                    entity,
                    HumanoidAnimationPoseLibrary.Kind.STANCE,
                    animation.direction(),
                    animation.elapsedSeconds()
            );
        }

        return HumanoidHurtboxLibrary.getHurtboxes(entity);
    }

    private static void renderAttackHitbox(
            MatrixStack matrices,
            VertexConsumer lines,
            LivingEntity entity
    ) {
        Optional<AnimatedAttackHitboxLibrary.OrientedBox> localPlayerHitbox =
                sampleLocalPlayerAttack(entity);
        if (localPlayerHitbox.isPresent()) {
            drawOrientedBox(
                    matrices,
                    lines,
                    localPlayerHitbox.get(),
                    1.0F,
                    0.0F,
                    0.0F,
                    1.0F
            );
            return;
        }

        ClientEntityGeckoAnimationState.ActiveAnimation animation =
                ClientEntityGeckoAnimationState.get(entity.getId());

        if (animation == null || animation.kind() != GeckoLikeAnimationLibrary.Kind.ATTACK) {
            return;
        }

        var moveConfig = animation.customAnimationName().isBlank()
                ? AttackMoveConfigs.get(animation.direction())
                : AttackMoveConfigs.getNamed(animation.customAnimationName());
        if (moveConfig == null) {
            moveConfig = AttackMoveConfigs.get(animation.direction());
        }
        boolean useRealHitbox = moveConfig.useRealHitbox();
        (moveConfig.animationName().isBlank()
                ? AnimatedAttackHitboxLibrary.sampleSeconds(
                        animation.direction(),
                        animation.elapsedSeconds(),
                        useRealHitbox
                )
                : AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                        moveConfig.animationName(),
                        animation.elapsedSeconds(),
                        useRealHitbox
                ))
                .flatMap(sample -> realOrSampledBox(entity, sample, useRealHitbox))
                .ifPresent(box -> drawOrientedBox(
                        matrices,
                        lines,
                        box,
                        1.0F,
                        0.0F,
                        0.0F,
                        1.0F
                ));
    }

    private static Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampleLocalPlayerAttack(
            LivingEntity entity
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity != client.player || !CombatClientState.attacking || CombatClientState.lastAttackDirection == null) {
            return Optional.empty();
        }

        String animationName = CombatClientState.currentAttackAnimationName;
        if (animationName != null) {
            var namedMove = AttackMoveConfigs.getNamed(animationName);
            boolean useRealHitbox = namedMove != null
                    ? namedMove.useRealHitbox()
                    : ComboMoveConfigs.findByAnimationName(animationName)
                            .map(ComboMoveConfig::useRealHitbox)
                            .orElse(false);
            return AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                    animationName,
                    CombatClientState.currentAttackElapsedSeconds(),
                    useRealHitbox,
                    EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                            entity.getMainHandStack(),
                            AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
                    ),
                    EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                            entity.getMainHandStack(),
                            AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
                    ),
                    EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                            entity.getMainHandStack(),
                            AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
                    )
            ).flatMap(sample -> realOrSampledBox(entity, sample, useRealHitbox));
        }

        boolean useRealHitbox = AttackMoveConfigs.get(
                CombatClientState.lastAttackDirection
        ).useRealHitbox();
        return AnimatedAttackHitboxLibrary.sampleSeconds(
                CombatClientState.lastAttackDirection,
                CombatClientState.currentAttackElapsedSeconds(),
                useRealHitbox
        ).flatMap(sample -> realOrSampledBox(entity, sample, useRealHitbox));
    }

    private static Optional<AnimatedAttackHitboxLibrary.OrientedBox> realOrSampledBox(
            LivingEntity entity,
            AnimatedAttackHitboxLibrary.SampledHitbox sample,
            boolean useRealHitbox
    ) {
        if (useRealHitbox) {
            Optional<AnimatedAttackHitboxLibrary.OrientedBox> itemBox =
                    ClientItemHitboxCache.get(entity.getId());
            if (itemBox.isPresent()) {
                return itemBox;
            }
        }

        return Optional.of(sample.toWorldBox(entity));
    }

    private static void drawOrientedBox(
            MatrixStack matrices,
            VertexConsumer consumer,
            AnimatedAttackHitboxLibrary.OrientedBox box,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3d[] corners = getCorners(box);

        drawLine(matrices, consumer, corners[0], corners[1], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[1], corners[3], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[3], corners[2], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[2], corners[0], red, green, blue, alpha);

        drawLine(matrices, consumer, corners[4], corners[5], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[5], corners[7], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[7], corners[6], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[6], corners[4], red, green, blue, alpha);

        drawLine(matrices, consumer, corners[0], corners[4], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[1], corners[5], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[2], corners[6], red, green, blue, alpha);
        drawLine(matrices, consumer, corners[3], corners[7], red, green, blue, alpha);
    }

    private static Vec3d[] getCorners(AnimatedAttackHitboxLibrary.OrientedBox box) {
        Vec3d x = box.axisX().multiply(box.halfExtents().x);
        Vec3d y = box.axisY().multiply(box.halfExtents().y);
        Vec3d z = box.axisZ().multiply(box.halfExtents().z);
        Vec3d center = box.center();

        return new Vec3d[]{
                center.subtract(x).subtract(y).subtract(z),
                center.add(x).subtract(y).subtract(z),
                center.subtract(x).add(y).subtract(z),
                center.add(x).add(y).subtract(z),
                center.subtract(x).subtract(y).add(z),
                center.add(x).subtract(y).add(z),
                center.subtract(x).add(y).add(z),
                center.add(x).add(y).add(z)
        };
    }

    private static void drawLine(
            MatrixStack matrices,
            VertexConsumer consumer,
            Vec3d from,
            Vec3d to,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3d normal = to.subtract(from).normalize();
        MatrixStack.Entry entry = matrices.peek();

        consumer.vertex(entry, (float) from.x, (float) from.y, (float) from.z)
                .color(red, green, blue, alpha)
                .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.vertex(entry, (float) to.x, (float) to.y, (float) to.z)
                .color(red, green, blue, alpha)
                .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
