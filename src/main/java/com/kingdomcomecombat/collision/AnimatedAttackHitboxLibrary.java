package com.kingdomcomecombat.collision;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.animation.KeyframeInterpolation;
import com.kingdomcomecombat.combat.CombatAnimationResource;
import com.kingdomcomecombat.combat.CombatDirection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AnimatedAttackHitboxLibrary {
    private static final double MODEL_UNIT_TO_BLOCK = 1.0 / 16.0;
    private static final double EPSILON = 1.0E-6;
    private static final Map<CombatDirection, AttackHitboxAnimation> CACHE =
            new ConcurrentHashMap<>();
    private static final Map<String, AttackHitboxAnimation> NAMED_CACHE = new ConcurrentHashMap<>();
    private static Vec3d realHitboxSizeUnits = new Vec3d(1.0, 1.0, 14.0);
    private static Vec3d realHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d realHitboxRotationDegrees = Vec3d.ZERO;

    private AnimatedAttackHitboxLibrary() {
    }

    public static Optional<SampledHitbox> sample(
            CombatDirection direction,
            int ageTicks
    ) {
        return sampleSeconds(direction, ageTicks / 20.0F);
    }

    public static Optional<SampledHitbox> sampleSeconds(
            CombatDirection direction,
            float elapsedSeconds
    ) {
        return sampleSeconds(direction, elapsedSeconds, false);
    }

    public static Optional<SampledHitbox> sampleSeconds(
            CombatDirection direction,
            float elapsedSeconds,
            boolean useRealHitbox
    ) {
        AttackHitboxAnimation animation = CACHE.computeIfAbsent(
                direction,
                AnimatedAttackHitboxLibrary::load
        );

        return animation.sample(elapsedSeconds, useRealHitbox);
    }

    public static Optional<SampledHitbox> sampleNamedSeconds(
            String animationName,
            float elapsedSeconds
    ) {
        return sampleNamedSeconds(animationName, elapsedSeconds, false);
    }

    public static Optional<SampledHitbox> sampleNamedSeconds(
            String animationName,
            float elapsedSeconds,
            boolean useRealHitbox
    ) {
        return sampleNamedSeconds(animationName, elapsedSeconds, useRealHitbox, realHitboxSizeUnits);
    }

    public static Optional<SampledHitbox> sampleNamedSeconds(
            String animationName,
            float elapsedSeconds,
            boolean useRealHitbox,
            Vec3d realHitboxSizeUnits
    ) {
        return sampleNamedSeconds(
                animationName,
                elapsedSeconds,
                useRealHitbox,
                realHitboxSizeUnits,
                realHitboxOffsetUnits,
                realHitboxRotationDegrees
        );
    }

    public static Optional<SampledHitbox> sampleNamedSeconds(
            String animationName,
            float elapsedSeconds,
            boolean useRealHitbox,
            Vec3d realHitboxSizeUnits,
            Vec3d realHitboxOffsetUnits,
            Vec3d realHitboxRotationDegrees
    ) {
        return NAMED_CACHE.computeIfAbsent(animationName, AnimatedAttackHitboxLibrary::loadNamed)
                .sample(
                        elapsedSeconds,
                        useRealHitbox,
                        sanitizeRealHitboxSize(realHitboxSizeUnits),
                        realHitboxOffsetUnits == null ? AnimatedAttackHitboxLibrary.realHitboxOffsetUnits : realHitboxOffsetUnits,
                        realHitboxRotationDegrees == null ? AnimatedAttackHitboxLibrary.realHitboxRotationDegrees : realHitboxRotationDegrees
                );
    }

    public static int getLengthTicks(CombatDirection direction) {
        return Math.max(1, Math.round(CACHE.computeIfAbsent(
                direction,
                AnimatedAttackHitboxLibrary::load
        ).lengthSeconds() * 20.0F));
    }

    public static int getNamedLengthTicks(String animationName) {
        return Math.max(1, Math.round(NAMED_CACHE.computeIfAbsent(
                animationName,
                AnimatedAttackHitboxLibrary::loadNamed
        ).lengthSeconds() * 20.0F));
    }

    public static void setRealHitboxSizeUnits(double sizeUnits) {
        setRealHitboxSizeUnits(sizeUnits, sizeUnits, sizeUnits);
    }

    public static void setRealHitboxSizeUnits(double x, double y, double z) {
        realHitboxSizeUnits = new Vec3d(
                Math.max(0.1, x),
                Math.max(0.1, y),
                Math.max(0.1, z)
        );
    }

    public static Vec3d getRealHitboxSizeUnits() {
        return realHitboxSizeUnits;
    }

    private static Vec3d sanitizeRealHitboxSize(Vec3d sizeUnits) {
        if (sizeUnits == null) {
            return realHitboxSizeUnits;
        }

        return new Vec3d(
                Math.max(0.1, sizeUnits.x),
                Math.max(0.1, sizeUnits.y),
                Math.max(0.1, sizeUnits.z)
        );
    }

    public static void setRealHitboxOffsetUnits(double x, double y, double z) {
        realHitboxOffsetUnits = new Vec3d(x, y, z);
    }

    public static Vec3d getRealHitboxOffsetUnits() {
        return realHitboxOffsetUnits;
    }

    public static void setRealHitboxRotationDegrees(double x, double y, double z) {
        realHitboxRotationDegrees = new Vec3d(x, y, z);
    }

    public static Vec3d getRealHitboxRotationDegrees() {
        return realHitboxRotationDegrees;
    }

    public static void clearCache() {
        CACHE.clear();
        NAMED_CACHE.clear();
    }

    private static AttackHitboxAnimation load(CombatDirection direction) {
        String animationName = getAnimationName(direction);
        return loadByName(animationName);
    }

    private static AttackHitboxAnimation loadNamed(String animationName) {
        return loadByName(CombatAnimationResource.resolve(animationName));
    }

    private static AttackHitboxAnimation loadByName(String animationName) {
        return loadByName(CombatAnimationResource.resolve(animationName));
    }

    private static AttackHitboxAnimation loadByName(CombatAnimationResource.Resolved resolved) {
        String path = resolved.classpathResourcePath();

        try (InputStream stream = AnimatedAttackHitboxLibrary.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            if (stream == null) {
                KingdomComeCombat.LOGGER.warn(
                        "Attack hitbox animation resource missing: {}",
                        path
                );
                return AttackHitboxAnimation.empty(0.5F);
            }

            try (InputStreamReader reader = new InputStreamReader(
                    stream,
                    StandardCharsets.UTF_8
            )) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                JsonObject animation = animations.getAsJsonObject(resolved.animationKey());
                if (animation == null && !resolved.fallbackAnimationKey().equals(resolved.animationKey())) {
                    animation = animations.getAsJsonObject(resolved.fallbackAnimationKey());
                }
                if (animation == null
                        && !resolved.fileAnimationKey().equals(resolved.animationKey())
                        && !resolved.fileAnimationKey().equals(resolved.fallbackAnimationKey())) {
                    animation = animations.getAsJsonObject(resolved.fileAnimationKey());
                }
                if (animation == null) {
                    KingdomComeCombat.LOGGER.warn(
                            "Attack hitbox animation key missing: {}:{}",
                            path,
                            resolved.animationKey()
                    );
                    return AttackHitboxAnimation.empty(0.5F);
                }
                float lengthSeconds = animation.has("animation_length")
                        ? animation.get("animation_length").getAsFloat()
                        : 0.5F;
                JsonObject bones = animation.getAsJsonObject("bones");

                if (bones == null || !bones.has("hitbox")) {
                    return AttackHitboxAnimation.empty(lengthSeconds);
                }

                JsonObject hitbox = bones.getAsJsonObject("hitbox");
                List<Keyframe> scales = hitbox.has("scale")
                        ? parseKeyframes(hitbox.get("scale"))
                        : List.of();

                if (scales.isEmpty()) {
                    return AttackHitboxAnimation.empty(lengthSeconds);
                }

                List<Keyframe> positions = hitbox.has("position")
                        ? parseKeyframes(hitbox.get("position"))
                        : List.of();
                List<Keyframe> rotations = hitbox.has("rotation")
                        ? parseKeyframes(hitbox.get("rotation"))
                        : List.of();

                JsonObject item = bones.getAsJsonObject("item");
                List<Keyframe> itemPositions = item != null && item.has("position")
                        ? parseKeyframes(item.get("position"))
                        : List.of();
                List<Keyframe> itemRotations = item != null && item.has("rotation")
                        ? parseKeyframes(item.get("rotation"))
                        : List.of();

                return new AttackHitboxAnimation(
                        lengthSeconds,
                        positions,
                        rotations,
                        scales,
                        itemPositions,
                        itemRotations
                );
            }
        } catch (Exception e) {
            KingdomComeCombat.LOGGER.warn(
                    "Failed to load attack hitbox animation {}:{}",
                    path,
                    resolved.animationKey(),
                    e
            );
            return AttackHitboxAnimation.empty(0.5F);
        }
    }

    private static String getAnimationName(CombatDirection direction) {
        return switch (direction) {
            case LEFT -> "attack_left";
            case RIGHT -> "attack_right";
            case UP -> "attack_up";
            case DOWN -> "attack_down";
        };
    }

    private static List<Keyframe> parseKeyframes(JsonElement channelElement) {
        if (channelElement.isJsonArray()) {
            return List.of(Keyframe.linear(0.0F, Pose.fromJson(channelElement)));
        }

        JsonObject channel = channelElement.getAsJsonObject();
        if (channel.has("vector") || channel.has("post")) {
            return List.of(new Keyframe(
                    0.0F,
                    Pose.fromJson(channelElement),
                    KeyframeInterpolation.modeFrom(channelElement),
                    KeyframeInterpolation.bezierFrom(channelElement),
                    KeyframeInterpolation.handlesFrom(channelElement)
            ));
        }

        List<Keyframe> keyframes = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : channel.entrySet()) {
            keyframes.add(new Keyframe(
                    Float.parseFloat(entry.getKey()),
                    Pose.fromJson(entry.getValue()),
                    KeyframeInterpolation.modeFrom(entry.getValue()),
                    KeyframeInterpolation.bezierFrom(entry.getValue()),
                    KeyframeInterpolation.handlesFrom(entry.getValue())
            ));
        }

        keyframes.sort((a, b) -> Float.compare(a.time(), b.time()));
        return keyframes;
    }

    public record SampledHitbox(
            Vec3d centerOffsetBlocks,
            Pose rotationDegrees,
            Vec3d halfExtentsBlocks
    ) {
        public OrientedBox toWorldBox(LivingEntity attacker) {
            double yawRadians = Math.toRadians(attacker.getYaw());
            double cosYaw = Math.cos(yawRadians);
            double sinYaw = Math.sin(yawRadians);

            Vec3d center = attacker.getPos().add(modelToWorld(
                    centerOffsetBlocks,
                    cosYaw,
                    sinYaw
            ));

            Vec3d axisX = modelToWorld(rotateLocalAxis(
                    1.0,
                    0.0,
                    0.0,
                    rotationDegrees
            ), cosYaw, sinYaw).normalize();
            Vec3d axisY = modelToWorld(rotateLocalAxis(
                    0.0,
                    1.0,
                    0.0,
                    rotationDegrees
            ), cosYaw, sinYaw).normalize();
            Vec3d axisZ = modelToWorld(rotateLocalAxis(
                    0.0,
                    0.0,
                    1.0,
                    rotationDegrees
            ), cosYaw, sinYaw).normalize();

            return new OrientedBox(center, halfExtentsBlocks, axisX, axisY, axisZ);
        }
    }

    public record OrientedBox(
            Vec3d center,
            Vec3d halfExtents,
            Vec3d axisX,
            Vec3d axisY,
            Vec3d axisZ
    ) {
        public Box candidateBox() {
            double radiusX = Math.abs(axisX.x * halfExtents.x)
                    + Math.abs(axisY.x * halfExtents.y)
                    + Math.abs(axisZ.x * halfExtents.z);
            double radiusY = Math.abs(axisX.y * halfExtents.x)
                    + Math.abs(axisY.y * halfExtents.y)
                    + Math.abs(axisZ.y * halfExtents.z);
            double radiusZ = Math.abs(axisX.z * halfExtents.x)
                    + Math.abs(axisY.z * halfExtents.y)
                    + Math.abs(axisZ.z * halfExtents.z);

            return new Box(
                    center.x - radiusX,
                    center.y - radiusY,
                    center.z - radiusZ,
                    center.x + radiusX,
                    center.y + radiusY,
                    center.z + radiusZ
            );
        }

        public boolean intersects(Box box) {
            Vec3d aabbCenter = new Vec3d(
                    (box.minX + box.maxX) * 0.5,
                    (box.minY + box.maxY) * 0.5,
                    (box.minZ + box.maxZ) * 0.5
            );
            double[] aabbHalf = {
                    (box.maxX - box.minX) * 0.5,
                    (box.maxY - box.minY) * 0.5,
                    (box.maxZ - box.minZ) * 0.5
            };

            Vec3d[] obbAxes = {axisX, axisY, axisZ};
            Vec3d[] worldAxes = {
                    new Vec3d(1.0, 0.0, 0.0),
                    new Vec3d(0.0, 1.0, 0.0),
                    new Vec3d(0.0, 0.0, 1.0)
            };
            double[] obbHalf = {halfExtents.x, halfExtents.y, halfExtents.z};
            Vec3d delta = aabbCenter.subtract(center);

            for (int i = 0; i < 3; i++) {
                if (isSeparated(delta, obbAxes[i], obbAxes, obbHalf, worldAxes, aabbHalf)) {
                    return false;
                }
            }

            for (int i = 0; i < 3; i++) {
                if (isSeparated(delta, worldAxes[i], obbAxes, obbHalf, worldAxes, aabbHalf)) {
                    return false;
                }
            }

            for (Vec3d obbAxis : obbAxes) {
                for (Vec3d worldAxis : worldAxes) {
                    Vec3d cross = obbAxis.crossProduct(worldAxis);
                    if (cross.lengthSquared() <= EPSILON) {
                        continue;
                    }

                    if (isSeparated(
                            delta,
                            cross.normalize(),
                            obbAxes,
                            obbHalf,
                            worldAxes,
                            aabbHalf
                    )) {
                        return false;
                    }
                }
            }

            return true;
        }

        public boolean intersects(OrientedBox other) {
            Vec3d[] thisAxes = {axisX, axisY, axisZ};
            Vec3d[] otherAxes = {other.axisX(), other.axisY(), other.axisZ()};
            double[] thisHalf = {halfExtents.x, halfExtents.y, halfExtents.z};
            double[] otherHalf = {
                    other.halfExtents().x,
                    other.halfExtents().y,
                    other.halfExtents().z
            };
            Vec3d delta = other.center().subtract(center);

            for (int i = 0; i < 3; i++) {
                if (isSeparated(delta, thisAxes[i], thisAxes, thisHalf, otherAxes, otherHalf)) {
                    return false;
                }
            }

            for (int i = 0; i < 3; i++) {
                if (isSeparated(delta, otherAxes[i], thisAxes, thisHalf, otherAxes, otherHalf)) {
                    return false;
                }
            }

            for (Vec3d thisAxis : thisAxes) {
                for (Vec3d otherAxis : otherAxes) {
                    Vec3d cross = thisAxis.crossProduct(otherAxis);
                    if (cross.lengthSquared() <= EPSILON) {
                        continue;
                    }

                    if (isSeparated(
                            delta,
                            cross.normalize(),
                            thisAxes,
                            thisHalf,
                            otherAxes,
                            otherHalf
                    )) {
                        return false;
                    }
                }
            }

            return true;
        }

        private static boolean isSeparated(
                Vec3d delta,
                Vec3d axis,
                Vec3d[] obbAxes,
                double[] obbHalf,
                Vec3d[] aabbAxes,
                double[] aabbHalf
        ) {
            double distance = Math.abs(delta.dotProduct(axis));
            double obbRadius = projectionRadius(axis, obbAxes, obbHalf);
            double aabbRadius = projectionRadius(axis, aabbAxes, aabbHalf);

            return distance > obbRadius + aabbRadius + EPSILON;
        }

        private static double projectionRadius(
                Vec3d axis,
                Vec3d[] boxAxes,
                double[] halfExtents
        ) {
            return Math.abs(axis.dotProduct(boxAxes[0])) * halfExtents[0]
                    + Math.abs(axis.dotProduct(boxAxes[1])) * halfExtents[1]
                    + Math.abs(axis.dotProduct(boxAxes[2])) * halfExtents[2];
        }
    }

    public record Pose(float x, float y, float z) {
        private static final Pose ZERO = new Pose(0.0F, 0.0F, 0.0F);

        private static Pose fromJson(JsonElement element) {
            if (element.isJsonArray()) {
                return fromJsonArray(element);
            }

            JsonObject object = element.getAsJsonObject();
            if (object.has("vector")) {
                return fromJsonArray(object.get("vector"));
            }

            if (object.has("post")) {
                return fromJsonArray(object.getAsJsonObject("post").get("vector"));
            }

            return ZERO;
        }

        private static Pose fromJsonArray(JsonElement element) {
            return new Pose(
                    element.getAsJsonArray().get(0).getAsFloat(),
                    element.getAsJsonArray().get(1).getAsFloat(),
                    element.getAsJsonArray().get(2).getAsFloat()
            );
        }

        private Pose lerp(Pose other, float progress) {
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            return new Pose(
                    x + (other.x - x) * progress,
                    y + (other.y - y) * progress,
                    z + (other.z - z) * progress
            );
        }

        private static Pose catmullRom(Pose before, Pose from, Pose to, Pose after, float progress) {
            return new Pose(
                    KeyframeInterpolation.catmullRom(before.x, from.x, to.x, after.x, progress),
                    KeyframeInterpolation.catmullRom(before.y, from.y, to.y, after.y, progress),
                    KeyframeInterpolation.catmullRom(before.z, from.z, to.z, after.z, progress)
            );
        }

        private static Pose bezier(Keyframe from, Keyframe to, float progress) {
            return new Pose(
                    KeyframeInterpolation.bezierValue(
                            from.time(), to.time(), from.pose().x, to.pose().x,
                            from.handles(), to.handles(), 0, progress, from.bezier()
                    ),
                    KeyframeInterpolation.bezierValue(
                            from.time(), to.time(), from.pose().y, to.pose().y,
                            from.handles(), to.handles(), 1, progress, from.bezier()
                    ),
                    KeyframeInterpolation.bezierValue(
                            from.time(), to.time(), from.pose().z, to.pose().z,
                            from.handles(), to.handles(), 2, progress, from.bezier()
                    )
            );
        }
    }

    private record Keyframe(
            float time,
            Pose pose,
            KeyframeInterpolation.Mode interpolation,
            KeyframeInterpolation.Bezier bezier,
            KeyframeInterpolation.Handles handles
    ) {
        private static Keyframe linear(float time, Pose pose) {
            return new Keyframe(
                    time,
                    pose,
                    KeyframeInterpolation.Mode.LINEAR,
                    KeyframeInterpolation.DEFAULT_BEZIER,
                    KeyframeInterpolation.DEFAULT_HANDLES
            );
        }
    }

    private record AttackHitboxAnimation(
            float lengthSeconds,
            List<Keyframe> positions,
            List<Keyframe> rotations,
            List<Keyframe> scales,
            List<Keyframe> itemPositions,
            List<Keyframe> itemRotations
    ) {
        static AttackHitboxAnimation empty(float lengthSeconds) {
            return new AttackHitboxAnimation(
                    lengthSeconds,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        Optional<SampledHitbox> sample(float elapsedSeconds, boolean useRealHitbox) {
            return sample(
                    elapsedSeconds,
                    useRealHitbox,
                    realHitboxSizeUnits,
                    realHitboxOffsetUnits,
                    realHitboxRotationDegrees
            );
        }

        Optional<SampledHitbox> sample(
                float elapsedSeconds,
                boolean useRealHitbox,
                Vec3d realHitboxSizeUnits,
                Vec3d realHitboxOffsetUnits,
                Vec3d realHitboxRotationDegrees
        ) {
            if (scales.isEmpty()) {
                return Optional.empty();
            }

            Pose scale = sample(scales, elapsedSeconds);
            if (scale.x <= 0.0F || scale.y <= 0.0F || scale.z <= 0.0F) {
                return Optional.empty();
            }

            List<Keyframe> sampledPositions = useRealHitbox && !itemPositions.isEmpty()
                    ? itemPositions
                    : positions;
            List<Keyframe> sampledRotations = useRealHitbox ? itemRotations : rotations;
            Pose position = sampledPositions.isEmpty()
                    ? Pose.ZERO
                    : sample(sampledPositions, elapsedSeconds);
            Pose rotation = sampledRotations.isEmpty()
                    ? Pose.ZERO
                    : sample(sampledRotations, elapsedSeconds);
            if (useRealHitbox) {
                rotation = new Pose(
                        (float) (rotation.x + realHitboxRotationDegrees.x),
                        (float) (rotation.y + realHitboxRotationDegrees.y),
                        (float) (rotation.z + realHitboxRotationDegrees.z)
                );
            }
            Vec3d sanitizedRealHitboxSize = sanitizeRealHitboxSize(realHitboxSizeUnits);
            Vec3d halfExtents = useRealHitbox
                    ? new Vec3d(
                            sanitizedRealHitboxSize.x * 0.5 * MODEL_UNIT_TO_BLOCK,
                            sanitizedRealHitboxSize.y * 0.5 * MODEL_UNIT_TO_BLOCK,
                            sanitizedRealHitboxSize.z * 0.5 * MODEL_UNIT_TO_BLOCK
                    )
                    : new Vec3d(
                            scale.x * MODEL_UNIT_TO_BLOCK,
                            scale.y * MODEL_UNIT_TO_BLOCK,
                            scale.z * MODEL_UNIT_TO_BLOCK
                    );

            return Optional.of(new SampledHitbox(
                    new Vec3d(
                            (position.x + (useRealHitbox ? realHitboxOffsetUnits.x : 0.0)) * MODEL_UNIT_TO_BLOCK,
                            (position.y + (useRealHitbox ? realHitboxOffsetUnits.y : 0.0)) * MODEL_UNIT_TO_BLOCK,
                            (position.z + (useRealHitbox ? realHitboxOffsetUnits.z : 0.0)) * MODEL_UNIT_TO_BLOCK
                    ),
                    rotation,
                    halfExtents
            ));
        }

        private Pose sample(List<Keyframe> keyframes, float elapsedSeconds) {
            if (elapsedSeconds <= keyframes.getFirst().time()) {
                return keyframes.getFirst().pose();
            }

            for (int i = 1; i < keyframes.size(); i++) {
                Keyframe previous = keyframes.get(i - 1);
                Keyframe next = keyframes.get(i);

                if (elapsedSeconds <= next.time()) {
                    float span = Math.max(0.0001F, next.time() - previous.time());
                    float progress = (elapsedSeconds - previous.time()) / span;
                    if (previous.interpolation() == KeyframeInterpolation.Mode.SMOOTH) {
                        Pose before = keyframes.get(Math.max(0, i - 2)).pose();
                        Pose after = keyframes.get(Math.min(keyframes.size() - 1, i + 1)).pose();
                        return Pose.catmullRom(before, previous.pose(), next.pose(), after, progress);
                    }
                    if (previous.interpolation() == KeyframeInterpolation.Mode.BEZIER) {
                        return Pose.bezier(previous, next, progress);
                    }

                    return previous.pose().lerp(
                            next.pose(),
                            KeyframeInterpolation.progress(previous.interpolation(), progress, previous.bezier())
                    );
                }
            }

            return keyframes.getLast().pose();
        }
    }

    private static Vec3d rotateLocalAxis(
            double x,
            double y,
            double z,
            Pose rotationDegrees
    ) {
        double pitch = Math.toRadians(rotationDegrees.x);
        double yaw = Math.toRadians(rotationDegrees.y);
        double roll = Math.toRadians(rotationDegrees.z);

        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosRoll = Math.cos(roll);
        double sinRoll = Math.sin(roll);

        double rollX = x * cosRoll - y * sinRoll;
        double rollY = x * sinRoll + y * cosRoll;
        double rollZ = z;

        double pitchX = rollX;
        double pitchY = rollY * cosPitch - rollZ * sinPitch;
        double pitchZ = rollY * sinPitch + rollZ * cosPitch;

        double yawX = pitchX * cosYaw + pitchZ * sinYaw;
        double yawY = pitchY;
        double yawZ = -pitchX * sinYaw + pitchZ * cosYaw;

        return new Vec3d(yawX, yawY, yawZ);
    }

    public static Vec3d modelToWorld(Vec3d local, float yawDegrees) {
        double yawRadians = Math.toRadians(yawDegrees);
        return modelToWorld(local, Math.cos(yawRadians), Math.sin(yawRadians));
    }

    private static Vec3d modelToWorld(Vec3d local, double cosYaw, double sinYaw) {
        return new Vec3d(
                local.x * cosYaw + local.z * sinYaw,
                local.y,
                local.x * sinYaw - local.z * cosYaw
        );
    }
}
