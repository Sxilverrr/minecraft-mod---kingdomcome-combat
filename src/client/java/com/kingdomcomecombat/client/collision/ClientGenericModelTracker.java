package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.client.animation.ClientHitReactionState;
import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.VanillaSkeletonGeckoAnimationApplier;
import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class ClientGenericModelTracker {
    private static final float MODEL_UNIT_TO_BLOCK = 1.0F / 16.0F;
    private static final String[] HEAD_PART_NAMES = {
            "head", "head_parts", "headparts", "head_part", "skull"
    };
    private static final String[] BODY_PART_NAMES = {
            "body", "torso", "chest", "upper_body", "upperbody"
    };
    private static final String[] RIGHT_ARM_PART_NAMES = {
            "rightArm", "right_arm", "rightarm", "arm_right", "r_arm", "right_hand"
    };
    private static final String[] LEFT_ARM_PART_NAMES = {
            "leftArm", "left_arm", "leftarm", "arm_left", "l_arm", "left_hand"
    };
    private static final String[] RIGHT_LEG_PART_NAMES = {
            "rightLeg", "right_leg", "rightleg", "leg_right", "r_leg", "right_foot"
    };
    private static final String[] LEFT_LEG_PART_NAMES = {
            "leftLeg", "left_leg", "leftleg", "leg_left", "l_leg", "left_foot"
    };
    private static RenderContext context;
    private static final IdentityHashMap<EntityModel<?>, GenericHumanoidParts> GENERIC_PARTS_CACHE = new IdentityHashMap<>();
    private static final Set<EntityModel<?>> NON_HUMANOID_MODEL_CACHE = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private ClientGenericModelTracker() {
    }

    public static void clearModelClassificationCache() {
        GENERIC_PARTS_CACHE.clear();
        NON_HUMANOID_MODEL_CACHE.clear();
    }

    public static boolean begin(int entityId, EntityModel<?> model, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null
                || !(client.world.getEntityById(entityId) instanceof net.minecraft.entity.LivingEntity entity)) {
            context = null;
            return false;
        }
        if (!ClientCollisionTrackingPolicy.shouldCaptureHurtbox(entity)) {
            context = null;
            return false;
        }

        if (HumanoidHurtboxLibrary.isHumanoidTarget(entity) && model instanceof BipedEntityModel<?> biped) {
            IdentityHashMap<ModelPart, List<PartSpec>> parts = new IdentityHashMap<>();
            add(parts, biped.head, HumanoidHurtboxLibrary.Part.HEAD, 0, -4, 0, 8, 8, 8);
            add(parts, biped.body, HumanoidHurtboxLibrary.Part.BODY, 0, 6, 0, 8, 12, 4);
            add(parts, biped.rightArm, HumanoidHurtboxLibrary.Part.SHOULDERS, 0, 3, 0, 4, 6, 4);
            add(parts, biped.rightArm, HumanoidHurtboxLibrary.Part.RIGHT_ARM, 0, 9, 0, 4, 6, 4);
            add(parts, biped.leftArm, HumanoidHurtboxLibrary.Part.SHOULDERS, 0, 3, 0, 4, 6, 4);
            add(parts, biped.leftArm, HumanoidHurtboxLibrary.Part.LEFT_ARM, 0, 9, 0, 4, 6, 4);
            add(parts, biped.rightLeg, HumanoidHurtboxLibrary.Part.RIGHT_LEG, 0, 6, 0, 4, 12, 4);
            add(parts, biped.leftLeg, HumanoidHurtboxLibrary.Part.LEFT_LEG, 0, 6, 0, 4, 12, 4);
            float tickProgress = client.getRenderTickCounter().getTickProgress(true);
            Vector3f origin = matrices.peek().getPositionMatrix().transformPosition(new Vector3f());
            context = new RenderContext(
                    entityId, null, parts, new IdentityHashMap<>(), false, null,
                    entity.getLerpedPos(tickProgress), new Vec3d(origin.x, origin.y, origin.z)
            );
            return true;
        }

        GenericHumanoidParts genericParts = (HumanoidHurtboxLibrary.isHumanoidTarget(entity)
                || ClientEntityGeckoAnimationState.hasActiveCombatLayer(entityId))
                ? findGenericHumanoidParts(model)
                : null;
        if (genericParts != null) {
            IdentityHashMap<ModelPart, List<PartSpec>> parts = new IdentityHashMap<>();
            add(parts, genericParts.head(), HumanoidHurtboxLibrary.Part.HEAD, 0, -4, 0, 8, 8, 8);
            add(parts, genericParts.rightArm(), HumanoidHurtboxLibrary.Part.RIGHT_ARM, 0, 6, 0, 4, 12, 4);
            add(parts, genericParts.leftArm(), HumanoidHurtboxLibrary.Part.LEFT_ARM, 0, 6, 0, 4, 12, 4);
            add(parts, genericParts.rightLeg(), HumanoidHurtboxLibrary.Part.RIGHT_LEG, 0, 6, 0, 4, 12, 4);
            add(parts, genericParts.leftLeg(), HumanoidHurtboxLibrary.Part.LEFT_LEG, 0, 6, 0, 4, 12, 4);
            if (genericParts.body() != null) {
                add(parts, genericParts.body(), HumanoidHurtboxLibrary.Part.BODY, 0, 6, 0, 8, 12, 4);
            }
            float tickProgress = client.getRenderTickCounter().getTickProgress(true);
            Vector3f origin = matrices.peek().getPositionMatrix().transformPosition(new Vector3f());
            context = new RenderContext(
                    entityId, null, parts, new IdentityHashMap<>(), false, genericParts,
                    entity.getLerpedPos(tickProgress), new Vec3d(origin.x, origin.y, origin.z)
            );
            return true;
        }

        ModelPart head = findHead(model);
        context = new RenderContext(
                entityId, head, new IdentityHashMap<>(), new IdentityHashMap<>(), true, null,
                entity.getLerpedPos(client.getRenderTickCounter().getTickProgress(true)), Vec3d.ZERO
        );
        matrices.push();
        applyBodyReaction(matrices, ClientHitReactionState.getTorsoDrivenDelta(entityId));
        return true;
    }

    public static void end(MatrixStack matrices) {
        RenderContext current = context;
        context = null;
        if (current != null && !current.renderedBoxes().isEmpty()) {
            int boxCount = 0;
            for (List<HumanoidHurtboxLibrary.PartBox> boxes : current.renderedBoxes().values()) {
                boxCount += boxes.size();
            }
            List<HumanoidHurtboxLibrary.PartBox> allBoxes = new ArrayList<>(boxCount);
            for (List<HumanoidHurtboxLibrary.PartBox> boxes : current.renderedBoxes().values()) {
                allBoxes.addAll(boxes);
            }
            ClientModelHurtboxCache.updateRendered(current.entityId(), allBoxes);
        }
        if (current != null && current.pushedMatrices()) {
            matrices.pop();
        }
    }

    public static boolean beginFinalRender(int entityId, EntityModel<?> model, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null
                || !(client.world.getEntityById(entityId) instanceof net.minecraft.entity.LivingEntity entity)) {
            return false;
        }
        // The command queue also executes armor feature models with the same
        // player render state. Only the actual player body may own its cache.
        if (entity instanceof net.minecraft.entity.player.PlayerEntity
                && !(model instanceof net.minecraft.client.render.entity.model.PlayerEntityModel)) {
            return false;
        }
        boolean started = begin(entityId, model, matrices);
        if (started && context != null) {
            context.finalRenderMatrices = true;
        }
        return started;
    }

    public static void captureIfHead(ModelPart part, MatrixStack matrices) {
        RenderContext current = context;
        if (current != null
                && current.genericParts() != null
                && !current.animationApplied()
                && ClientEntityGeckoAnimationState.hasActiveCombatLayer(current.entityId())) {
            current.setAnimationApplied();
            GenericHumanoidParts generic = current.genericParts();
            VanillaSkeletonGeckoAnimationApplier.applyGenericParts(
                    current.entityId(), generic.body(), generic.head(), generic.rightArm(),
                    generic.leftArm(), generic.rightLeg(), generic.leftLeg()
            );
        }
        if (current != null && current.humanoidParts().containsKey(part)) {
            captureHumanoidPart(current, part, matrices, current.finalRenderMatrices());
            return;
        }
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

    /** Legacy builds defer combat poses here; modern models apply them in begin(). */
    public static void applyPendingCombatAnimation() {
    }

    private static void captureHumanoidPart(RenderContext current, ModelPart part, MatrixStack matrices) {
        captureHumanoidPart(current, part, matrices, false);
    }

    private static void captureHumanoidPart(
            RenderContext current,
            ModelPart part,
            MatrixStack matrices,
            boolean finalCommandMatrix
    ) {
        matrices.push();
        part.applyTransform(matrices);
        Matrix4f transform = new Matrix4f(matrices.peek().getPositionMatrix());
        matrices.pop();

        List<HumanoidHurtboxLibrary.PartBox> partBoxes = new ArrayList<>();
        for (PartSpec spec : current.humanoidParts().get(part)) {
            Vector3f center = transform.transformPosition(new Vector3f(
                    spec.centerX() * MODEL_UNIT_TO_BLOCK,
                    spec.centerY() * MODEL_UNIT_TO_BLOCK,
                    spec.centerZ() * MODEL_UNIT_TO_BLOCK
            ));
            Vector3f x = transform.transformDirection(new Vector3f(1, 0, 0));
            Vector3f y = transform.transformDirection(new Vector3f(0, 1, 0));
            Vector3f z = transform.transformDirection(new Vector3f(0, 0, 1));
            double sx = x.length();
            double sy = y.length();
            double sz = z.length();
            x.normalize();
            y.normalize();
            z.normalize();
            Vec3d renderedCenter = new Vec3d(center.x, center.y, center.z);
            Vec3d worldCenter = finalCommandMatrix
                    ? MinecraftClient.getInstance().gameRenderer.getCamera().getPos().add(renderedCenter)
                    : current.entityOrigin().add(renderedCenter.subtract(current.renderOrigin()));
            worldCenter = worldCenter.subtract(
                    FirstPersonBodyRenderOffsetContext.offsetFor(current.entityId()));
            partBoxes.add(new HumanoidHurtboxLibrary.PartBox(
                    spec.part(),
                    new com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary.OrientedBox(
                            worldCenter,
                            new Vec3d(spec.width() * MODEL_UNIT_TO_BLOCK * 0.5 * sx,
                                    spec.height() * MODEL_UNIT_TO_BLOCK * 0.5 * sy,
                                    spec.depth() * MODEL_UNIT_TO_BLOCK * 0.5 * sz),
                            new Vec3d(x.x, x.y, x.z),
                            new Vec3d(y.x, y.y, y.z),
                            new Vec3d(z.x, z.y, z.z)
                    )
            ));
        }
        current.renderedBoxes().put(part, partBoxes);
    }

    private static void add(Map<ModelPart, List<PartSpec>> parts, ModelPart modelPart,
                            HumanoidHurtboxLibrary.Part part, float x, float y, float z,
                            float width, float height, float depth) {
        parts.computeIfAbsent(modelPart, ignored -> new ArrayList<>())
                .add(new PartSpec(part, x, y, z, width, height, depth));
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

    private static GenericHumanoidParts findGenericHumanoidParts(EntityModel<?> model) {
        GenericHumanoidParts cached = GENERIC_PARTS_CACHE.get(model);
        if (cached != null) return cached;
        if (NON_HUMANOID_MODEL_CACHE.contains(model)) return null;

        Function<String, ModelPart> getter = model.getRootPart().createPartGetter();
        ModelPart head = findPart(getter, model.getRootPart(), HEAD_PART_NAMES);
        ModelPart rightArm = findPart(getter, model.getRootPart(), RIGHT_ARM_PART_NAMES);
        ModelPart leftArm = findPart(getter, model.getRootPart(), LEFT_ARM_PART_NAMES);
        ModelPart rightLeg = findPart(getter, model.getRootPart(), RIGHT_LEG_PART_NAMES);
        ModelPart leftLeg = findPart(getter, model.getRootPart(), LEFT_LEG_PART_NAMES);
        if (head == null || rightArm == null || leftArm == null || rightLeg == null || leftLeg == null) {
            NON_HUMANOID_MODEL_CACHE.add(model);
            return null;
        }
        GenericHumanoidParts resolved = new GenericHumanoidParts(
                findPart(getter, model.getRootPart(), BODY_PART_NAMES),
                head, rightArm, leftArm, rightLeg, leftLeg
        );
        GENERIC_PARTS_CACHE.put(model, resolved);
        return resolved;
    }

    private static ModelPart findPart(Function<String, ModelPart> getter, ModelPart root, String[] names) {
        for (String name : names) {
            try {
                ModelPart part = getter.apply(name);
                if (part != null && part != root) {
                    return part;
                }
            } catch (RuntimeException ignored) {
                // Try the next common model-part alias.
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

    private static final class RenderContext {
        private final int entityId;
        private final ModelPart head;
        private final IdentityHashMap<ModelPart, List<PartSpec>> humanoidParts;
        private final IdentityHashMap<ModelPart, List<HumanoidHurtboxLibrary.PartBox>> renderedBoxes;
        private final boolean pushedMatrices;
        private final GenericHumanoidParts genericParts;
        private boolean animationApplied;
        private boolean finalRenderMatrices;
        private final Vec3d entityOrigin;
        private final Vec3d renderOrigin;

        private RenderContext(int entityId, ModelPart head,
                              IdentityHashMap<ModelPart, List<PartSpec>> humanoidParts,
                              IdentityHashMap<ModelPart, List<HumanoidHurtboxLibrary.PartBox>> renderedBoxes,
                              boolean pushedMatrices, GenericHumanoidParts genericParts,
                              Vec3d entityOrigin, Vec3d renderOrigin) {
            this.entityId = entityId;
            this.head = head;
            this.humanoidParts = humanoidParts;
            this.renderedBoxes = renderedBoxes;
            this.pushedMatrices = pushedMatrices;
            this.genericParts = genericParts;
            this.entityOrigin = entityOrigin;
            this.renderOrigin = renderOrigin;
        }

        int entityId() { return entityId; }
        ModelPart head() { return head; }
        IdentityHashMap<ModelPart, List<PartSpec>> humanoidParts() { return humanoidParts; }
        IdentityHashMap<ModelPart, List<HumanoidHurtboxLibrary.PartBox>> renderedBoxes() { return renderedBoxes; }
        boolean pushedMatrices() { return pushedMatrices; }
        GenericHumanoidParts genericParts() { return genericParts; }
        boolean animationApplied() { return animationApplied; }
        void setAnimationApplied() { animationApplied = true; }
        boolean finalRenderMatrices() { return finalRenderMatrices; }
        Vec3d entityOrigin() { return entityOrigin; }
        Vec3d renderOrigin() { return renderOrigin; }
    }

    private record GenericHumanoidParts(ModelPart body, ModelPart head, ModelPart rightArm,
                                        ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
    }

    private record PartSpec(HumanoidHurtboxLibrary.Part part,
                            float centerX, float centerY, float centerZ,
                            float width, float height, float depth) {
    }
}
