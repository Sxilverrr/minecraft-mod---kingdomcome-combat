package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.combat.CombatAnimationResource;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.CombatTiming;
import com.kingdomcomecombat.combat.CombatWeaponUtil;
import com.kingdomcomecombat.config.CombatClientConfig;
import com.kingdomcomecombat.item.HandCannonItem;
import com.kingdomcomecombat.item.ModItems;
import com.kingdomcomecombat.client.lockon.LockOnState;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.easing.EasingType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class CombatAnimationClient {
    private static final Identifier COMBAT_LAYER_ID =
            Identifier.of(KingdomComeCombat.MOD_ID, "combat_layer");
    private static final Identifier BLOCK_LAYER_ID =
            Identifier.of(KingdomComeCombat.MOD_ID, "block_layer");

    private static final Identifier NEUTRAL =
            Identifier.of(KingdomComeCombat.MOD_ID, "neutral");

    private static final Identifier ATTACK_RIGHT =
            Identifier.of(KingdomComeCombat.MOD_ID, "attack_right");
    private static final Identifier ATTACK_LEFT =
            Identifier.of(KingdomComeCombat.MOD_ID, "attack_left");
    private static final Identifier ATTACK_UP =
            Identifier.of(KingdomComeCombat.MOD_ID, "attack_up");
    private static final Identifier ATTACK_DOWN =
            Identifier.of(KingdomComeCombat.MOD_ID, "attack_down");

    private static final Identifier STANCE_RIGHT =
            Identifier.of(KingdomComeCombat.MOD_ID, "stance_right");
    private static final Identifier STANCE_LEFT =
            Identifier.of(KingdomComeCombat.MOD_ID, "stance_left");
    private static final Identifier STANCE_UP =
            Identifier.of(KingdomComeCombat.MOD_ID, "stance_up");
    private static final Identifier STANCE_DOWN =
            Identifier.of(KingdomComeCombat.MOD_ID, "stance_down");
    private static final Identifier BLOCK_RIGHT =
            Identifier.of(KingdomComeCombat.MOD_ID, "block_right");
    private static final Identifier BLOCK_LEFT =
            Identifier.of(KingdomComeCombat.MOD_ID, "block_left");
    private static final Identifier BLOCK_UP =
            Identifier.of(KingdomComeCombat.MOD_ID, "block_up");
    private static final Identifier BLOCK_DOWN =
            Identifier.of(KingdomComeCombat.MOD_ID, "block_down");
    private static final Identifier BLOCK_UNPERFECT_1 =
            Identifier.of(KingdomComeCombat.MOD_ID, "block_unprefect_1");
    private static final Identifier BLOCK_UNPERFECT_2 =
            Identifier.of(KingdomComeCombat.MOD_ID, "block_unprefect_2");
    private static final Identifier HAND_CANNON_RELOAD =
            Identifier.of(KingdomComeCombat.MOD_ID, "load_hand_cannon");
    private static final String HAND_CANNON_RELOAD_NAME = "load_hand_cannon";

    private static final int ATTACK_FADE_TICKS = 2;
    private static final int BLOCK_IN_FADE_TICKS = 1;
    private static final int BLOCK_OUT_FADE_TICKS = 8;
    private static final int STANCE_FADE_TICKS = CombatTiming.ATTACK_TO_STANCE_BLEND_TICKS;
    private static final int PERFECT_BLOCK_ANIMATION_TICKS = 8;
    private static final int UNPERFECT_BLOCK_ANIMATION_TICKS = 6;

    private static Identifier currentAnimation = null;
    private static boolean attackAnimationPlaying = false;
    private static SpeedModifier combatSpeedModifier = null;
    private static SpeedModifier blockSpeedModifier = null;
    private static int blockReturnTicks = 0;
    private static int hitReactionLockTicks = 0;
    private static int visualHitStopTicks = 0;
    private static TrackedAnimation combatItemAnimation = null;
    private static TrackedAnimation blockItemAnimation = null;
    private static boolean bladeTrailActive = false;
    private static final float ITEM_TRANSITION_SECONDS = 0.08F;
    private static final float CAMERA_TRANSITION_SECONDS = 0.12F;
    private static GeckoLikeAnimationLibrary.BoneTransform displayedItemTransform =
            zeroItemTransform();
    private static GeckoLikeAnimationLibrary.BoneTransform displayedCameraTransform =
            zeroItemTransform();
    private static GeckoLikeAnimationLibrary.BoneTransform displayedOffhandItemTransform =
            zeroItemTransform();
    private static float displayedStanceBodyYawOffsetDegrees = 0.0F;
    private static double displayedItemTransformUpdatedAt = -1.0;
    private static double displayedCameraTransformUpdatedAt = -1.0;
    private static double displayedOffhandItemTransformUpdatedAt = -1.0;
    private static double displayedStanceBodyYawUpdatedAt = -1.0;
    private static boolean handCannonReloadActive = false;
    private static boolean handCannonReloadVisualComplete = false;
    private static boolean handCannonReloadResumedRamrod = false;
    private static float handCannonReloadLastElapsedSeconds = 0.0F;
    private static boolean handCannonPowderBurstOne = false;
    private static boolean handCannonPowderBurstTwo = false;
    private static final float HAND_CANNON_RAMROD_START_SECONDS = HandCannonItem.RAMROD_START_SECONDS;
    private static final float HAND_CANNON_RAMROD_END_SECONDS = 7.05F;

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                COMBAT_LAYER_ID,
                2000,
                CombatAnimationClient::createCombatController
        );
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                BLOCK_LAYER_ID,
                5000,
                CombatAnimationClient::createCombatController
        );
        ClientTickEvents.END_CLIENT_TICK.register(CombatAnimationClient::tick);
    }

    private static PlayerAnimationController createCombatController(
            AbstractClientPlayerEntity player
    ) {
        PlayerAnimationController controller = new PlayerAnimationController(
                player,
                (animationController, state, animationSetter) -> null
        );
        controller.setFirstPersonModeHandler(ignored ->
                shouldUseSpecialFirstPerson(player)
                        ? FirstPersonMode.THIRD_PERSON_MODEL
                        : FirstPersonMode.DISABLED
        );
        controller.setFirstPersonConfiguration(new FirstPersonConfiguration(
                true,
                true,
                true,
                true,
                true
        ));
        return controller;
    }

    public static void playAttack(CombatDirection direction) {
        playAttack(direction, false);
    }

    public static void playSyncedEntityAttack(
            int entityId,
            CombatDirection direction,
            float speedMultiplier,
            float startupSlowdown,
            String animationName
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.player == null) {
            return;
        }

        Entity entity = client.world.getEntityById(entityId);
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (livingEntity == client.player) {
            retimeLocalSyncedAttack(direction, speedMultiplier, animationName);
            return;
        }

        if (livingEntity instanceof AbstractClientPlayerEntity) {
            playAttackOnPlayer((AbstractClientPlayerEntity) livingEntity, direction, false);
            return;
        }

        ClientEntityGeckoAnimationState.startAttack(
                entityId,
                direction,
                speedMultiplier,
                startupSlowdown,
                animationName
        );
    }

    private static void retimeLocalSyncedAttack(
            CombatDirection direction,
            float speedMultiplier,
            String animationName
    ) {
        float speed = Math.max(0.05F, speedMultiplier);
        int totalTicks = Math.max(
                1,
                Math.round((animationName == null || animationName.isBlank()
                        ? GeckoLikeAnimationLibrary.getLengthSeconds(GeckoLikeAnimationLibrary.Kind.ATTACK, direction)
                        : GeckoLikeAnimationLibrary.getNamedLengthSeconds(animationName)) * 20.0F / speed)
        );
        CombatClientState.retimeCurrentAttack(
                totalTicks,
                speed,
                CombatTiming.LIGHT_ATTACK_TRANSITION_TICKS
        );
        if (animationName != null && !animationName.isBlank()) {
            CombatClientState.setCurrentAttackAnimationName(animationName);
        }
        setCombatAnimationSpeed(speed);
    }

    public static void playAttack(CombatDirection direction, boolean chained) {
        playAttack(direction, chained, -1);
    }

    public static void playAttack(CombatDirection direction, boolean chained, int chainedFadeTicks) {
        Identifier animation = getAttackAnimation(direction);

        PlayerAnimationController controller = getController();
        if (controller == null) {
            return;
        }

        int fadeTicks = chained
                ? sanitizeFadeTicks(chainedFadeTicks, CombatTiming.ATTACK_CHAIN_BLEND_TICKS)
                : ATTACK_FADE_TICKS;

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        if (!isContinuingAttackTransition(animation, chained)) {
            playWithFade(controller, animation, fadeTicks);
        }

        currentAnimation = animation;
        attackAnimationPlaying = true;
        combatItemAnimation = new TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind.ATTACK,
                direction,
                animationTicks(),
                1.0F
        );
        bladeTrailActive = false;
    }

    public static void playComboAttack(
            String animationName,
            CombatDirection fallbackDirection,
            boolean chained
    ) {
        playComboAttack(animationName, fallbackDirection, chained, false);
    }

    public static void playComboAttack(
            String animationName,
            CombatDirection fallbackDirection,
            boolean chained,
            boolean bladeTrail
    ) {
        playComboAttack(animationName, fallbackDirection, chained, bladeTrail, -1);
    }

    public static void playComboAttack(
            String animationName,
            CombatDirection fallbackDirection,
            boolean chained,
            boolean bladeTrail,
            int chainedFadeTicks
    ) {
        PlayerAnimationController controller = getController();
        if (controller == null) {
            return;
        }

        int fadeTicks = chained
                ? sanitizeFadeTicks(chainedFadeTicks, CombatTiming.ATTACK_CHAIN_BLEND_TICKS)
                : ATTACK_FADE_TICKS;
        Identifier animation = CombatAnimationResource.playerAnimationId(animationName);

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        if (!isContinuingAttackTransition(animation, chained)) {
            playWithFade(controller, animation, fadeTicks);
        }
        displayedCameraTransformUpdatedAt = -1L;

        currentAnimation = animation;
        attackAnimationPlaying = true;
        combatItemAnimation = new TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind.ATTACK,
                fallbackDirection,
                animationTicks(),
                1.0F,
                animationName
        );
        bladeTrailActive = bladeTrail;
    }

    public static void playHitReaction(String animationName, CombatDirection fallbackDirection) {
        if (animationName == null || animationName.isBlank()) {
            return;
        }

        PlayerAnimationController controller = getController();
        if (controller == null) {
            return;
        }

        Identifier animation = CombatAnimationResource.playerAnimationId(animationName);

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        playWithFade(controller, animation, CombatTiming.ATTACK_CHAIN_TRANSITION_TICKS);
        displayedCameraTransformUpdatedAt = -1L;
        hitReactionLockTicks = Math.max(
                1,
                (int) Math.ceil(GeckoLikeAnimationLibrary.getNamedLengthSeconds(animationName) * 20.0F)
        );

        currentAnimation = animation;
        attackAnimationPlaying = false;
        combatItemAnimation = new TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind.ATTACK,
                fallbackDirection,
                animationTicks(),
                1.0F,
                animationName
        );
        bladeTrailActive = false;
    }

    public static void playBlock(int animationType, CombatDirection direction) {
        PlayerAnimationController controller = getBlockController();
        if (controller == null) {
            return;
        }

        Identifier animation = switch (animationType) {
            case 1 -> BLOCK_UNPERFECT_1;
            case 2 -> BLOCK_UNPERFECT_2;
            default -> getBlockAnimation(direction);
        };

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        setBlockAnimationSpeed(1.0F);
        playWithFade(controller, animation, BLOCK_IN_FADE_TICKS);
        blockReturnTicks = getBlockAnimationTicks(animationType);
        blockItemAnimation = new TrackedAnimation(
                getBlockKind(animationType),
                direction,
                animationTicks(),
                1.0F
        );
        attackAnimationPlaying = false;
        bladeTrailActive = false;
    }

    private static void tick(MinecraftClient client) {
        maintainAlwaysOnFirstPersonModel(client);
        tickHandCannonReload(client);
        tickVisualHitStop();
        if (combatItemAnimation != null
                && combatItemAnimation.kind() == GeckoLikeAnimationLibrary.Kind.STANCE
                && (client.player == null || !CombatItemUtil.hasCombatWeapon(client.player))) {
            exitCombatPose();
        }
        if (blockReturnTicks <= 0) {
            tickHitReactionLock();
            return;
        }

        blockReturnTicks--;
        tickHitReactionLock();
        if (blockReturnTicks > 0) {
            return;
        }

        PlayerAnimationController controller = getBlockController();
        if (controller == null) {
            return;
        }

        setBlockAnimationSpeed(1.0F);
        playWithFade(controller, NEUTRAL, BLOCK_OUT_FADE_TICKS);
        blockItemAnimation = null;
    }

    private static void playAttackOnPlayer(
            AbstractClientPlayerEntity player,
            CombatDirection direction,
            boolean chained
    ) {
        Identifier animation = getAttackAnimation(direction);

        PlayerAnimationController controller = getController(player);
        if (controller == null) {
            return;
        }

        int fadeTicks = chained
                ? CombatTiming.ATTACK_CHAIN_BLEND_TICKS
                : ATTACK_FADE_TICKS;

        playWithFade(controller, animation, fadeTicks);
    }
    public static void playAttackTransitionPreview(CombatDirection direction) {
        String customAttackAnimation = localPlayerAttackAnimationName(direction);
        Identifier animation = customAttackAnimation.isBlank()
                ? getAttackAnimation(direction)
                : CombatAnimationResource.playerAnimationId(customAttackAnimation);

        if (animation.equals(currentAnimation) && !attackAnimationPlaying) {
            return;
        }

        PlayerAnimationController controller = getController();
        if (controller == null) {
            return;
        }

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        playWithFade(controller, animation, CombatTiming.ATTACK_CHAIN_TRANSITION_TICKS);

        currentAnimation = animation;
        attackAnimationPlaying = false;
        combatItemAnimation = new TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind.ATTACK,
                direction,
                animationTicks(),
                1.0F,
                customAttackAnimation.isBlank() ? null : customAttackAnimation
        );
        bladeTrailActive = false;
    }

    public static void playStance(CombatDirection direction) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !CombatItemUtil.hasCombatWeapon(client.player)) {
            exitCombatPose();
            return;
        }

        String customStanceAnimation = localPlayerStanceAnimationName(direction);
        Identifier animation = customStanceAnimation.isBlank()
                ? getStanceAnimation(direction)
                : CombatAnimationResource.playerAnimationId(customStanceAnimation);

        if (animation.equals(currentAnimation) && !attackAnimationPlaying) {
            return;
        }

        PlayerAnimationController controller = getController();
        if (controller == null) {
            return;
        }

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        playWithFade(controller, animation, STANCE_FADE_TICKS);

        currentAnimation = animation;
        attackAnimationPlaying = false;
        combatItemAnimation = new TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind.STANCE,
                direction,
                animationTicks(),
                1.0F,
                customStanceAnimation.isBlank() ? null : customStanceAnimation
        );
    }

    public static void blendToStanceAfterAttack(CombatDirection direction) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !CombatItemUtil.hasCombatWeapon(client.player)) {
            blendToNeutralAfterAttack();
            return;
        }

        String customStanceAnimation = localPlayerStanceAnimationName(direction);
        Identifier animation = customStanceAnimation.isBlank()
                ? getStanceAnimation(direction)
                : CombatAnimationResource.playerAnimationId(customStanceAnimation);

        PlayerAnimationController controller = getController();
        if (controller == null) {
            return;
        }

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        playWithFade(controller, animation, STANCE_FADE_TICKS);

        currentAnimation = animation;
        attackAnimationPlaying = false;
        combatItemAnimation = new TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind.STANCE,
                direction,
                animationTicks(),
                1.0F,
                customStanceAnimation.isBlank() ? null : customStanceAnimation
        );
    }

    public static void blendToNeutralAfterAttack() {
        PlayerAnimationController controller = getController();

        if (controller == null) {
            currentAnimation = null;
            attackAnimationPlaying = false;
            return;
        }

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        playWithFade(controller, NEUTRAL, STANCE_FADE_TICKS);

        currentAnimation = NEUTRAL;
        attackAnimationPlaying = false;
        combatItemAnimation = null;
        bladeTrailActive = false;
        bladeTrailActive = false;
    }

    public static void onAttackFinished(boolean locked, CombatDirection direction) {
        attackAnimationPlaying = false;
        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);

        // 如果提前 recovery blend 已经进入 stance / neutral，这里不要二次触发。
        if (currentAnimation != null) {
            return;
        }

        if (locked) {
            playStance(direction);
        } else {
            playNeutral();
        }
    }

    public static void startLocalHitStop(int ticks) {
        if (ticks <= 0 || (combatItemAnimation == null && blockItemAnimation == null)) {
            return;
        }

        visualHitStopTicks = Math.max(visualHitStopTicks, ticks);
        setCombatAnimationSpeed(0.01F);
        setBlockAnimationSpeed(0.01F);
        retimeCombatItemAnimation(0.0F);
        retimeBlockItemAnimation(0.0F);
    }

    private static void clearVisualHitStop() {
        if (visualHitStopTicks <= 0) {
            return;
        }

        visualHitStopTicks = 0;
        setCombatAnimationSpeed(1.0F);
        setBlockAnimationSpeed(1.0F);
    }

    private static void tickVisualHitStop() {
        if (visualHitStopTicks <= 0) {
            return;
        }

        visualHitStopTicks--;
        if (visualHitStopTicks <= 0) {
            setCombatAnimationSpeed(1.0F);
            setBlockAnimationSpeed(1.0F);
        }
    }

    public static void playNeutral() {
        PlayerAnimationController controller = getController();

        if (controller == null) {
            currentAnimation = null;
            attackAnimationPlaying = false;
            return;
        }

        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        playWithFade(controller, NEUTRAL, STANCE_FADE_TICKS);

        currentAnimation = NEUTRAL;
        attackAnimationPlaying = false;
        combatItemAnimation = null;
    }

    public static void exitCombatPose() {
        playNeutral();
        blockItemAnimation = null;
        hitReactionLockTicks = 0;
    }

    public static void clearCurrentAnimation() {
        if (handCannonReloadActive) {
            return;
        }
        if (!attackAnimationPlaying) {
            currentAnimation = null;
            combatItemAnimation = null;
            bladeTrailActive = false;
        }
    }

    public static boolean isBladeTrailActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        return attackAnimationPlaying
                && (bladeTrailActive
                || (client.player != null
                && CombatItemUtil.hasSweepingEdge(client.player.getMainHandStack())));
    }

    public static boolean isLocalMovementLockedByAnimation() {
        return blockReturnTicks > 0 || hitReactionLockTicks > 0;
    }

    public static boolean hasLocalPlayerCombatAnimation() {
        return combatItemAnimation != null || blockItemAnimation != null;
    }

    public static boolean isKccSpecialFirstPersonActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player instanceof AbstractClientPlayerEntity player
                && shouldUseSpecialFirstPerson(player);
    }

    private static boolean shouldUseSpecialFirstPerson(AbstractClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != player
                || player.isGliding()
                || player.isSwimming()
                || player.isInSwimmingPose()
                || CombatClientConfig.disablesSpecialFirstPerson(player.getMainHandStack())
                || CombatClientConfig.disablesSpecialFirstPerson(player.getOffHandStack())) {
            return false;
        }
        if (CombatClientConfig.firstPersonCameraHeadBindingEnabled()
                || CombatClientConfig.firstPersonSimpleEyeSimulationEnabled()) {
            return true;
        }

        return switch (CombatClientConfig.firstPersonModelMode()) {
            case WHEN_NEEDED -> hasLocalPlayerCombatAnimation()
                    || handCannonReloadActive
                    || blockReturnTicks > 0
                    || hitReactionLockTicks > 0
                    || CombatClientState.attacking;
            case ALWAYS -> true;
            case LOCKED_ONLY -> LockOnState.locked;
        };
    }

    private static void maintainAlwaysOnFirstPersonModel(MinecraftClient client) {
        if (!(client.player instanceof AbstractClientPlayerEntity player)
                || (CombatClientConfig.firstPersonModelMode()
                != CombatClientConfig.FirstPersonModelMode.ALWAYS
                && !CombatClientConfig.firstPersonCameraHeadBindingEnabled()
                && !CombatClientConfig.firstPersonSimpleEyeSimulationEnabled())
                || !shouldUseSpecialFirstPerson(player)) {
            return;
        }
        PlayerAnimationController controller = getController(player);
        if (controller != null && !controller.isActive()) {
            playWithFade(controller, NEUTRAL, STANCE_FADE_TICKS);
            currentAnimation = NEUTRAL;
        }
    }

    private static void tickHitReactionLock() {
        if (hitReactionLockTicks > 0) {
            hitReactionLockTicks--;
        }
    }

    public static Optional<GeckoLikeAnimationLibrary.BoneTransform> getLocalPlayerItemTransform() {
        Optional<GeckoLikeAnimationLibrary.BoneTransform> blockTransform =
                sampleTrackedItemTransform(blockItemAnimation);
        if (blockTransform.isPresent()) {
            return Optional.of(updateDisplayedItemTransform(blockTransform.get()));
        }

        GeckoLikeAnimationLibrary.BoneTransform target =
                sampleTrackedItemTransform(combatItemAnimation).orElseGet(
                        CombatAnimationClient::zeroItemTransform
                );
        return Optional.of(updateDisplayedItemTransform(target));
    }

    public static Optional<GeckoLikeAnimationLibrary.BoneTransform> getLocalPlayerOffhandItemTransform() {
        GeckoLikeAnimationLibrary.BoneTransform target =
                sampleTrackedBoneTransform(combatItemAnimation, "item_left")
                        .map(CombatAnimationClient::fillMissingChannelsWithZero)
                        .orElseGet(CombatAnimationClient::zeroItemTransform);
        return Optional.of(updateDisplayedOffhandItemTransform(target));
    }

    public static Optional<GeckoLikeAnimationLibrary.BoneTransform> getLocalPlayerRenderedItemTransform(
            ItemStack stack,
            ItemDisplayContext displayContext
    ) {
        if (!isHeldHandContext(displayContext) || !handCannonReloadActive) {
            return Optional.empty();
        }
        if (stack.isOf(ModItems.HAND_CANNON)) {
            return getLocalPlayerItemTransform();
        }
        if (stack.isOf(ModItems.BULLET_WITH_GUNPOWDER)
                || (stack.isOf(Items.STICK) && isHandCannonRamrodWindow(displayContext))) {
            return getLocalPlayerOffhandItemTransform();
        }
        return Optional.empty();
    }

    public static boolean shouldRenderHandCannonRamrod(ItemStack stack, ItemDisplayContext displayContext) {
        return isLocalPlayerOffhandContext(displayContext)
                && isHandCannonRamrodWindow(displayContext);
    }

    public static boolean isLocalHandCannonReloadRamrodWindow() {
        Optional<Float> elapsed = getLocalHandCannonReloadElapsedSeconds();
        return elapsed.isPresent()
                && elapsed.get() >= HAND_CANNON_RAMROD_START_SECONDS
                && elapsed.get() <= HAND_CANNON_RAMROD_END_SECONDS;
    }

    private static boolean isHandCannonRamrodWindow(ItemDisplayContext displayContext) {
        if (!isHeldHandContext(displayContext)) {
            return false;
        }
        return isLocalHandCannonReloadRamrodWindow();
    }

    private static boolean isLocalPlayerOffhandContext(ItemDisplayContext displayContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        Arm offArm = client.player.getMainArm().getOpposite();
        return displayContext == (offArm == Arm.RIGHT
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                : ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                || displayContext == (offArm == Arm.RIGHT
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                : ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
    }

    public static Optional<GeckoLikeAnimationLibrary.BoneTransform> getLocalPlayerCameraTransform() {
        Optional<GeckoLikeAnimationLibrary.BoneTransform> blockTransform =
                sampleTrackedBoneTransform(blockItemAnimation, "camera");
        if (blockTransform.isPresent()) {
            return Optional.of(updateDisplayedCameraTransform(
                    fillMissingChannelsWithZero(blockTransform.get())
            ));
        }

        GeckoLikeAnimationLibrary.BoneTransform target =
                sampleTrackedBoneTransform(combatItemAnimation, "camera")
                        .map(CombatAnimationClient::fillMissingChannelsWithZero)
                        .orElseGet(CombatAnimationClient::zeroItemTransform);
        return Optional.of(updateDisplayedCameraTransform(target));
    }

    public static Optional<GeckoLikeAnimationLibrary.BoneTransform> getLocalPlayerRawCameraTransform() {
        Optional<GeckoLikeAnimationLibrary.BoneTransform> blockTransform =
                sampleTrackedBoneTransform(blockItemAnimation, "camera");
        if (blockTransform.isPresent()) {
            return Optional.of(fillMissingChannelsWithZero(blockTransform.get()));
        }

        return sampleTrackedBoneTransform(combatItemAnimation, "camera")
                .map(CombatAnimationClient::fillMissingChannelsWithZero);
    }

    public static void setCombatAnimationSpeed(float speed) {
        speed = Math.max(0.01F, speed);
        retimeCombatItemAnimation(speed);

        PlayerAnimationController controller = getController();
        if (controller == null) {
            return;
        }

        if (Math.abs(speed - 1.0F) < 0.001F) {
            if (combatSpeedModifier != null) {
                SpeedModifier modifier = combatSpeedModifier;
                controller.removeModifierIf(existing -> existing == modifier);
                combatSpeedModifier = null;
            }
            return;
        }

        if (combatSpeedModifier == null) {
            combatSpeedModifier = new SpeedModifier(speed);
            controller.addModifierLast(combatSpeedModifier);
            return;
        }

        combatSpeedModifier.speed = speed;
    }

    public static void setBlockAnimationSpeed(float speed) {
        retimeBlockItemAnimation(speed);

        PlayerAnimationController controller = getBlockController();
        if (controller == null) {
            return;
        }

        speed = Math.max(0.01F, speed);
        if (Math.abs(speed - 1.0F) < 0.001F) {
            if (blockSpeedModifier != null) {
                SpeedModifier modifier = blockSpeedModifier;
                controller.removeModifierIf(existing -> existing == modifier);
                blockSpeedModifier = null;
            }
            return;
        }

        if (blockSpeedModifier == null) {
            blockSpeedModifier = new SpeedModifier(speed);
            controller.addModifierLast(blockSpeedModifier);
            return;
        }

        blockSpeedModifier.speed = speed;
    }

    public static void resetAnimationSpeeds() {
        clearVisualHitStop();
        setCombatAnimationSpeed(1.0F);
        setBlockAnimationSpeed(1.0F);
    }

    private static Identifier getAttackAnimation(CombatDirection direction) {
        return switch (direction) {
            case RIGHT -> ATTACK_RIGHT;
            case LEFT -> ATTACK_LEFT;
            case UP -> ATTACK_UP;
            case DOWN -> ATTACK_DOWN;
        };
    }

    private static Identifier getStanceAnimation(CombatDirection direction) {
        return switch (direction) {
            case RIGHT -> STANCE_RIGHT;
            case LEFT -> STANCE_LEFT;
            case UP -> STANCE_UP;
            case DOWN -> STANCE_DOWN;
        };
    }

    private static Identifier getBlockAnimation(CombatDirection direction) {
        return switch (direction) {
            case RIGHT -> BLOCK_RIGHT;
            case LEFT -> BLOCK_LEFT;
            case UP -> BLOCK_UP;
            case DOWN -> BLOCK_DOWN;
        };
    }

    private static int getBlockAnimationTicks(int animationType) {
        return switch (animationType) {
            case 1, 2 -> UNPERFECT_BLOCK_ANIMATION_TICKS;
            default -> PERFECT_BLOCK_ANIMATION_TICKS;
        };
    }

    private static GeckoLikeAnimationLibrary.Kind getBlockKind(int animationType) {
        return switch (animationType) {
            case 1 -> GeckoLikeAnimationLibrary.Kind.BLOCK_UNPERFECT_1;
            case 2 -> GeckoLikeAnimationLibrary.Kind.BLOCK_UNPERFECT_2;
            default -> GeckoLikeAnimationLibrary.Kind.BLOCK;
        };
    }

    private static Identifier blockAnimationId(TrackedAnimation animation) {
        return switch (animation.kind()) {
            case BLOCK_UNPERFECT_1 -> BLOCK_UNPERFECT_1;
            case BLOCK_UNPERFECT_2 -> BLOCK_UNPERFECT_2;
            default -> getBlockAnimation(animation.direction());
        };
    }

    private static Optional<GeckoLikeAnimationLibrary.BoneTransform> sampleTrackedItemTransform(
            TrackedAnimation animation
    ) {
        return sampleTrackedBoneTransform(animation, "item")
                .map(CombatAnimationClient::fillMissingChannelsWithZero);
    }

    private static Optional<GeckoLikeAnimationLibrary.BoneTransform> sampleTrackedBoneTransform(
            TrackedAnimation animation,
            String boneName
    ) {
        if (animation == null) {
            return Optional.empty();
        }

        float elapsed = animation.elapsedSeconds();
        float length = getTrackedAnimationLength(animation);

        if (length > 0.0F && animation.kind() == GeckoLikeAnimationLibrary.Kind.STANCE) {
            elapsed %= length;
        }

        if (animation.kind() != GeckoLikeAnimationLibrary.Kind.STANCE && elapsed > length) {
            return Optional.empty();
        }

        GeckoLikeAnimationLibrary.BoneTransform transform = animation.customAnimationName() != null
                ? GeckoLikeAnimationLibrary.sampleNamedBone(
                        animation.customAnimationName(),
                        boneName,
                        elapsed
                )
                : GeckoLikeAnimationLibrary.sampleBone(
                        animation.kind(),
                        animation.direction(),
                        boneName,
                        elapsed
                );

        return transform.empty() ? Optional.empty() : Optional.of(transform);
    }

    public static float getLocalStanceBodyYawOffsetDegrees() {
        Optional<GeckoLikeAnimationLibrary.BoneTransform> transform =
                sampleTrackedBoneTransform(combatItemAnimation, "body");
        float target = transform
                .flatMap(GeckoLikeAnimationLibrary.BoneTransform::rotation)
                .map(GeckoLikeAnimationLibrary.BonePose::y)
                .orElse(0.0F);
        return updateDisplayedStanceBodyYawOffsetDegrees(target);
    }

    private static float updateDisplayedStanceBodyYawOffsetDegrees(float target) {
        double now = animationTicks();
        if (displayedStanceBodyYawUpdatedAt < 0.0) {
            displayedStanceBodyYawUpdatedAt = now;
            displayedStanceBodyYawOffsetDegrees = target;
            return displayedStanceBodyYawOffsetDegrees;
        }

        float deltaSeconds = MathHelper.clamp(
                (float) ((now - displayedStanceBodyYawUpdatedAt) / 20.0),
                0.0F,
                0.05F
        );
        displayedStanceBodyYawUpdatedAt = now;

        float amount = 1.0F - (float) Math.exp(-18.0F * deltaSeconds);
        displayedStanceBodyYawOffsetDegrees += MathHelper.wrapDegrees(
                target - displayedStanceBodyYawOffsetDegrees
        ) * amount;
        return displayedStanceBodyYawOffsetDegrees;
    }

    private static float getTrackedAnimationLength(TrackedAnimation animation) {
        if (animation.kind() == GeckoLikeAnimationLibrary.Kind.BLOCK_UNPERFECT_1
                || animation.kind() == GeckoLikeAnimationLibrary.Kind.BLOCK_UNPERFECT_2) {
            return GeckoLikeAnimationLibrary.getLengthSeconds(animation.kind());
        }

        if (animation.customAnimationName() != null) {
            return GeckoLikeAnimationLibrary.getNamedLengthSeconds(animation.customAnimationName());
        }

        return GeckoLikeAnimationLibrary.getLengthSeconds(animation.kind(), animation.direction());
    }

    private static String localPlayerStanceAnimationName(CombatDirection direction) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return "";
        }

        return CombatWeaponUtil.stanceAnimationName(client.player, direction);
    }

    private static String localPlayerAttackAnimationName(CombatDirection direction) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return "";
        }

        return CombatWeaponUtil.resolveAttackMove(client.player, direction).animationName();
    }

    private static GeckoLikeAnimationLibrary.BoneTransform updateDisplayedItemTransform(
            GeckoLikeAnimationLibrary.BoneTransform target
    ) {
        displayedItemTransform = updateDisplayedTransform(
                target,
                displayedItemTransform,
                true
        );
        return displayedItemTransform;
    }

    private static GeckoLikeAnimationLibrary.BoneTransform updateDisplayedCameraTransform(
            GeckoLikeAnimationLibrary.BoneTransform target
    ) {
        displayedCameraTransform = updateDisplayedTransform(
                target,
                displayedCameraTransform,
                false
        );
        return displayedCameraTransform;
    }

    private static GeckoLikeAnimationLibrary.BoneTransform updateDisplayedOffhandItemTransform(
            GeckoLikeAnimationLibrary.BoneTransform target
    ) {
        double now = animationTicks();
        if (displayedOffhandItemTransformUpdatedAt < 0.0) {
            displayedOffhandItemTransformUpdatedAt = now;
            return displayedOffhandItemTransform;
        }

        float deltaSeconds = (float) ((now - displayedOffhandItemTransformUpdatedAt) / 20.0);
        displayedOffhandItemTransformUpdatedAt = now;
        float progress = ITEM_TRANSITION_SECONDS <= 0.0F
                ? 1.0F
                : Math.min(1.0F, deltaSeconds / ITEM_TRANSITION_SECONDS);
        displayedOffhandItemTransform = lerpItemTransform(displayedOffhandItemTransform, target, progress);
        return displayedOffhandItemTransform;
    }

    private static GeckoLikeAnimationLibrary.BoneTransform updateDisplayedTransform(
            GeckoLikeAnimationLibrary.BoneTransform target,
            GeckoLikeAnimationLibrary.BoneTransform displayed,
            boolean item
    ) {
        double now = animationTicks();
        double updatedAt = item ? displayedItemTransformUpdatedAt : displayedCameraTransformUpdatedAt;
        if (updatedAt < 0.0) {
            if (item) {
                displayedItemTransformUpdatedAt = now;
            } else {
                displayedCameraTransformUpdatedAt = now;
            }
            return displayed;
        }

        float deltaSeconds =
                (float) ((now - updatedAt) / 20.0);
        if (item) {
            displayedItemTransformUpdatedAt = now;
        } else {
            displayedCameraTransformUpdatedAt = now;
        }
        float progress = ITEM_TRANSITION_SECONDS <= 0.0F
                ? 1.0F
                : Math.min(1.0F, deltaSeconds / (item
                        ? ITEM_TRANSITION_SECONDS
                        : CAMERA_TRANSITION_SECONDS));
        return lerpItemTransform(displayed, target, progress);
    }

    private static GeckoLikeAnimationLibrary.BoneTransform lerpItemTransform(
            GeckoLikeAnimationLibrary.BoneTransform from,
            GeckoLikeAnimationLibrary.BoneTransform to,
            float progress
    ) {
        return new GeckoLikeAnimationLibrary.BoneTransform(
                Optional.of(from.rotation()
                        .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO)
                        .lerp(to.rotation()
                                .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO), progress)),
                Optional.of(from.position()
                        .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO)
                        .lerp(to.position()
                                .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO), progress))
        );
    }

    private static GeckoLikeAnimationLibrary.BoneTransform fillMissingChannelsWithZero(
            GeckoLikeAnimationLibrary.BoneTransform transform
    ) {
        return new GeckoLikeAnimationLibrary.BoneTransform(
                Optional.of(transform.rotation().orElse(GeckoLikeAnimationLibrary.BonePose.ZERO)),
                Optional.of(transform.position().orElse(GeckoLikeAnimationLibrary.BonePose.ZERO))
        );
    }

    private static GeckoLikeAnimationLibrary.BoneTransform zeroItemTransform() {
        return new GeckoLikeAnimationLibrary.BoneTransform(
                Optional.of(GeckoLikeAnimationLibrary.BonePose.ZERO),
                Optional.of(GeckoLikeAnimationLibrary.BonePose.ZERO)
        );
    }

    private static PlayerAnimationController getController() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!(client.player instanceof AbstractClientPlayerEntity player)) {
            return null;
        }

        return getController(player);
    }

    private static PlayerAnimationController getController(AbstractClientPlayerEntity player) {
        return getController(player, COMBAT_LAYER_ID);
    }

    private static PlayerAnimationController getBlockController() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!(client.player instanceof AbstractClientPlayerEntity player)) {
            return null;
        }

        return getController(player, BLOCK_LAYER_ID);
    }

    private static PlayerAnimationController getController(
            AbstractClientPlayerEntity player,
            Identifier layerId
    ) {
        return (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(
                player,
                layerId
        );
    }

    private static void playWithFade(
            PlayerAnimationController controller,
            Identifier animation,
            int fadeTicks
    ) {
        try {
            AbstractFadeModifier fadeModifier =
                    AbstractFadeModifier.standardFadeIn(
                            fadeTicks,
                            EasingType.LINEAR
                    );

            boolean success = controller.replaceAnimationWithFade(
                    fadeModifier,
                    animation
            );

            if (!success) {
                controller.triggerAnimation(animation);
            }
        } catch (Exception e) {
            System.out.println("[KCC] playWithFade failed: " + e);
            controller.triggerAnimation(animation);
        }
    }

    private static void retimeCombatItemAnimation(float speed) {
        if (combatItemAnimation == null || Math.abs(combatItemAnimation.speedMultiplier() - speed) < 0.001F) {
            return;
        }

        combatItemAnimation = new TrackedAnimation(
                combatItemAnimation.kind(),
                combatItemAnimation.direction(),
                animationTicks(),
                speed,
                combatItemAnimation.customAnimationName(),
                combatItemAnimation.elapsedSeconds()
        );
    }

    private static void retimeBlockItemAnimation(float speed) {
        if (blockItemAnimation == null || Math.abs(blockItemAnimation.speedMultiplier() - speed) < 0.001F) {
            return;
        }

        blockItemAnimation = new TrackedAnimation(
                blockItemAnimation.kind(),
                blockItemAnimation.direction(),
                animationTicks(),
                speed,
                blockItemAnimation.customAnimationName(),
                blockItemAnimation.elapsedSeconds()
        );
    }

    private static int sanitizeFadeTicks(int requestedTicks, int fallbackTicks) {
        return requestedTicks > 0 ? requestedTicks : Math.max(1, fallbackTicks);
    }

    private static boolean isContinuingAttackTransition(Identifier animation, boolean chained) {
        return chained
                && animation.equals(currentAnimation)
                && !attackAnimationPlaying;
    }

    private static void tickHandCannonReload(MinecraftClient client) {
        if (client.player == null
                || !client.player.isUsingItem()
                || client.player.getActiveHand() != Hand.MAIN_HAND
                || !client.player.getActiveItem().isOf(ModItems.HAND_CANNON)
                || !HandCannonItem.shouldPlayReloadAnimation(client.player.getActiveItem())) {
            stopHandCannonReloadAnimation();
            handCannonReloadVisualComplete = false;
            return;
        }

        if (handCannonReloadVisualComplete) {
            return;
        }

        if (!handCannonReloadActive) {
            playHandCannonReloadAnimation();
        }

        Optional<Float> elapsed = getLocalHandCannonReloadElapsedSeconds();
        if (elapsed.isEmpty()) {
            return;
        }

        if (elapsed.get() >= GeckoLikeAnimationLibrary.getNamedLengthSeconds(HAND_CANNON_RELOAD_NAME)) {
            stopHandCannonReloadAnimation();
            handCannonReloadVisualComplete = true;
            return;
        }

        spawnHandCannonPowderParticles(client, elapsed.get());
        handCannonReloadLastElapsedSeconds = elapsed.get();
    }

    private static void playHandCannonReloadAnimation() {
        PlayerAnimationController controller = getController();
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack activeStack = client.player == null ? ItemStack.EMPTY : client.player.getActiveItem();
        ItemStack mainHandStack = client.player == null ? ItemStack.EMPTY : client.player.getMainHandStack();
        boolean resumeRamrod = HandCannonItem.canResumeRamrod(activeStack)
                || HandCannonItem.canResumeRamrod(mainHandStack);
        float startOffsetSeconds = resumeRamrod ? HandCannonItem.RAMROD_START_SECONDS : 0.0F;
        setCombatAnimationSpeed(1.0F);
        if (controller != null) {
            if (startOffsetSeconds > 0.0F && !controller.triggerAnimation(HAND_CANNON_RELOAD, startOffsetSeconds)) {
                playWithFade(controller, HAND_CANNON_RELOAD, ATTACK_FADE_TICKS);
            } else if (startOffsetSeconds <= 0.0F) {
                playWithFade(controller, HAND_CANNON_RELOAD, ATTACK_FADE_TICKS);
            }
        }
        currentAnimation = HAND_CANNON_RELOAD;
        attackAnimationPlaying = false;
        combatItemAnimation = new TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind.ATTACK,
                CombatDirection.RIGHT,
                animationTicks(),
                1.0F,
                HAND_CANNON_RELOAD_NAME,
                startOffsetSeconds
        );
        displayedItemTransformUpdatedAt = -1L;
        displayedOffhandItemTransformUpdatedAt = -1L;
        handCannonReloadActive = true;
        handCannonReloadVisualComplete = false;
        handCannonReloadResumedRamrod = resumeRamrod;
        handCannonReloadLastElapsedSeconds = startOffsetSeconds;
        handCannonPowderBurstOne = resumeRamrod;
        handCannonPowderBurstTwo = resumeRamrod;
        bladeTrailActive = false;
    }

    private static void stopHandCannonReloadAnimation() {
        if (!handCannonReloadActive) {
            return;
        }

        handCannonReloadActive = false;
        handCannonReloadResumedRamrod = false;
        handCannonReloadLastElapsedSeconds = 0.0F;
        handCannonPowderBurstOne = false;
        handCannonPowderBurstTwo = false;
        displayedItemTransformUpdatedAt = -1L;
        displayedOffhandItemTransformUpdatedAt = -1L;
        if (currentAnimation != null && currentAnimation.equals(HAND_CANNON_RELOAD)) {
            PlayerAnimationController controller = getController();
            if (controller != null) {
                playWithFade(controller, NEUTRAL, ATTACK_FADE_TICKS);
                currentAnimation = NEUTRAL;
            } else {
                currentAnimation = null;
            }
            combatItemAnimation = null;
        }
    }

    private static Optional<Float> getLocalHandCannonReloadElapsedSeconds() {
        if (!handCannonReloadActive || combatItemAnimation == null) {
            return Optional.empty();
        }
        if (!HAND_CANNON_RELOAD_NAME.equals(combatItemAnimation.customAnimationName())) {
            return Optional.empty();
        }
        return Optional.of(combatItemAnimation.elapsedSeconds());
    }

    private static void spawnHandCannonPowderParticles(MinecraftClient client, float elapsedSeconds) {
        if (client.world == null || client.player == null || handCannonReloadResumedRamrod) {
            return;
        }

        if (!handCannonPowderBurstOne
                && crossedElapsed(handCannonReloadLastElapsedSeconds, elapsedSeconds, 2.2417F)) {
            spawnPowderParticles(client, 14);
            handCannonPowderBurstOne = true;
        }
        if (!handCannonPowderBurstTwo
                && crossedElapsed(handCannonReloadLastElapsedSeconds, elapsedSeconds, 3.0333F)) {
            spawnPowderParticles(client, 14);
            handCannonPowderBurstTwo = true;
        }
        if (elapsedSeconds >= 2.24F && elapsedSeconds <= 3.10F) {
            spawnPowderParticles(client, 4);
        }
    }

    private static boolean crossedElapsed(float previous, float current, float threshold) {
        return previous < threshold && current >= threshold;
    }

    private static void spawnPowderParticles(MinecraftClient client, int count) {
        Vec3d origin = sampleLocalOffhandItemWorldPos(client);
        for (int i = 0; i < count; i++) {
            double spreadX = (client.player.getRandom().nextDouble() - 0.5) * 0.05;
            double spreadZ = (client.player.getRandom().nextDouble() - 0.5) * 0.05;
            double fall = -0.035 - client.player.getRandom().nextDouble() * 0.035;
            client.world.addParticleClient(
                    i % 3 == 0 ? ParticleTypes.POOF : ParticleTypes.ASH,
                    origin.x + spreadX,
                    origin.y,
                    origin.z + spreadZ,
                    spreadX * 0.35,
                    fall,
                    spreadZ * 0.35
            );
        }
    }

    private static Vec3d sampleLocalOffhandItemWorldPos(MinecraftClient client) {
        Vec3d look = client.player.getRotationVec(1.0F);
        Vec3d side = look.crossProduct(new Vec3d(0.0, 1.0, 0.0)).normalize();
        Vec3d base = client.player.getEyePos()
                .add(look.multiply(0.48))
                .add(side.multiply(-0.22))
                .add(0.0, -0.16, 0.0);

        Optional<GeckoLikeAnimationLibrary.BoneTransform> transform = getLocalPlayerOffhandItemTransform();
        if (transform.isEmpty() || transform.get().position().isEmpty()) {
            return base;
        }

        GeckoLikeAnimationLibrary.BonePose position = transform.get().position().get();
        Vec3d local = new Vec3d(position.x(), position.y(), position.z()).multiply(1.0 / 16.0);
        return base.add(modelToWorld(local, client.player.getYaw()));
    }

    private static Vec3d modelToWorld(Vec3d local, float yawDegrees) {
        double yawRadians = Math.toRadians(yawDegrees);
        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);
        return new Vec3d(
                local.x * cosYaw + local.z * sinYaw,
                local.y,
                local.x * sinYaw - local.z * cosYaw
        );
    }

    private static boolean isHeldHandContext(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    public static Optional<Float> getLocalAttackAnimationElapsedSeconds() {
        if (!attackAnimationPlaying || combatItemAnimation == null) {
            return Optional.empty();
        }

        return Optional.of(combatItemAnimation.elapsedSeconds());
    }

    private record TrackedAnimation(
            GeckoLikeAnimationLibrary.Kind kind,
            CombatDirection direction,
            double startedAtTicks,
            float speedMultiplier,
            String customAnimationName,
            float startOffsetSeconds
    ) {
        private TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind kind,
                CombatDirection direction,
                double startedAtTicks,
                float speedMultiplier
        ) {
            this(kind, direction, startedAtTicks, speedMultiplier, null, 0.0F);
        }

        private TrackedAnimation(
                GeckoLikeAnimationLibrary.Kind kind,
                CombatDirection direction,
                double startedAtTicks,
                float speedMultiplier,
                String customAnimationName
        ) {
            this(kind, direction, startedAtTicks, speedMultiplier, customAnimationName, 0.0F);
        }

        float elapsedSeconds() {
            return startOffsetSeconds
                    + (float) ((animationTicks() - startedAtTicks) / 20.0) * speedMultiplier;
        }
    }

    private static double animationTicks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return 0.0;
        }

        return client.world.getTime()
                + client.getRenderTickCounter().getTickProgress(false);
    }
}
