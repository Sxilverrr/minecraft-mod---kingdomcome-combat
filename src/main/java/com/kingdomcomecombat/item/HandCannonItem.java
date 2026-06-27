package com.kingdomcomecombat.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import com.kingdomcomecombat.entity.HandCannonBulletEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HandCannonItem extends Item {
    private static final String LOADED_KEY = "kcc_hand_cannon_loaded";
    private static final String POWDER_LOADED_KEY = "kcc_hand_cannon_powder_loaded";
    private static final String FUSE_TICKS_KEY = "kcc_hand_cannon_fuse_ticks";
    private static final String FUSE_TOTAL_KEY = "kcc_hand_cannon_fuse_total";
    private static final String LOAD_LOCK_TICKS_KEY = "kcc_hand_cannon_load_lock_ticks";
    public static final int RELOAD_TICKS = 150;
    public static final int POWDER_LOAD_TICKS = 52;
    public static final int RAMROD_TICKS = RELOAD_TICKS - POWDER_LOAD_TICKS;
    public static final float RAMROD_START_SECONDS = POWDER_LOAD_TICKS / 20.0F;
    public static final int RAMROD_RENDER_TICKS = 70;
    private static final int LOAD_LOCK_TICKS = 14;
    private static final int MIN_FUSE_TICKS = 60;
    private static final int MAX_FUSE_TICKS = 100;
    private static final double PROJECTILE_DAMAGE = 2.0;
    private static final float PROJECTILE_SPEED = 5.2F;
    private static final float PROJECTILE_DIVERGENCE = 16.0F;

    public HandCannonItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (fuseTicks(stack) > 0) {
            return ActionResult.CONSUME;
        }
        if (loadLockTicks(stack) > 0) {
            return ActionResult.CONSUME;
        }
        if (isLoaded(stack)) {
            if (!world.isClient && user instanceof ServerPlayerEntity player) {
                startFuse(player, stack);
            }
            return ActionResult.CONSUME;
        }
        if (!isPowderLoaded(stack) && !user.getOffHandStack().isOf(ModItems.BULLET_WITH_GUNPOWDER)) {
            if (!world.isClient) {
                user.sendMessage(net.minecraft.text.Text.literal("副手需要火枪弹药"), true);
            }
            return ActionResult.FAIL;
        }
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return isPowderLoaded(stack) ? RAMROD_TICKS : RELOAD_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (shouldPlayReloadAnimation(stack)) {
            if (user instanceof PlayerEntity player) {
                Vec3d velocity = player.getVelocity();
                player.setVelocity(0.0, velocity.y, 0.0);
                player.velocityModified = true;
            }
            if (!world.isClient
                    && !isPowderLoaded(stack)
                    && user instanceof ServerPlayerEntity player) {
                int usedTicks = getMaxUseTime(stack, user) - remainingUseTicks;
                if (usedTicks >= POWDER_LOAD_TICKS) {
                    ItemStack ammo = player.getOffHandStack();
                    if (ammo.isOf(ModItems.BULLET_WITH_GUNPOWDER)) {
                        ammo.decrementUnlessCreative(1, player);
                        setPowderLoaded(stack, true);
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.PLAYERS, 0.85F, 0.65F);
                    }
                }
            }
        } else if (isFuseLit(stack) && world instanceof ServerWorld serverWorld && remainingUseTicks % 12 == 0) {
            spawnFuseSparks(serverWorld, user, 2);
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            finishReload(world, player, stack);
        }
        return stack;
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient
                && remainingUseTicks <= 0
                && user instanceof ServerPlayerEntity player) {
            finishReload(world, player, stack);
            return true;
        }

        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, net.minecraft.entity.EquipmentSlot slot) {
        int lockTicks = loadLockTicks(stack);
        if (lockTicks > 0) {
            setLoadLockTicks(stack, lockTicks - 1);
        }
        if (fuseTicks(stack) <= 0) {
            return;
        }
        if (!(entity instanceof ServerPlayerEntity player) || player.getMainHandStack() != stack) {
            backfire(world, entity, stack);
            return;
        }
        int ticks = fuseTicks(stack) - 1;
        setFuseTicks(stack, ticks, fuseTotal(stack));
        spawnFuseSparks(world, player, 3);
        if (ticks <= 0) {
            fire(world, player, stack);
        }
    }

    public static boolean isLoaded(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(LOADED_KEY, false);
    }

    public static boolean isPowderLoaded(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(POWDER_LOADED_KEY, false);
    }

    public static boolean shouldPlayReloadAnimation(ItemStack stack) {
        return !isLoaded(stack) && !isFuseLit(stack);
    }

    public static boolean canResumeRamrod(ItemStack stack) {
        return !isLoaded(stack) && !isFuseLit(stack) && isPowderLoaded(stack);
    }

    private static int fuseTicks(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? 0 : data.copyNbt().getInt(FUSE_TICKS_KEY, 0);
    }

    public static boolean isFuseLit(ItemStack stack) {
        return fuseTicks(stack) > 0;
    }

    private static int fuseTotal(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? 0 : data.copyNbt().getInt(FUSE_TOTAL_KEY, 0);
    }

    private static int loadLockTicks(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? 0 : data.copyNbt().getInt(LOAD_LOCK_TICKS_KEY, 0);
    }

    private static void setLoaded(ItemStack stack, boolean loaded) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putBoolean(LOADED_KEY, loaded);
            if (loaded) {
                nbt.remove(POWDER_LOADED_KEY);
            }
            nbt.remove(FUSE_TICKS_KEY);
            nbt.remove(FUSE_TOTAL_KEY);
            if (!loaded) {
                nbt.remove(LOAD_LOCK_TICKS_KEY);
                nbt.remove(POWDER_LOADED_KEY);
            }
        });
    }

    private static void setPowderLoaded(ItemStack stack, boolean powderLoaded) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (powderLoaded) {
                nbt.putBoolean(POWDER_LOADED_KEY, true);
            } else {
                nbt.remove(POWDER_LOADED_KEY);
            }
        });
    }

    private static void setLoadLockTicks(ItemStack stack, int ticks) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (ticks > 0) {
                nbt.putInt(LOAD_LOCK_TICKS_KEY, ticks);
            } else {
                nbt.remove(LOAD_LOCK_TICKS_KEY);
            }
        });
    }

    private static void finishReload(World world, ServerPlayerEntity player, ItemStack stack) {
        if (isLoaded(stack) || !isPowderLoaded(stack)) {
            return;
        }

        setLoaded(stack, true);
        setLoadLockTicks(stack, LOAD_LOCK_TICKS);
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_CROSSBOW_LOADING_END,
                SoundCategory.PLAYERS,
                0.9F,
                0.7F
        );
    }

    private static void startFuse(ServerPlayerEntity player, ItemStack stack) {
        int ticks = MIN_FUSE_TICKS + player.getRandom().nextInt(MAX_FUSE_TICKS - MIN_FUSE_TICKS + 1);
        setFuseTicks(stack, ticks, ticks);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 1.0F, 0.8F);
    }

    private static void setFuseTicks(ItemStack stack, int ticks, int total) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putBoolean(LOADED_KEY, true);
            if (ticks > 0) {
                nbt.putInt(FUSE_TICKS_KEY, ticks);
                nbt.putInt(FUSE_TOTAL_KEY, total);
            } else {
                nbt.remove(FUSE_TICKS_KEY);
                nbt.remove(FUSE_TOTAL_KEY);
            }
        });
    }

    private static void fire(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
        Vec3d muzzle = muzzlePos(player);
        spawnMuzzleBlast(world, muzzle);
        HandCannonBulletEntity projectile = new HandCannonBulletEntity(world, player);
        projectile.setPosition(muzzle.x, muzzle.y, muzzle.z);
        projectile.setDamage(PROJECTILE_DAMAGE);
        projectile.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, PROJECTILE_SPEED, PROJECTILE_DIVERGENCE);
        world.spawnEntity(projectile);
        HandCannonProjectileTracker.track(projectile);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 3.0F, 0.65F);
        stack.damage(3, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        setLoaded(stack, false);
    }

    private static void backfire(ServerWorld world, Entity entity, ItemStack stack) {
        Vec3d pos = entity == null ? Vec3d.ZERO : entity.getPos().add(0.0, Math.max(0.2, entity.getHeight() * 0.55), 0.0);
        spawnMuzzleBlast(world, pos);
        if (entity instanceof LivingEntity living) {
            living.damage(world, world.getDamageSources().explosion(null, living), 8.0F);
        }
        setLoaded(stack, false);
    }

    private static Vec3d muzzlePos(PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0F);
        return player.getEyePos().add(look.multiply(0.9)).add(0.0, -0.12, 0.0);
    }

    private static void spawnFuseSparks(ServerWorld world, LivingEntity entity, int count) {
        Vec3d pos = entity.getEyePos().add(entity.getRotationVec(1.0F).multiply(0.55)).add(0.0, -0.18, 0.0);
        world.spawnParticles(ParticleTypes.SMALL_FLAME, pos.x, pos.y, pos.z, count, 0.05, 0.04, 0.05, 0.01);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, count, 0.06, 0.05, 0.06, 0.03);
    }

    private static void spawnMuzzleBlast(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 18, 0.25, 0.18, 0.25, 0.08);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 35, 0.35, 0.25, 0.35, 0.16);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 45, 0.45, 0.35, 0.45, 0.05);
        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.0);
    }
}
