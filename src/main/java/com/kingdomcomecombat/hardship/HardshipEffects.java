package com.kingdomcomecombat.hardship;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.stamina.ServerStaminaState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.village.TradeOffer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Server-authoritative runtime for the data-pack-defined hardship preks. */
public final class HardshipEffects {
    private static final Identifier BAREFOOT_SPEED = id("barefoot_speed");
    private static final Identifier CAVE_SPEED = id("cave_speed");
    private static final Identifier CROUCH_SPEED = id("crouch_speed");
    private static final Identifier BLOCK_REACH = id("block_reach");
    private static final Identifier MINING_SPEED = id("mining_speed");
    private static final Map<UUID, Vec3d> LAST_POSITION = new HashMap<>();
    private static final Map<UUID, Double> BOOT_DISTANCE = new HashMap<>();
    private static final Map<UUID, Integer> CROUCH_LINGER = new HashMap<>();
    private static final Map<UUID, Integer> LAST_AIR = new HashMap<>();
    private static final Map<UUID, Integer> LAST_FOOD = new HashMap<>();
    private static final Map<UUID, Float> LAST_SATURATION = new HashMap<>();
    private static final Map<UUID, Double> FOOD_REMAINDER = new HashMap<>();
    private static final Map<UUID, Double> EXPERIENCE_REMAINDER = new HashMap<>();
    private static final Map<UUID, Double> TOOL_DURABILITY_REMAINDER = new HashMap<>();
    private static final Map<UUID, Double> ARMOR_DURABILITY_REMAINDER = new HashMap<>();
    private static final Set<TradeOffer> MARKED_UP_OFFERS = Collections.newSetFromMap(new WeakHashMap<>());

    private HardshipEffects() {}

