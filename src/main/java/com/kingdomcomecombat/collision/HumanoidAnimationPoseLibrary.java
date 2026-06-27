package com.kingdomcomecombat.collision;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.animation.KeyframeInterpolation;
import com.kingdomcomecombat.combat.CombatDirection;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HumanoidAnimationPoseLibrary {
    private static final Map<Kind, Map<CombatDirection, Animation>> CACHE = new EnumMap<>(Kind.class);

    private HumanoidAnimationPoseLibrary() {
    }

    public static Optional<BonePose> sampleRotation(
            Kind kind,
            CombatDirection direction,
            String boneName,
            float elapsedSeconds
    ) {
        return get(kind, direction).sampleRotation(boneName, elapsedSeconds);
    }

    public static Optional<BonePose> samplePosition(
            Kind kind,
            CombatDirection direction,
            String boneName,
            float elapsedSeconds
    ) {
        return get(kind, direction).samplePosition(boneName, elapsedSeconds);
    }

    private static Animation get(Kind kind, CombatDirection direction) {
        return CACHE.computeIfAbsent(kind, ignored -> new EnumMap<>(CombatDirection.class))
                .computeIfAbsent(direction, dir -> load(kind, dir));
    }

    private static Animation load(Kind kind, CombatDirection direction) {
        String animationName = getAnimationName(kind, direction);
        String path = "assets/"
                + KingdomComeCombat.MOD_ID
                + "/player_animations/"
                + animationName
                + ".json";

        try (InputStream stream = HumanoidAnimationPoseLibrary.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            if (stream == null) {
                return Animation.empty();
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animation = root.getAsJsonObject("animations")
                        .getAsJsonObject(animationName);
                JsonObject bones = animation.getAsJsonObject("bones");

                Map<String, BoneChannels> channels = new HashMap<>();
                for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                    JsonObject bone = boneEntry.getValue().getAsJsonObject();
                    channels.put(
                            boneEntry.getKey(),
                            new BoneChannels(
                                    bone.has("rotation") ? parseKeyframes(bone.get("rotation")) : List.of(),
                                    bone.has("position") ? parseKeyframes(bone.get("position")) : List.of()
                            )
                    );
                }

                return new Animation(channels);
            }
        } catch (Exception e) {
            KingdomComeCombat.LOGGER.warn("Failed to load humanoid animation pose {}", path, e);
            return Animation.empty();
        }
    }

    private static String getAnimationName(Kind kind, CombatDirection direction) {
        String suffix = switch (direction) {
            case LEFT -> "left";
            case RIGHT -> "right";
            case UP -> "up";
            case DOWN -> "down";
        };

        return switch (kind) {
            case ATTACK -> "attack_" + suffix;
            case STANCE -> "stance_" + suffix;
        };
    }

    private static List<Keyframe> parseKeyframes(JsonElement channelElement) {
        if (channelElement.isJsonArray()) {
            return List.of(Keyframe.linear(0.0F, BonePose.fromJson(channelElement)));
        }

        JsonObject channel = channelElement.getAsJsonObject();
        if (channel.has("vector") || channel.has("post")) {
            return List.of(new Keyframe(
                    0.0F,
                    BonePose.fromJson(channelElement),
                    KeyframeInterpolation.modeFrom(channelElement),
                    KeyframeInterpolation.bezierFrom(channelElement),
                    KeyframeInterpolation.handlesFrom(channelElement)
            ));
        }

        List<Keyframe> keyframes = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : channel.entrySet()) {
            keyframes.add(new Keyframe(
                    Float.parseFloat(entry.getKey()),
                    BonePose.fromJson(entry.getValue()),
                    KeyframeInterpolation.modeFrom(entry.getValue()),
                    KeyframeInterpolation.bezierFrom(entry.getValue()),
                    KeyframeInterpolation.handlesFrom(entry.getValue())
            ));
        }
        keyframes.sort((a, b) -> Float.compare(a.time(), b.time()));
        return keyframes;
    }

    public enum Kind {
        ATTACK,
        STANCE
    }

    public record BonePose(float x, float y, float z) {
        public static final BonePose ZERO = new BonePose(0.0F, 0.0F, 0.0F);

        private static BonePose fromJson(JsonElement element) {
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

        private static BonePose fromJsonArray(JsonElement element) {
            return new BonePose(
                    element.getAsJsonArray().get(0).getAsFloat(),
                    element.getAsJsonArray().get(1).getAsFloat(),
                    element.getAsJsonArray().get(2).getAsFloat()
            );
        }

        private BonePose lerp(BonePose other, float progress) {
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            return new BonePose(
                    x + (other.x - x) * progress,
                    y + (other.y - y) * progress,
                    z + (other.z - z) * progress
            );
        }

        private static BonePose catmullRom(BonePose before, BonePose from, BonePose to, BonePose after, float progress) {
            return new BonePose(
                    KeyframeInterpolation.catmullRom(before.x, from.x, to.x, after.x, progress),
                    KeyframeInterpolation.catmullRom(before.y, from.y, to.y, after.y, progress),
                    KeyframeInterpolation.catmullRom(before.z, from.z, to.z, after.z, progress)
            );
        }

        private static BonePose bezier(Keyframe from, Keyframe to, float progress) {
            return new BonePose(
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

        public BonePose add(BonePose other) {
            return new BonePose(x + other.x, y + other.y, z + other.z);
        }
    }

    private record Keyframe(
            float time,
            BonePose pose,
            KeyframeInterpolation.Mode interpolation,
            KeyframeInterpolation.Bezier bezier,
            KeyframeInterpolation.Handles handles
    ) {
        private static Keyframe linear(float time, BonePose pose) {
            return new Keyframe(
                    time,
                    pose,
                    KeyframeInterpolation.Mode.LINEAR,
                    KeyframeInterpolation.DEFAULT_BEZIER,
                    KeyframeInterpolation.DEFAULT_HANDLES
            );
        }
    }

    private record BoneChannels(List<Keyframe> rotations, List<Keyframe> positions) {
    }

    private record Animation(Map<String, BoneChannels> channels) {
        static Animation empty() {
            return new Animation(Map.of());
        }

        Optional<BonePose> sampleRotation(String boneName, float elapsedSeconds) {
            BoneChannels boneChannels = channels.get(boneName);
            if (boneChannels == null || boneChannels.rotations().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(sample(boneChannels.rotations(), elapsedSeconds));
        }

        Optional<BonePose> samplePosition(String boneName, float elapsedSeconds) {
            BoneChannels boneChannels = channels.get(boneName);
            if (boneChannels == null || boneChannels.positions().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(sample(boneChannels.positions(), elapsedSeconds));
        }

        private BonePose sample(List<Keyframe> keyframes, float elapsedSeconds) {
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
                        BonePose before = keyframes.get(Math.max(0, i - 2)).pose();
                        BonePose after = keyframes.get(Math.min(keyframes.size() - 1, i + 1)).pose();
                        return BonePose.catmullRom(before, previous.pose(), next.pose(), after, progress);
                    }
                    if (previous.interpolation() == KeyframeInterpolation.Mode.BEZIER) {
                        return BonePose.bezier(previous, next, progress);
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
}
