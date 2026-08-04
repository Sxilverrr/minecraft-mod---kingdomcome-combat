package com.kingdomcomecombat.client.render;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.equipment.BloodiedEquipment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class BloodiedTextureCache {
    private static final int MAX_BUCKET = 100;
    private static final double MAX_COVERAGE = 0.9;
    private static final double MAX_HOLE_COVERAGE = 0.15;
    private static final double BROKEN_ARMOR_HOLE_COVERAGE = 0.38;
    private static final double LOW_DURABILITY_HOLE_THRESHOLD = 0.20;
    private static final double WEAPON_EDGE_WEAR_START_DAMAGE = 0.30;
    private static final double MAX_WEAPON_EDGE_WEAR_COVERAGE = 0.20;
    private static final double MAX_HIGH_OPACITY_COVERAGE = 0.10;
    private static final double VISUAL_BLOOD_BOOST = 1.12;
    private static final double VISUAL_STRENGTH_BOOST = 1.16;
    private static final int BLOOD_RED = 106;
    private static final int BLOOD_GREEN = 10;
    private static final int BLOOD_BLUE = 7;
    private static final int HOLE_EDGE_RED = 232;
    private static final int HOLE_EDGE_GREEN = 230;
    private static final int HOLE_EDGE_BLUE = 218;
    private static final int OVERLAY_SIZE = 64;
    private static final double MAX_BLOOD_ALPHA = 0.7;
    private static final double MAX_FRESH_BLOOD_ALPHA = 0.9;
    private static final Map<Key, Identifier> CACHE = new HashMap<>();
    private static final Map<Key, Identifier> OVERLAY_CACHE = new HashMap<>();

    private BloodiedTextureCache() {
    }

    public static Identifier getBloodiedTexture(Identifier sourceTexture, ItemStack stack) {
        double blood = CombatClientConfig.renderBlood() ? BloodiedEquipment.getBloodPercent(stack) : 0.0;
        double weaponWear = weaponEdgeWearPercent(stack);
        if (weaponWear <= 0.0) {
            return getBloodiedTexture(sourceTexture, blood);
        }

        double visualBlood = clampVisualBlood(blood);
        int bloodBucket = visualBlood > 0.0
                ? Math.max(1, Math.min(MAX_BUCKET, (int) Math.ceil(visualBlood * MAX_BUCKET)))
                : 0;
        int wearBucket = Math.max(1, Math.min(MAX_BUCKET, (int) Math.ceil(weaponWear * MAX_BUCKET)));
        Key key = new Key(sourceTexture, bloodBucket, wearBucket, CombatClientConfig.signature(), "weapon");
        Identifier cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        if (!canCreateBloodiedTexture(sourceTexture)) {
            return sourceTexture;
        }

        try (NativeImage source = loadSourceImage(sourceTexture)) {
            if (source == null) {
                return sourceTexture;
            }
            NativeImage image = source.applyToCopy(color -> color);
            int seed = stableSeed(sourceTexture);
            if (bloodBucket > 0) {
                applyBlood(image, bloodBucket / (double) MAX_BUCKET, seed);
            }
            applyWeaponEdgeWear(image, wearBucket / (double) MAX_BUCKET, seed ^ 0x4B1D);

            Identifier dynamicId = Identifier.of(
                    KingdomComeCombat.MOD_ID,
                    "dynamic/weapon/" + sanitize(sourceTexture) + "/" + CombatClientConfig.signature()
                            + "/" + bloodBucket + "_" + wearBucket
            );
            NativeImageBackedTexture texture = new NativeImageBackedTexture(
                    () -> KingdomComeCombat.MOD_ID + "/" + dynamicId.getPath(),
                    image
            );
            MinecraftClient.getInstance().getTextureManager().registerTexture(dynamicId, texture);
            CACHE.put(key, dynamicId);
            return dynamicId;
        } catch (IOException exception) {
            return sourceTexture;
        }
    }

    public static Identifier getBloodiedTexture(Identifier sourceTexture, double blood) {
        if (!CombatClientConfig.renderBlood() || blood <= 0.0) {
            return sourceTexture;
        }
        double visualBlood = clampVisualBlood(blood);
        int bucket = Math.max(1, Math.min(MAX_BUCKET, (int) Math.ceil(visualBlood * MAX_BUCKET)));
        Key key = new Key(sourceTexture, bucket, 0, CombatClientConfig.signature(), "blood");
        Identifier cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        if (!canCreateBloodiedTexture(sourceTexture)) {
            return sourceTexture;
        }

        try (NativeImage source = loadSourceImage(sourceTexture)) {
            if (source == null) {
                return sourceTexture;
            }
            NativeImage image = source.applyToCopy(color -> color);
            applyBlood(image, bucket / (double) MAX_BUCKET, stableSeed(sourceTexture));
            Identifier dynamicId = Identifier.of(
                    KingdomComeCombat.MOD_ID,
                    "dynamic/bloodied/" + sanitize(sourceTexture) + "/" + CombatClientConfig.signature() + "/" + bucket
            );
            NativeImageBackedTexture texture = new NativeImageBackedTexture(
                    () -> KingdomComeCombat.MOD_ID + "/" + dynamicId.getPath(),
                    image
            );
            MinecraftClient.getInstance().getTextureManager().registerTexture(dynamicId, texture);
            CACHE.put(key, dynamicId);
            return dynamicId;
        } catch (IOException exception) {
            return sourceTexture;
        }
    }

    public static Identifier getDragonCrackedTexture(Identifier sourceTexture) {
        Key key = new Key(sourceTexture, 1, 0, CombatClientConfig.signature(), "dragon_cracks");
        Identifier cached = CACHE.get(key);
        if (cached != null) return cached;
        if (!canCreateBloodiedTexture(sourceTexture)) return sourceTexture;

        try (NativeImage source = loadSourceImage(sourceTexture)) {
            if (source == null) return sourceTexture;
            NativeImage image = source.applyToCopy(color -> color);
            applyDragonCracks(image, stableSeed(sourceTexture));
            Identifier dynamicId = Identifier.of(
                    KingdomComeCombat.MOD_ID,
                    "dynamic/dragon_cracks/" + sanitize(sourceTexture)
            );
            MinecraftClient.getInstance().getTextureManager().registerTexture(
                    dynamicId,
                    new NativeImageBackedTexture(() -> KingdomComeCombat.MOD_ID + "/dragon_cracks", image)
            );
            CACHE.put(key, dynamicId);
            return dynamicId;
        } catch (IOException exception) {
            return sourceTexture;
        }
    }

    private static void applyDragonCracks(NativeImage image, int seed) {
        int width = image.getWidth();
        int height = image.getHeight();
        double phase = (seed & 255) / 37.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getColorArgb(x, y);
                int alpha = (color >>> 24) & 255;
                if (alpha == 0) continue;
                double branchA = Math.abs(Math.sin(x * 0.43 + Math.sin(y * 0.21 + phase) * 2.8));
                double branchB = Math.abs(Math.sin(y * 0.51 - Math.sin(x * 0.17 + phase) * 3.2));
                double branchMask = Math.min(branchA, branchB);
                if (branchMask > 0.075) continue;
                double strength = branchMask < 0.025 ? 0.90 : 0.62;
                int red = (color >>> 16) & 255;
                int green = (color >>> 8) & 255;
                int blue = color & 255;
                int nextRed = blend(red, 190, strength);
                int nextGreen = blend(green, 35, strength);
                int nextBlue = blend(blue, 255, strength);
                image.setColorArgb(x, y, (alpha << 24) | (nextRed << 16) | (nextGreen << 8) | nextBlue);
            }
        }
    }

    public static boolean shouldUseEquipmentTexture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return CombatClientConfig.renderBlood() && BloodiedEquipment.getBloodPercent(stack) > 0.0
                || shouldRenderArmorHoles(stack)
                || weaponEdgeWearPercent(stack) > 0.0;
    }

    /** Item-atlas rendering cannot safely use the worn-armor hole texture path. */
    public static boolean shouldUseItemTexture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return CombatClientConfig.renderBlood() && BloodiedEquipment.getBloodPercent(stack) > 0.0
                || weaponEdgeWearPercent(stack) > 0.0;
    }

    public static Identifier getEquipmentTexture(Identifier sourceTexture, ItemStack stack) {
        double blood = CombatClientConfig.renderBlood() ? BloodiedEquipment.getBloodPercent(stack) : 0.0;
        double visualBlood = clampVisualBlood(blood);
        double damage = armorHoleDamagePercent(stack);
        if (blood <= 0.0 && (!CombatClientConfig.renderArmorHoles() || damage <= 0.0)) {
            return sourceTexture;
        }

        int bloodBucket = visualBlood > 0.0
                ? Math.max(1, Math.min(MAX_BUCKET, (int) Math.ceil(visualBlood * MAX_BUCKET)))
                : 0;
        int damageBucket = shouldRenderArmorHoles(stack)
                ? Math.max(1, Math.min(MAX_BUCKET, (int) Math.ceil(damage * MAX_BUCKET)))
                : 0;
        Key key = new Key(sourceTexture, bloodBucket, damageBucket, CombatClientConfig.signature(), "equipment");
        Identifier cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        Identifier resourceId = toTextureResource(sourceTexture);
        if (!canCreateBloodiedTexture(sourceTexture)) {
            return sourceTexture;
        }

        try (InputStream input = MinecraftClient.getInstance().getResourceManager().open(resourceId);
             NativeImage source = NativeImage.read(input)) {
            NativeImage image = source.applyToCopy(color -> color);
            int seed = stableSeed(sourceTexture);
            if (bloodBucket > 0) {
                applyBlood(image, bloodBucket / (double) MAX_BUCKET, seed);
            }
            if (damageBucket > 0) {
                applyArmorHoles(image, damageBucket / (double) MAX_BUCKET, seed ^ 0x6C39);
            }

            Identifier dynamicId = Identifier.of(
                    KingdomComeCombat.MOD_ID,
                    "dynamic/equipment/" + sanitize(sourceTexture) + "/" + CombatClientConfig.signature()
                            + "/" + bloodBucket + "_" + damageBucket
            );
            NativeImageBackedTexture texture = new NativeImageBackedTexture(
                    () -> KingdomComeCombat.MOD_ID + "/" + dynamicId.getPath(),
                    image
            );
            MinecraftClient.getInstance().getTextureManager().registerTexture(dynamicId, texture);
            CACHE.put(key, dynamicId);
            return dynamicId;
        } catch (IOException exception) {
            return sourceTexture;
        }
    }

    public static boolean canCreateBloodiedTexture(Identifier sourceTexture) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getResourceManager().getResource(toTextureResource(sourceTexture)).isPresent()) {
            return true;
        }
        AbstractTexture texture = client.getTextureManager().getTexture(sourceTexture);
        return texture instanceof NativeImageBackedTexture backedTexture && backedTexture.getImage() != null;
    }

    private static NativeImage loadSourceImage(Identifier sourceTexture) throws IOException {
        MinecraftClient client = MinecraftClient.getInstance();
        Identifier resourceId = toTextureResource(sourceTexture);
        var resource = client.getResourceManager().getResource(resourceId);
        if (resource.isPresent()) {
            try (InputStream input = resource.get().getInputStream()) {
                return NativeImage.read(input);
            }
        }

        AbstractTexture texture = client.getTextureManager().getTexture(sourceTexture);
        if (texture instanceof NativeImageBackedTexture backedTexture && backedTexture.getImage() != null) {
            return backedTexture.getImage().applyToCopy(color -> color);
        }
        return null;
    }

    public static Identifier getBloodOverlayTexture(Identifier sourceTexture, double blood) {
        if (!CombatClientConfig.renderBlood() || blood <= 0.0) {
            return sourceTexture;
        }
        double visualBlood = clampVisualBlood(blood);
        int bucket = Math.max(1, Math.min(MAX_BUCKET, (int) Math.ceil(visualBlood * MAX_BUCKET)));
        Key key = new Key(sourceTexture, bucket, 0, CombatClientConfig.signature(), "overlay");
        Identifier cached = OVERLAY_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        NativeImage image = new NativeImage(OVERLAY_SIZE, OVERLAY_SIZE, false);
        applyBloodOverlay(image, bucket / (double) MAX_BUCKET, stableSeed(sourceTexture));
        Identifier dynamicId = Identifier.of(
                KingdomComeCombat.MOD_ID,
                "dynamic/body_blood_overlay/" + sanitize(sourceTexture) + "/" + CombatClientConfig.signature() + "/" + bucket
        );
        NativeImageBackedTexture texture = new NativeImageBackedTexture(
                () -> KingdomComeCombat.MOD_ID + "/" + dynamicId.getPath(),
                image
        );
        MinecraftClient.getInstance().getTextureManager().registerTexture(dynamicId, texture);
        OVERLAY_CACHE.put(key, dynamicId);
        return dynamicId;
    }

    private static void applyBlood(NativeImage image, double bloodPercent, int seed) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[] scores = new double[width * height];
        int visiblePixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (image.getColorArgb(x, y) >>> 24) & 255;
                int index = y * width + x;
                if (alpha == 0) {
                    scores[index] = Double.NEGATIVE_INFINITY;
                    continue;
                }
                visiblePixels++;
                scores[index] = bloodScore(x, y, width, height, seed);
            }
        }
        if (visiblePixels == 0) {
            return;
        }

        int coveredPixels = (int) Math.round(visiblePixels * Math.min(MAX_COVERAGE, bloodPercent * MAX_COVERAGE));
        if (coveredPixels <= 0) {
            return;
        }
        double[] sorted = Arrays.copyOf(scores, scores.length);
        Arrays.sort(sorted);
        double threshold = sorted[Math.max(0, sorted.length - coveredPixels)];
        double freshThreshold = freshBloodThreshold(sorted, visiblePixels, bloodPercent);
        double high = sorted[sorted.length - 1];
        double range = Math.max(0.000001, high - threshold);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double score = scores[y * width + x];
                if (score < threshold) {
                    continue;
                }
                int original = image.getColorArgb(x, y);
                int alpha = (original >>> 24) & 255;
                if (alpha == 0) {
                    continue;
                }
                int red = (original >>> 16) & 255;
                int green = (original >>> 8) & 255;
                int blue = original & 255;
                double normalized = Math.max(0.0, Math.min(1.0, (score - threshold) / range));
                double shaped = Math.pow(fade(normalized), CombatClientConfig.bloodNoiseContrast());
                double localNoise = smoothNoise(
                        x * CombatClientConfig.bloodDetailNoiseScale() * 1.31,
                        y * CombatClientConfig.bloodDetailNoiseScale() * 1.31,
                        seed ^ 0x4F11
                );
                double freshAmount = freshBloodAmount(score, freshThreshold, high);
                double targetMaximum = lerp(baseBloodMaximum(), MAX_FRESH_BLOOD_ALPHA, freshAmount);
                double targetMinimum = CombatClientConfig.bloodMinimumBlendStrength();
                double strength = Math.min(
                        targetMaximum,
                        lerp(targetMinimum, targetMaximum, shaped * (0.72 + localNoise * 0.28)) * VISUAL_STRENGTH_BOOST
                );
                int nextRed = blend(red, BLOOD_RED, strength);
                int nextGreen = blend(green, BLOOD_GREEN, strength);
                int nextBlue = blend(blue, BLOOD_BLUE, strength);
                image.setColorArgb(x, y, (alpha << 24) | (nextRed << 16) | (nextGreen << 8) | nextBlue);
            }
        }
    }

    private static void applyArmorHoles(NativeImage image, double damagePercent, int seed) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[] scores = new double[width * height];
        int visiblePixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (image.getColorArgb(x, y) >>> 24) & 255;
                int index = y * width + x;
                if (alpha == 0) {
                    scores[index] = Double.NEGATIVE_INFINITY;
                    continue;
                }
                visiblePixels++;
                scores[index] = holeScore(x, y, seed);
            }
        }
        if (visiblePixels == 0) {
            return;
        }

        boolean brokenArmor = damagePercent >= 0.999;
        double maxHoleCoverage = brokenArmor
                ? BROKEN_ARMOR_HOLE_COVERAGE
                : MAX_HOLE_COVERAGE;
        int holePixels = (int) Math.round(visiblePixels * Math.min(maxHoleCoverage, damagePercent * maxHoleCoverage));
        if (holePixels <= 0) {
            return;
        }

        boolean[] holes = distributedArmorHoles(image, scores, holePixels, seed);
        if (brokenArmor) {
            preserveLocalVisibleConnections(image, holes, width, height);
        }

        int[] original = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                original[y * width + x] = image.getColorArgb(x, y);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (holes[index]) {
                    image.setColorArgb(x, y, 0);
                    continue;
                }
                int color = original[index];
                int alpha = (color >>> 24) & 255;
                if (alpha == 0 || !touchesHole(holes, width, height, x, y)) {
                    continue;
                }
                int red = (color >>> 16) & 255;
                int green = (color >>> 8) & 255;
                int blue = color & 255;
                double edgeNoise = smoothNoise(x * 0.35, y * 0.35, seed ^ 0x2A91);
                double amount = 0.07 + edgeNoise * 0.06;
                int nextRed = blend(red, HOLE_EDGE_RED, amount);
                int nextGreen = blend(green, HOLE_EDGE_GREEN, amount);
                int nextBlue = blend(blue, HOLE_EDGE_BLUE, amount);
                image.setColorArgb(x, y, (alpha << 24) | (nextRed << 16) | (nextGreen << 8) | nextBlue);
            }
        }
    }

    private static void applyWeaponEdgeWear(NativeImage image, double wearPercent, int seed) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[] scores = new double[width * height];
        int visiblePixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (image.getColorArgb(x, y) >>> 24) & 255;
                int index = y * width + x;
                if (alpha == 0) {
                    scores[index] = Double.NEGATIVE_INFINITY;
                    continue;
                }
                int edgeDistance = transparentEdgeDistance(image, x, y, 4);
                if (edgeDistance < 0) {
                    scores[index] = Double.NEGATIVE_INFINITY;
                    continue;
                }
                visiblePixels++;
                double edgeBias = (4 - edgeDistance) / 4.0;
                double jagged = jaggedHoleNoise(x + y, y - x, seed);
                scores[index] = edgeBias * 0.72 + jagged * 0.28;
            }
        }
        if (visiblePixels == 0) {
            return;
        }

        int wornPixels = (int) Math.round(visiblePixels * Math.min(
                MAX_WEAPON_EDGE_WEAR_COVERAGE,
                wearPercent * MAX_WEAPON_EDGE_WEAR_COVERAGE
        ));
        if (wornPixels <= 0) {
            return;
        }

        double[] sorted = Arrays.copyOf(scores, scores.length);
        Arrays.sort(sorted);
        double threshold = sorted[Math.max(0, sorted.length - wornPixels)];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (scores[index] >= threshold) {
                    image.setColorArgb(x, y, 0);
                }
            }
        }
    }

    private static void applyBloodOverlay(NativeImage image, double bloodPercent, int seed) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[] scores = new double[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // NativeImage allocation is not guaranteed to be zeroed. Clear every
                // uncovered pixel so stale texture memory cannot leak into the overlay.
                image.setColorArgb(x, y, 0);
                scores[y * width + x] = bloodScore(x, y, width, height, seed);
            }
        }

        int coveredPixels = (int) Math.round(width * height * Math.min(MAX_COVERAGE, bloodPercent * MAX_COVERAGE));
        if (coveredPixels <= 0) {
            return;
        }
        double[] sorted = Arrays.copyOf(scores, scores.length);
        Arrays.sort(sorted);
        double threshold = sorted[Math.max(0, sorted.length - coveredPixels)];
        double freshThreshold = freshBloodThreshold(sorted, width * height, bloodPercent);
        double high = sorted[sorted.length - 1];
        double range = Math.max(0.000001, high - threshold);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double score = scores[y * width + x];
                if (score < threshold) {
                    continue;
                }
                double normalized = Math.max(0.0, Math.min(1.0, (score - threshold) / range));
                double shaped = Math.pow(fade(normalized), CombatClientConfig.bloodNoiseContrast());
                double localNoise = smoothNoise(
                        x * CombatClientConfig.bloodDetailNoiseScale() * 1.31,
                        y * CombatClientConfig.bloodDetailNoiseScale() * 1.31,
                        seed ^ 0x4F11
                );
                double freshAmount = freshBloodAmount(score, freshThreshold, high);
                double baseMaximum = baseBloodMaximum();
                double targetMaximum = lerp(baseMaximum, MAX_FRESH_BLOOD_ALPHA, freshAmount);
                double targetMinimum = CombatClientConfig.bloodMinimumBlendStrength();
                double strength = Math.min(
                        targetMaximum,
                        lerp(targetMinimum, targetMaximum, shaped * (0.72 + localNoise * 0.28)) * VISUAL_STRENGTH_BOOST
                );
                double alphaValue = Math.max(0.0, Math.min(targetMaximum, strength));
                int alpha = (int) Math.round(alphaValue * 255.0);
                image.setColorArgb(x, y, (alpha << 24) | (BLOOD_RED << 16) | (BLOOD_GREEN << 8) | BLOOD_BLUE);
            }
        }
    }

    private static double freshBloodThreshold(double[] sortedScores, int visiblePixels, double bloodPercent) {
        int freshPixels = (int) Math.round(visiblePixels * Math.min(MAX_HIGH_OPACITY_COVERAGE, bloodPercent * MAX_HIGH_OPACITY_COVERAGE));
        if (freshPixels <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return sortedScores[Math.max(0, sortedScores.length - freshPixels)];
    }

    private static double freshBloodAmount(double score, double freshThreshold, double high) {
        if (!Double.isFinite(freshThreshold) || high <= freshThreshold) {
            return 0.0;
        }
        double transitionStart = freshThreshold - (high - freshThreshold) * 0.35;
        return fade(Math.max(0.0, Math.min(1.0, (score - transitionStart) / (high - transitionStart))));
    }

    private static double bloodScore(int x, int y, int width, int height, int seed) {
        double nx = x / Math.max(1.0, width - 1.0);
        double ny = y / Math.max(1.0, height - 1.0);
        double low = smoothNoise(
                x * CombatClientConfig.bloodPrimaryNoiseScale(),
                y * CombatClientConfig.bloodPrimaryNoiseScale(),
                seed
        );
        double mid = smoothNoise(
                x * CombatClientConfig.bloodDetailNoiseScale() * 1.3 + 17.0,
                y * CombatClientConfig.bloodDetailNoiseScale() * 1.3 - 9.0,
                seed ^ 0x55AA
        );
        double fine = smoothNoise(
                x * CombatClientConfig.bloodDetailNoiseScale() * 3.45 - 21.0,
                y * CombatClientConfig.bloodDetailNoiseScale() * 3.45 + 4.0,
                seed ^ 0x7B2D
        );
        double speckle = smoothNoise(
                x * CombatClientConfig.bloodDetailNoiseScale() * 7.25 + 3.0,
                y * CombatClientConfig.bloodDetailNoiseScale() * 7.25 - 19.0,
                seed ^ 0x346D
        );
        double drip = 1.0 - Math.abs((
                ny + smoothNoise(x * CombatClientConfig.bloodDripNoiseScale(), 0.0, seed ^ 0x31C3) * 0.35
        ) - 0.58);
        double edge = 1.0 - Math.abs(nx - 0.5) * 0.45;
        return low * 0.24 + mid * 0.28 + fine * 0.24 + speckle * 0.18 + drip * 0.04 + edge * 0.02;
    }

    private static double holeScore(int x, int y, int seed) {
        double large = smoothNoise(x * 0.019 - 4.0, y * 0.019 + 6.0, seed ^ 0x0D31);
        double coarse = smoothNoise(x * 0.034, y * 0.034, seed);
        double mid = smoothNoise(x * 0.072 + 13.0, y * 0.072 - 7.0, seed ^ 0x51E7);
        double jagged = jaggedHoleNoise(x, y, seed ^ 0x19B3);
        double tooth = jaggedHoleNoise(x + y * 2, y - x, seed ^ 0x44A7);
        return large * 0.48 + coarse * 0.34 + mid * 0.10 + jagged * 0.06 + tooth * 0.02;
    }

    private static double jaggedHoleNoise(int x, int y, int seed) {
        int cellX = Math.floorDiv(x, 2);
        int cellY = Math.floorDiv(y, 2);
        double cell = randomUnit(cellX, cellY, seed);
        double neighbor = randomUnit(cellX + (x & 1), cellY + (y & 1), seed ^ 0x2C9D);
        return cell * 0.72 + neighbor * 0.28;
    }

    private static boolean[] distributedArmorHoles(
            NativeImage image,
            double[] scores,
            int holePixels,
            int seed
    ) {
        int width = image.getWidth();
        int height = image.getHeight();
        int columns = Math.min(5, Math.max(1, width / 8));
        int rows = Math.min(5, Math.max(1, height / 8));
        int tileCount = columns * rows;
        int[] visibleByTile = new int[tileCount];
        double[] bestScoreByTile = new double[tileCount];
        Arrays.fill(bestScoreByTile, Double.NEGATIVE_INFINITY);

        int visiblePixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (((image.getColorArgb(x, y) >>> 24) & 255) == 0) {
                    continue;
                }
                int tile = tileIndex(x, y, width, height, columns, rows);
                visibleByTile[tile]++;
                visiblePixels++;
                bestScoreByTile[tile] = Math.max(bestScoreByTile[tile], scores[y * width + x]);
            }
        }

        if (visiblePixels == 0) {
            return new boolean[width * height];
        }

        int[] pixelsByTile = new int[tileCount];
        double[] fractions = new double[tileCount];
        int assigned = 0;
        for (int tile = 0; tile < tileCount; tile++) {
            if (visibleByTile[tile] == 0) {
                continue;
            }
            double ideal = holePixels * (visibleByTile[tile] / (double) visiblePixels);
            int pixels = Math.min(visibleByTile[tile], (int) Math.floor(ideal));
            pixelsByTile[tile] = pixels;
            fractions[tile] = ideal - pixels;
            assigned += pixels;
        }

        while (assigned < holePixels) {
            int bestTile = -1;
            double bestPriority = Double.NEGATIVE_INFINITY;
            for (int tile = 0; tile < tileCount; tile++) {
                if (visibleByTile[tile] <= pixelsByTile[tile]) {
                    continue;
                }
                double priority = fractions[tile]
                        + bestScoreByTile[tile] * 0.08
                        + smoothNoise(tile * 1.7, 0.0, seed ^ 0x7A31) * 0.02;
                if (priority > bestPriority) {
                    bestPriority = priority;
                    bestTile = tile;
                }
            }
            if (bestTile < 0) {
                break;
            }
            pixelsByTile[bestTile]++;
            fractions[bestTile] = 0.0;
            assigned++;
        }

        boolean[] holes = new boolean[width * height];
        for (int tile = 0; tile < tileCount; tile++) {
            if (pixelsByTile[tile] <= 0) {
                continue;
            }
            markTileHoles(image, scores, holes, columns, rows, tile, pixelsByTile[tile]);
        }
        return holes;
    }

    private static void markTileHoles(
            NativeImage image,
            double[] scores,
            boolean[] holes,
            int columns,
            int rows,
            int tile,
            int holePixels
    ) {
        int width = image.getWidth();
        int height = image.getHeight();
        int tileX = tile % columns;
        int tileY = tile / columns;
        int minX = tileX * width / columns;
        int maxX = (tileX + 1) * width / columns;
        int minY = tileY * height / rows;
        int maxY = (tileY + 1) * height / rows;
        double[] tileScores = new double[Math.max(1, (maxX - minX) * (maxY - minY))];
        int count = 0;

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (((image.getColorArgb(x, y) >>> 24) & 255) == 0) {
                    continue;
                }
                tileScores[count++] = scores[y * width + x];
            }
        }
        if (count == 0) {
            return;
        }

        Arrays.sort(tileScores, 0, count);
        double threshold = tileScores[Math.max(0, count - Math.min(holePixels, count))];
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                int index = y * width + x;
                if (((image.getColorArgb(x, y) >>> 24) & 255) > 0 && scores[index] >= threshold) {
                    holes[index] = true;
                }
            }
        }
    }

    private static int tileIndex(int x, int y, int width, int height, int columns, int rows) {
        int tileX = Math.min(columns - 1, x * columns / Math.max(1, width));
        int tileY = Math.min(rows - 1, y * rows / Math.max(1, height));
        return tileY * columns + tileX;
    }

    private static boolean touchesHole(boolean[] holes, int width, int height, int x, int y) {
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0) {
                    continue;
                }
                int nx = x + ox;
                int ny = y + oy;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && holes[ny * width + nx]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void preserveLocalVisibleConnections(
            NativeImage image,
            boolean[] holes,
            int width,
            int height
    ) {
        boolean[] original = Arrays.copyOf(holes, holes.length);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (!original[index]) {
                    continue;
                }

                if (visibleNeighborGroups(image, original, width, height, x, y) >= 2) {
                    holes[index] = false;
                }
            }
        }
    }

    private static int visibleNeighborGroups(
            NativeImage image,
            boolean[] holes,
            int width,
            int height,
            int x,
            int y
    ) {
        boolean[] visible = new boolean[8];
        visible[0] = isVisibleAfterHoles(image, holes, width, height, x - 1, y - 1);
        visible[1] = isVisibleAfterHoles(image, holes, width, height, x, y - 1);
        visible[2] = isVisibleAfterHoles(image, holes, width, height, x + 1, y - 1);
        visible[3] = isVisibleAfterHoles(image, holes, width, height, x + 1, y);
        visible[4] = isVisibleAfterHoles(image, holes, width, height, x + 1, y + 1);
        visible[5] = isVisibleAfterHoles(image, holes, width, height, x, y + 1);
        visible[6] = isVisibleAfterHoles(image, holes, width, height, x - 1, y + 1);
        visible[7] = isVisibleAfterHoles(image, holes, width, height, x - 1, y);

        int visibleCount = 0;
        int groups = 0;
        for (int i = 0; i < visible.length; i++) {
            if (visible[i]) {
                visibleCount++;
            }
            if (visible[i] && !visible[(i + visible.length - 1) % visible.length]) {
                groups++;
            }
        }
        return visibleCount <= 1 ? 0 : groups;
    }

    private static boolean isVisibleAfterHoles(
            NativeImage image,
            boolean[] holes,
            int width,
            int height,
            int x,
            int y
    ) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        int index = y * width + x;
        return !holes[index] && ((image.getColorArgb(x, y) >>> 24) & 255) > 0;
    }

    private static int transparentEdgeDistance(NativeImage image, int x, int y, int maxDistance) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int distance = 1; distance <= maxDistance; distance++) {
            for (int oy = -distance; oy <= distance; oy++) {
                for (int ox = -distance; ox <= distance; ox++) {
                    if (Math.max(Math.abs(ox), Math.abs(oy)) != distance) {
                        continue;
                    }
                    int nx = x + ox;
                    int ny = y + oy;
                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                        return distance;
                    }
                    if (((image.getColorArgb(nx, ny) >>> 24) & 255) == 0) {
                        return distance;
                    }
                }
            }
        }
        return -1;
    }

    private static double smoothNoise(double x, double y, int seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        double sx = fade(fx);
        double sy = fade(fy);
        double a = randomUnit(x0, y0, seed);
        double b = randomUnit(x0 + 1, y0, seed);
        double c = randomUnit(x0, y0 + 1, seed);
        double d = randomUnit(x0 + 1, y0 + 1, seed);
        return lerp(lerp(a, b, sx), lerp(c, d, sx), sy);
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }

    private static double randomUnit(int x, int y, int seed) {
        int h = x * 73428767 ^ y * 912931 ^ seed * 42349;
        h ^= h >>> 13;
        h *= 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535.0;
    }

    private static int blend(int base, int stain, double amount) {
        return (int) Math.round(base + (stain - base) * Math.max(0.0, Math.min(1.0, amount)));
    }

    private static double baseBloodMaximum() {
        return Math.min(MAX_BLOOD_ALPHA, CombatClientConfig.bloodMaximumBlendStrength() * VISUAL_STRENGTH_BOOST);
    }

    private static double clampVisualBlood(double blood) {
        return Math.max(0.0, Math.min(1.0, blood * VISUAL_BLOOD_BOOST));
    }

    private static Identifier toTextureResource(Identifier texture) {
        String path = texture.getPath();
        if (path.startsWith("textures/") && path.endsWith(".png")) {
            return texture;
        }
        return Identifier.of(texture.getNamespace(), "textures/" + path + ".png");
    }

    private static String sanitize(Identifier id) {
        return (id.getNamespace() + "/" + id.getPath()).replaceAll("[^a-zA-Z0-9_./-]", "_");
    }

    private static int stableSeed(Identifier id) {
        return id.toString().hashCode();
    }

    private static boolean shouldRenderArmorHoles(ItemStack stack) {
        return CombatClientConfig.renderArmorHoles()
                && BloodiedEquipment.isArmor(stack)
                && armorDamagePercent(stack) > 0.0;
    }

    private static double armorHoleDamagePercent(ItemStack stack) {
        double damage = armorDamagePercent(stack);
        double remaining = 1.0 - damage;
        if (remaining >= LOW_DURABILITY_HOLE_THRESHOLD) {
            return damage * 0.58;
        }

        double lowDurabilityProgress = 1.0 - remaining / LOW_DURABILITY_HOLE_THRESHOLD;
        double accelerated = Math.pow(lowDurabilityProgress, 0.42);
        return Math.max(damage * 0.58, 0.46 + accelerated * 0.54);
    }

    private static double armorDamagePercent(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageable()) {
            return 0.0;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, stack.getDamage() / (double) maxDamage));
    }

    private static double weaponEdgeWearPercent(ItemStack stack) {
        if (stack == null
                || stack.isEmpty()
                || !BloodiedEquipment.isWeaponLike(stack)
                || !stack.isDamageable()
                || stack.getMaxDamage() <= 0) {
            return 0.0;
        }

        double damageRatio = Math.max(0.0, Math.min(1.0, stack.getDamage() / (double) stack.getMaxDamage()));
        if (damageRatio <= WEAPON_EDGE_WEAR_START_DAMAGE) {
            return 0.0;
        }
        return (damageRatio - WEAPON_EDGE_WEAR_START_DAMAGE) / (1.0 - WEAPON_EDGE_WEAR_START_DAMAGE);
    }

    private record Key(Identifier sourceTexture, int bucket, int damageBucket, int configSignature, String type) {
    }
}
