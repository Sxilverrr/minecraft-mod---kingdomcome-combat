package com.kingdomcomecombat.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class KeyframeInterpolation {
    public static final Bezier DEFAULT_BEZIER = new Bezier(0.42F, 0.0F, 0.58F, 1.0F);
    public static final Handles DEFAULT_HANDLES = new Handles(
            new AxisVector(-0.1F, -0.1F, -0.1F),
            AxisVector.ZERO,
            new AxisVector(0.1F, 0.1F, 0.1F),
            AxisVector.ZERO,
            false
    );

    private KeyframeInterpolation() {
    }

    public static Mode modeFrom(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Mode.LINEAR;
        }

        JsonObject object = element.getAsJsonObject();
        String mode = readString(object, "interpolation");
        if (mode == null) {
            mode = readString(object, "lerp_mode");
        }
        if (mode == null) {
            mode = readString(object, "easing");
        }
        if (mode == null) {
            mode = readString(object, "easing_type");
        }
        if (mode == null) {
            return Mode.LINEAR;
        }

        return switch (mode.toLowerCase().replace('-', '_')) {
            case "catmullrom", "catmull_rom", "smooth" -> Mode.SMOOTH;
            case "bezier", "ease", "ease_in_out", "easeinout" -> Mode.BEZIER;
            case "step", "hold", "constant" -> Mode.STEP;
            default -> Mode.LINEAR;
        };
    }

    public static Bezier bezierFrom(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return DEFAULT_BEZIER;
        }

        JsonObject object = element.getAsJsonObject();
        Bezier fromArray = readBezierArray(object, "bezier");
        if (fromArray != null) {
            return fromArray;
        }

        fromArray = readBezierArray(object, "easing_args");
        if (fromArray != null) {
            return fromArray;
        }

        fromArray = readBezierArray(object, "easingArgs");
        if (fromArray != null) {
            return fromArray;
        }

        if (hasNumber(object, "x1")
                && hasNumber(object, "y1")
                && hasNumber(object, "x2")
                && hasNumber(object, "y2")) {
            return new Bezier(
                    object.get("x1").getAsFloat(),
                    object.get("y1").getAsFloat(),
                    object.get("x2").getAsFloat(),
                    object.get("y2").getAsFloat()
            );
        }

        return DEFAULT_BEZIER;
    }

    public static Handles handlesFrom(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return DEFAULT_HANDLES;
        }

        JsonObject object = element.getAsJsonObject();
        AxisVector leftTime = readAxisVector(object, "bezier_left_time", DEFAULT_HANDLES.leftTime());
        AxisVector leftValue = readAxisVector(object, "bezier_left_value", DEFAULT_HANDLES.leftValue());
        AxisVector rightTime = readAxisVector(object, "bezier_right_time", DEFAULT_HANDLES.rightTime());
        AxisVector rightValue = readAxisVector(object, "bezier_right_value", DEFAULT_HANDLES.rightValue());
        boolean custom = object.has("bezier_left_time")
                || object.has("bezier_left_value")
                || object.has("bezier_right_time")
                || object.has("bezier_right_value");
        return new Handles(leftTime, leftValue, rightTime, rightValue, custom);
    }

    public static float progress(Mode mode, float progress, Bezier bezier) {
        progress = clamp01(progress);
        return switch (mode) {
            case STEP -> 0.0F;
            case BEZIER -> cubicBezierProgress(progress, bezier == null ? DEFAULT_BEZIER : bezier);
            case LINEAR, SMOOTH -> progress;
        };
    }

    public static float catmullRom(float before, float from, float to, float after, float progress) {
        progress = clamp01(progress);
        float squared = progress * progress;
        float cubed = squared * progress;
        return 0.5F * (
                2.0F * from
                        + (-before + to) * progress
                        + (2.0F * before - 5.0F * from + 4.0F * to - after) * squared
                        + (-before + 3.0F * from - 3.0F * to + after) * cubed
        );
    }

    public static float bezierValue(
            float fromTime,
            float toTime,
            float fromValue,
            float toValue,
            Handles fromHandles,
            Handles toHandles,
            int axis,
            float progress,
            Bezier fallbackBezier
    ) {
        float span = Math.max(0.0001F, toTime - fromTime);
        progress = clamp01(progress);
        if ((fromHandles == null || !fromHandles.custom())
                && (toHandles == null || !toHandles.custom())) {
            return lerp(fromValue, toValue, cubicBezierProgress(
                    progress,
                    fallbackBezier == null ? DEFAULT_BEZIER : fallbackBezier
            ));
        }

        Handles from = fromHandles == null ? DEFAULT_HANDLES : fromHandles;
        Handles to = toHandles == null ? DEFAULT_HANDLES : toHandles;
        float controlTime1 = fromTime + from.rightTime().get(axis);
        float controlTime2 = toTime + to.leftTime().get(axis);
        float x1 = (controlTime1 - fromTime) / span;
        float x2 = (controlTime2 - fromTime) / span;
        float controlValue1 = fromValue + from.rightValue().get(axis);
        float controlValue2 = toValue + to.leftValue().get(axis);
        float t = solveBezierTime(progress, x1, x2);
        return cubicBezier(t, fromValue, controlValue1, controlValue2, toValue);
    }

    private static float solveBezierTime(float x, float x1, float x2) {
        float t = x;
        for (int i = 0; i < 6; i++) {
            float estimate = cubicBezier(t, 0.0F, x1, x2, 1.0F) - x;
            float derivative = cubicBezierDerivative(t, 0.0F, x1, x2, 1.0F);
            if (Math.abs(derivative) < 0.0001F) {
                break;
            }
            t = clamp01(t - estimate / derivative);
        }

        return t;
    }

    private static float cubicBezierProgress(float x, Bezier bezier) {
        float t = x;
        for (int i = 0; i < 5; i++) {
            float estimate = cubicBezier(t, 0.0F, bezier.x1(), bezier.x2(), 1.0F) - x;
            float derivative = cubicBezierDerivative(t, 0.0F, bezier.x1(), bezier.x2(), 1.0F);
            if (Math.abs(derivative) < 0.0001F) {
                break;
            }
            t = clamp01(t - estimate / derivative);
        }

        return cubicBezier(t, 0.0F, bezier.y1(), bezier.y2(), 1.0F);
    }

    private static float cubicBezier(float t, float p0, float p1, float p2, float p3) {
        float inverse = 1.0F - t;
        return inverse * inverse * inverse * p0
                + 3.0F * inverse * inverse * t * p1
                + 3.0F * inverse * t * t * p2
                + t * t * t * p3;
    }

    private static float cubicBezierDerivative(float t, float p0, float p1, float p2, float p3) {
        float inverse = 1.0F - t;
        return 3.0F * inverse * inverse * (p1 - p0)
                + 6.0F * inverse * t * (p2 - p1)
                + 3.0F * t * t * (p3 - p2);
    }

    private static Bezier readBezierArray(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return null;
        }

        JsonArray array = object.getAsJsonArray(key);
        if (array.size() < 4) {
            return null;
        }

        return new Bezier(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat(),
                array.get(3).getAsFloat()
        );
    }

    private static AxisVector readAxisVector(JsonObject object, String key, AxisVector fallback) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return fallback;
        }

        JsonArray array = object.getAsJsonArray(key);
        if (array.size() < 3) {
            return fallback;
        }

        return new AxisVector(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        );
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp01(progress);
    }

    private static String readString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return null;
        }

        return object.get(key).getAsString();
    }

    private static boolean hasNumber(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive();
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public enum Mode {
        LINEAR,
        BEZIER,
        SMOOTH,
        STEP
    }

    public record Bezier(float x1, float y1, float x2, float y2) {
    }

    public record Handles(
            AxisVector leftTime,
            AxisVector leftValue,
            AxisVector rightTime,
            AxisVector rightValue,
            boolean custom
    ) {
    }

    public record AxisVector(float x, float y, float z) {
        public static final AxisVector ZERO = new AxisVector(0.0F, 0.0F, 0.0F);

        public float get(int axis) {
            return switch (axis) {
                case 0 -> x;
                case 1 -> y;
                default -> z;
            };
        }
    }
}