    public static void clearPlayer(UUID playerUuid) {
        LAST_POSITION.remove(playerUuid);
        BOOT_DISTANCE.remove(playerUuid);
        CROUCH_LINGER.remove(playerUuid);
        LAST_AIR.remove(playerUuid);
        LAST_FOOD.remove(playerUuid);
        LAST_SATURATION.remove(playerUuid);
        FOOD_REMAINDER.remove(playerUuid);
        EXPERIENCE_REMAINDER.remove(playerUuid);
        TOOL_DURABILITY_REMAINDER.remove(playerUuid);
        ARMOR_DURABILITY_REMAINDER.remove(playerUuid);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HardshipEffects::tick);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer
                    && entity instanceof MerchantEntity merchant
                    && HardshipSelectionState.active(serverPlayer, "hardship_06")) {
                double multiplier = HardshipSelectionState.prek(
                        serverPlayer, "hardship_06", "villager_price_multiplier", 2.0);
                for (TradeOffer offer : merchant.getOffers()) {
                    if (MARKED_UP_OFFERS.add(offer)) {
                        int baseCount = offer.getOriginalFirstBuyItem().getCount();
                        offer.increaseSpecialPrice(Math.max(0, (int) Math.ceil(baseCount * (multiplier - 1.0))));
                    }
                }
            }
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            onPlayerDamaged(entity, damage);
        });
    }

    private static void onPlayerDamaged(net.minecraft.entity.LivingEntity entity, float damage) {
        if (!(entity instanceof ServerPlayerEntity player) || damage <= 0.0F) return;
        if (HardshipSelectionState.active(player, "hardship_15")) {
            float trueDamage = (float) HardshipSelectionState.prek(
                    player, "hardship_15", "unblocked_true_damage", 1.0);
            if (trueDamage > 0.0F) player.setHealth(Math.max(0.0F, player.getHealth() - trueDamage));
        }
        if (HardshipSelectionState.active(player, "hardship_20")
                && player.getRandom().nextDouble() < HardshipSelectionState.prek(
                        player, "hardship_20", "bleeding_chance_on_damage", 0.5)) {
            ModStatusEffects.applyInjury(player, "bleeding", 1);
        }
    }

    private static void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        tickFootwear(player);
        tickSleepDeprivation(player);
        tickNightBlindness(player);
        tickCavePhobia(player);
        tickWeakLegs(player);
        tickReachAndMining(player);
        tickAchingBack(player);
        tickSwimming(player);
        tickFoodGain(player);
        if (player.age % 20 == 0) tickHostileDetection(player);
    }

    private static void tickSleepDeprivation(ServerPlayerEntity player) {
        if (!HardshipSelectionState.active(player, "hardship_02")) return;
        int threshold = Math.max(1, (int) HardshipSelectionState.prek(
                player, "hardship_02", "phantom_sleepless_ticks", 48000.0));
        int timeSinceRest = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
        if (timeSinceRest >= threshold && timeSinceRest < 72000) {
            player.getStatHandler().setStat(player, Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST), 72000);
        }
    }

    private static void tickFoodGain(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int food = player.getHungerManager().getFoodLevel();
        float saturation = player.getHungerManager().getSaturationLevel();
        Integer previousFood = LAST_FOOD.put(uuid, food);
        Float previousSaturation = LAST_SATURATION.put(uuid, saturation);
        if (!HardshipSelectionState.active(player, "hardship_12")
                || previousFood == null || previousSaturation == null) return;
        if (food > previousFood) {
            double multiplier = HardshipSelectionState.prek(player, "hardship_12", "food_nutrition_multiplier", 0.5);
            double scaled = (food - previousFood) * multiplier + FOOD_REMAINDER.getOrDefault(uuid, 0.0);
            int accepted = (int) Math.floor(scaled);
            FOOD_REMAINDER.put(uuid, scaled - accepted);
            food = previousFood + accepted;
            player.getHungerManager().setFoodLevel(food);
        }
        if (saturation > previousSaturation) {
            double multiplier = HardshipSelectionState.prek(player, "hardship_12", "food_saturation_multiplier", 0.5);
            double scaled = (saturation - previousSaturation) * multiplier;
            saturation = previousSaturation + (float) scaled;
            player.getHungerManager().setSaturationLevel(Math.min(saturation, food));
        }
        LAST_FOOD.put(uuid, player.getHungerManager().getFoodLevel());
        LAST_SATURATION.put(uuid, player.getHungerManager().getSaturationLevel());
    }

    private static void tickFootwear(ServerPlayerEntity player) {
        boolean active = HardshipSelectionState.active(player, "hardship_01");
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        setMultiplier(player, EntityAttributes.MOVEMENT_SPEED, BAREFOOT_SPEED,
                active && boots.isEmpty()
                        ? HardshipSelectionState.prek(player, "hardship_01", "barefoot_speed_multiplier", 0.8)
                        : 1.0);
        Vec3d now = player.getPos();
        Vec3d before = LAST_POSITION.put(player.getUuid(), now);
        if (!active || before == null || boots.isEmpty() || !boots.isDamageable()
                || player.hasVehicle() || player.isGliding()) return;
        double travelled = Math.hypot(now.x - before.x, now.z - before.z);
        if (travelled <= 0.0 || travelled > 8.0) return;
        double total = BOOT_DISTANCE.getOrDefault(player.getUuid(), 0.0) + travelled;
        double threshold = HardshipSelectionState.prek(player, "hardship_01",
                boots.isOf(Items.LEATHER_BOOTS) ? "leather_boot_durability_distance" : "boot_durability_distance",
                boots.isOf(Items.LEATHER_BOOTS) ? 100.0 : 10.0);
        while (threshold > 0.0 && total >= threshold && !boots.isEmpty()) {
            boots.damage(1, player, EquipmentSlot.FEET);
            total -= threshold;
        }
        BOOT_DISTANCE.put(player.getUuid(), total);
    }

    private static void tickNightBlindness(ServerPlayerEntity player) {
        if (!HardshipSelectionState.active(player, "hardship_03")) return;
        int threshold = (int) HardshipSelectionState.prek(player, "hardship_03", "blindness_light_threshold", 2.0);
        if (player.getWorld().getLightLevel(player.getBlockPos()) < threshold) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 30, 0, false, false, true));
        }
    }

    private static void tickCavePhobia(ServerPlayerEntity player) {
        boolean cave = HardshipSelectionState.active(player, "hardship_07") && player.getY() < 0.0;
        setMultiplier(player, EntityAttributes.MOVEMENT_SPEED, CAVE_SPEED,
                cave ? HardshipSelectionState.prek(player, "hardship_07", "underground_speed_multiplier", 0.9) : 1.0);
    }

    private static void tickWeakLegs(ServerPlayerEntity player) {
        if (!HardshipSelectionState.active(player, "hardship_08")) return;
        if (player.isSprinting()) {
            double perSecond = HardshipSelectionState.prek(player, "hardship_08", "sprint_stamina_per_second", 4.0);
            if (!ServerStaminaState.consume(player, perSecond / 20.0)) player.setSprinting(false);
        }
    }

    private static void tickReachAndMining(ServerPlayerEntity player) {
        setAdditive(player, EntityAttributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH,
                HardshipSelectionState.active(player, "hardship_10")
                        ? -HardshipSelectionState.prek(player, "hardship_10", "block_interaction_range_penalty", 1.0)
                        : 0.0);
        setMultiplier(player, EntityAttributes.BLOCK_BREAK_SPEED, MINING_SPEED,
                HardshipSelectionState.active(player, "hardship_13")
                        ? HardshipSelectionState.prek(player, "hardship_13", "mining_speed_multiplier", 0.7)
                        : 1.0);
    }

    private static void tickAchingBack(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int linger = CROUCH_LINGER.getOrDefault(uuid, 0);
        if (HardshipSelectionState.active(player, "hardship_14") && player.isSneaking()) {
            linger = (int) HardshipSelectionState.prek(player, "hardship_14", "sneaking_penalty_linger_ticks", 20.0);
        } else if (linger > 0) {
            linger--;
        }
        if (linger > 0) CROUCH_LINGER.put(uuid, linger); else CROUCH_LINGER.remove(uuid);
        setMultiplier(player, EntityAttributes.MOVEMENT_SPEED, CROUCH_SPEED,
                linger > 0 ? HardshipSelectionState.prek(player, "hardship_14", "sneaking_speed_multiplier", 0.7) : 1.0);
    }

    private static void tickSwimming(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int air = player.getAir();
        int previous = LAST_AIR.getOrDefault(uuid, air);
        if (HardshipSelectionState.active(player, "hardship_18") && player.isSwimming()) {
            double speed = HardshipSelectionState.prek(player, "hardship_18", "swim_speed_multiplier", 0.7);
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x * speed, velocity.y, velocity.z * speed);
            if (air < previous && player.age % 2 == 0) player.setAir(Math.max(-20, air - 1));
        }
        LAST_AIR.put(uuid, player.getAir());
    }

    private static void tickHostileDetection(ServerPlayerEntity player) {
        if (!HardshipSelectionState.active(player, "hardship_09")) return;
        double multiplier = HardshipSelectionState.prek(player, "hardship_09", "hostile_detection_multiplier", 1.5);
        double radius = 48.0 * multiplier;
        for (HostileEntity hostile : player.getWorld().getEntitiesByClass(
                HostileEntity.class, player.getBoundingBox().expand(radius),
                mob -> mob.isAlive() && mob.getTarget() == null && mob.canSee(player))) {
            double normal = hostile.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
            if (hostile.squaredDistanceTo(player) <= normal * normal * multiplier * multiplier) hostile.setTarget(player);
        }
    }

    public static boolean tryConsumeJumpStamina(ServerPlayerEntity player) {
        if (!HardshipSelectionState.active(player, "hardship_08")) return true;
        return ServerStaminaState.consume(player,
                HardshipSelectionState.prek(player, "hardship_08", "jump_stamina_cost", 10.0));
    }

    public static int scaleExperienceGain(ServerPlayerEntity player, int amount) {
        if (amount <= 0 || !HardshipSelectionState.active(player, "hardship_11")) return amount;
        UUID uuid = player.getUuid();
        double scaled = amount * HardshipSelectionState.prek(
                player, "hardship_11", "experience_gain_multiplier", 0.7)
                + EXPERIENCE_REMAINDER.getOrDefault(uuid, 0.0);
        int accepted = (int) Math.floor(scaled);
        EXPERIENCE_REMAINDER.put(uuid, scaled - accepted);
        return accepted;
    }

    public static int scaleDurabilityDamage(ServerPlayerEntity player, int amount, boolean armor) {
        if (amount <= 0 || !HardshipSelectionState.active(player, "hardship_19")) return amount;
        Map<UUID, Double> remainders = armor ? ARMOR_DURABILITY_REMAINDER : TOOL_DURABILITY_REMAINDER;
        UUID uuid = player.getUuid();
        double multiplier = HardshipSelectionState.prek(player, "hardship_19",
                armor ? "armor_durability_multiplier" : "tool_durability_multiplier", armor ? 1.5 : 2.0);
        double scaled = amount * multiplier + remainders.getOrDefault(uuid, 0.0);
        int accepted = Math.max(1, (int) Math.floor(scaled));
        remainders.put(uuid, Math.max(0.0, scaled - accepted));
        return accepted;
    }

    public static double staminaRegenerationMultiplier(ServerPlayerEntity player) {
        return HardshipSelectionState.active(player, "hardship_07") && player.getY() < 0.0
                ? HardshipSelectionState.prek(player, "hardship_07", "underground_stamina_regen_multiplier", 0.6)
                : 1.0;
    }

    public static void teleportAfterSleep(ServerPlayerEntity player) {
        if (!HardshipSelectionState.active(player, "hardship_02")
                || !(player.getWorld() instanceof ServerWorld world)) return;
        int radius = Math.max(1, (int) HardshipSelectionState.prek(player, "hardship_02", "wake_teleport_radius", 200.0));
        BlockPos origin = player.getBlockPos();
        for (int attempt = 0; attempt < 64; attempt++) {
            int x = origin.getX() + player.getRandom().nextInt(radius * 2 + 1) - radius;
            int z = origin.getZ() + player.getRandom().nextInt(radius * 2 + 1) - radius;
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!world.isSkyVisible(feet) || !world.getBlockState(feet).isAir()
                    || !world.getBlockState(feet.up()).isAir()) continue;
            player.requestTeleport(x + 0.5, y, z + 0.5);
            return;
        }
        KingdomComeCombat.LOGGER.warn("Could not find a safe sleepwalking destination for {}", player.getName().getString());
    }

    private static void setMultiplier(ServerPlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                                      Identifier id, double multiplier) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (Math.abs(multiplier - 1.0) > 1.0E-6) instance.addTemporaryModifier(new EntityAttributeModifier(
                id, multiplier - 1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void setAdditive(ServerPlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                                    Identifier id, double value) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (Math.abs(value) > 1.0E-6) instance.addTemporaryModifier(new EntityAttributeModifier(
                id, value, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static Identifier id(String path) { return Identifier.of(KingdomComeCombat.MOD_ID, "hardship_" + path); }
}
