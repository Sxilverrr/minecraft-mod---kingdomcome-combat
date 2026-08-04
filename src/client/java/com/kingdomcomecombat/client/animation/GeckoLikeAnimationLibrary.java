package com.kingdomcomecombat.client.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.animation.KeyframeInterpolation;
import com.kingdomcomecombat.combat.CombatAnimationResource;
import com.kingdomcomecombat.combat.CombatDirection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeckoLikeAnimationLibrary {
    private static final Map<Key, GeckoLikeAnimation> CACHE = new HashMap<>();
    private static final Map<String, GeckoLikeAnimation> NAMED_CACHE = new HashMap<>();

    private GeckoLikeAnimationLibrary() {
    }

    public static void clearCache() {
        CACHE.clear();
        NAMED_CACHE.clear();
    }

    public static float getLengthSeconds(Kind kind, CombatDirection direction) {
        return get(kind, direction).lengthSeconds();
    }

    public static float getLengthSeconds(Kind kind) {
        return get(kind, CombatDirection.RIGHT).lengthSeconds();
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

    public static BoneTransform sampleBone(
            Kind kind,
            CombatDirection direction,
            String boneName,
            float elapsedSeconds
    ) {
        return new BoneTransform(
                sampleRotation(kind, direction, boneName, elapsedSeconds),
                samplePosition(kind, direction, boneName, elapsedSeconds)
        );
    }

    public static float getNamedLengthSeconds(String animationName) {
        return getNamed(animationName).lengthSeconds();
    }

    public static BoneTransform sampleNamedBone(
            String animationName,
            String boneName,
            float elapsedSeconds
    ) {
        GeckoLikeAnimation animation = getNamed(animationName);
        return new BoneTransform(
                animation.sampleRotation(boneName, elapsedSeconds),
                animation.samplePosition(boneName, elapsedSeconds)
        );
    }

    private static GeckoLikeAnimation get(Kind kind, CombatDirection direction) {
        return CACHE.computeIfAbsent(new Key(kind, direction), GeckoLikeAnimationLibrary::load);
    }

    private static GeckoLikeAnimation getNamed(String animationName) {
        return NAMED_CACHE.computeIfAbsent(animationName, GeckoLikeAnimationLibrary::loadNamed);
    }

    private static GeckoLikeAnimation loadNamed(String animationName) {
        CombatAnimationResource.Resolved resolved = CombatAnimationResource.resolve(animationName);
        return loadAnimation(
                resolved.resourceId(),
                resolved.animationKey(),
                resolved.fallbackAnimationKey(),
                resolved.fileAnimationKey()
        );
    }

    private static GeckoLikeAnimation load(Key key) {
        String animationName = getAnimationName(key.kind(), key.direction());
        Identifier id = Identifier.of(
                KingdomComeCombat.MOD_ID,
                "player_animations/" + animationName + ".json"
        );
        return loadAnimation(id, animationName);
    }

    private static GeckoLikeAnimation loadAnimation(Identifier id, String animationName) {
        return loadAnimation(id, animationName, animationName, animationName);
    }

    private static GeckoLikeAnimation loadAnimation(
            Identifier id,
            String animationName,
            String fallbackAnimationName,
            String fileAnimationName
    ) {
        try {
            Optional<Resource> resource = MinecraftClient.getInstance()
                    .getResourceManager()
                    .getResource(id);

            if (resource.isEmpty()) {
                return GeckoLikeAnimation.empty();
            }

            try (InputStreamReader reader = new InputStreamReader(
                    resource.get().getInputStream(),
                    StandardCharsets.UTF_8
            )) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                JsonObject animation = animations.getAsJsonObject(animationName);
                if (animation == null && !fallbackAnimationName.equals(animationName)) {
                    animation = animations.getAsJsonObject(fallbackAnimationName);
                }
                if (animation == null
                        && !fileAnimationName.equals(animationName)
                        && !fileAnimationName.equals(fallbackAnimationName)) {
                    animation = animations.getAsJsonObject(fileAnimationName);
                }
                if (animation == null) {
                    System.out.println(
                            "[KCC] Gecko-like animation "
                                    + id
                                    + " does not contain key "
                                    + animationName
                                    + ". Expected animation key or fallback: "
                                    + fallbackAnimationName
                                    + ", "
                                    + fileAnimationName
                    );
                    return GeckoLikeAnimation.empty();
                }
                float lengthSeconds = animation.get("animation_length").getAsFloat();
                JsonObject bones = animation.getAsJsonObject("bones");

                Map<String, BoneChannels> channels = new HashMap<>();
                for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                    JsonObject bone = boneEntry.getValue().getAsJsonObject();
                    List<Keyframe> rotations = bone.has("rotation")
                            ? parseKeyframes(bone.get("rotation"))
                            : List.of();
                    List<Keyframe> positions = bone.has("position")
                            ? parseKeyframes(bone.get("position"))
                            : List.of();

                    channels.put(boneEntry.getKey(), new BoneChannels(rotations, positions));
                }

                return new GeckoLikeAnimation(lengthSeconds, channels);
            }
        } catch (Exception e) {
            System.out.println("[KCC] Failed to load Gecko-like animation " + id + ": " + e);
            return GeckoLikeAnimation.empty();
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
            case BLOCK -> "block_" + suffix;
            case BLOCK_UNPERFECT_1 -> "block_unprefect_1";
            case BLOCK_UNPERFECT_2 -> "block_unprefect_2";
        };
    }

    private static List<Keyframe> parseKeyframes(JsonElement channelElement) {
        if (channelElement.isJsonArray()) {
            return List.of(Keyframe.linear(0.0F, readVector(channelElement)));
        }

        JsonObject channel = channelElement.getAsJsonObject();
        if (channel.has("vector") || channel.has("post")) {
            return List.of(new Keyframe(
                    0.0F,
                    readVector(channelElement),
                    KeyframeInterpolation.modeFrom(channelElement),
                    KeyframeInterpolation.bezierFrom(channelElement),
                    KeyframeInterpolation.handlesFrom(channelElement)
            ));
        }

        List<Keyframe> keyframes = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : channel.entrySet()) {
            float time = Float.parseFloat(entry.getKey());
            JsonElement value = entry.getValue();
            if (value.isJsonObject()
                    && !value.getAsJsonObject().has("vector")
                    && (value.getAsJsonObject().has("pre") || value.getAsJsonObject().has("post"))) {
                JsonObject bedrockFrame = value.getAsJsonObject();
                if (bedrockFrame.has("pre")) {
                    // PAL inserts Bedrock's pre value immediately before the
                    // timestamp, then applies the post value at the timestamp.
                    keyframes.add(Keyframe.linear(
                            time == 0.0F ? 0.0F : time - 0.001F,
                            readBedrockVector(bedrockFrame.get("pre"))
                    ));
                }
                if (bedrockFrame.has("post")) {
                    keyframes.add(new Keyframe(
                            time,
                            readBedrockVector(bedrockFrame.get("post")),
                            KeyframeInterpolation.modeFrom(value),
                            KeyframeInterpolation.bezierFrom(value),
                            KeyframeInterpolation.handlesFrom(value)
                    ));
                }
                continue;
            }

            keyframes.add(new Keyframe(
                    time,
                    readVector(value),
                    KeyframeInterpolation.modeFrom(value),
                    KeyframeInterpolation.bezierFrom(value),
                    KeyframeInterpolation.handlesFrom(value)
            ));
        }

        keyframes.sort((a, b) -> Float.compare(a.time(), b.time()));
        return keyframes;
    }

    private static BonePose readVector(JsonElement element) {
        if (element.isJsonArray()) {
            return BonePose.fromJsonArray(element.getAsJsonArray());
        }

        JsonObject object = element.getAsJsonObject();
        if (object.has("vector")) {
            return BonePose.fromJsonArray(object.getAsJsonArray("vector"));
        }

        if (object.has("post")) {
            return BonePose.fromJsonArray(
                    object.getAsJsonObject("post").getAsJsonArray("vector")
            );
        }

        return BonePose.ZERO;
    }

    private static BonePose readBedrockVector(JsonElement element) {
        if (element.isJsonArray()) {
            return BonePose.fromJsonArray(element.getAsJsonArray());
        }
        if (element.isJsonObject() && element.getAsJsonObject().has("vector")) {
            return BonePose.fromJsonArray(element.getAsJsonObject().getAsJsonArray("vector"));
        }
        return BonePose.ZERO;
    }

    public enum Kind {
        ATTACK,
        STANCE,
        BLOCK,
        BLOCK_UNPERFECT_1,
        BLOCK_UNPERFECT_2
    }

    public record BonePose(float x, float y, float z) {
        public static final BonePose ZERO = new BonePose(0.0F, 0.0F, 0.0F);

        public static BonePose fromJsonArray(com.google.gson.JsonArray array) {
            return new BonePose(
                    array.get(0).getAsFloat(),
                    array.get(1).getAsFloat(),
                    array.get(2).getAsFloat()
            );
        }

        public BonePose lerp(BonePose other, float progress) {
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            return new BonePose(
                    x + (other.x - x) * progress,
                    y + (other.y - y) * progress,
                    z + (other.z - z) * progress
            );
        }

        public static BonePose catmullRom(BonePose before, BonePose from, BonePose to, BonePose after, float progress) {
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
    }

    public record BoneTransform(
            Optional<BonePose> rotation,
            Optional<BonePose> position
    ) {
        public boolean empty() {
            return rotation.isEmpty() && position.isEmpty();
        }
    }

    private record Key(Kind kind, CombatDirection direction) {
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

    private record BoneChannels(
            List<Keyframe> rotations,
            List<Keyframe> positions
    ) {
    }

    private record GeckoLikeAnimation(
            float lengthSeconds,
            Map<String, BoneChannels> channels
    ) {
        static GeckoLikeAnimation empty() {
            return new GeckoLikeAnimation(0.5F, Map.of());
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
            if (elapsedSeconds < keyframes.getFirst().time()) {
                return keyframes.getFirst().pose();
            }

            for (int i = 1; i < keyframes.size(); i++) {
                Keyframe previous = keyframes.get(i - 1);
                Keyframe next = keyframes.get(i);

                if (elapsedSeconds < next.time()) {
                    float span = Math.max(0.0001F, next.time() - previous.time());
                    float progress = (elapsedSeconds - previous.time()) / span;
                    // PAL stores the easing on the keyframe whose value ends the
                    // current segment (AnimationLoader builds previous -> current).
                    if (next.interpolation() == KeyframeInterpolation.Mode.SMOOTH) {
                        BonePose before = keyframes.get(Math.max(0, i - 2)).pose();
                        BonePose after = keyframes.get(Math.min(keyframes.size() - 1, i + 1)).pose();
                        return BonePose.catmullRom(before, previous.pose(), next.pose(), after, progress);
                    }
                    if (next.interpolation() == KeyframeInterpolation.Mode.BEZIER) {
                        return BonePose.bezier(previous, next, progress);
                    }

                    return previous.pose().lerp(
                            next.pose(),
                            KeyframeInterpolation.progress(next.interpolation(), progress, next.bezier())
                    );
                }
            }

            return keyframes.getLast().pose();
        }
    }
}
