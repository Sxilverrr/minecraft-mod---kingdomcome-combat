package com.kingdomcomecombat.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Comparator;

public class HandCannonBulletEntity extends ProjectileEntity {
    private static final int MAX_AGE = 100;
    private static final double HIT_EXPANSION = 0.3;
    private double damage = 2.0;

    public HandCannonBulletEntity(EntityType<? extends HandCannonBulletEntity> type, World world) {
        super(type, world);
        this.noClip = false;
    }

    public HandCannonBulletEntity(World world, LivingEntity owner) {
        this(ModEntities.HAND_CANNON_BULLET, world);
        this.setOwner(owner);
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    @Override
    protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        if (this.age > MAX_AGE || this.getVelocity().lengthSquared() < 0.01) {
            this.discard();
            return;
        }

        Vec3d start = this.getPos();
        Vec3d velocity = this.getVelocity();
        Vec3d end = start.add(velocity);
        BlockHitResult blockHit = this.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this
        ));
        Vec3d entityRayEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();
        if (tryHitEntity(start, entityRayEnd)) {
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS && handleBlockHit(blockHit)) {
            return;
        }

        this.move(MovementType.SELF, velocity);
        this.setVelocity(velocity.multiply(0.992).add(0.0, -0.015, 0.0));
    }

    private boolean tryHitEntity(Vec3d start, Vec3d end) {
        Box searchBox = this.getBoundingBox().stretch(this.getVelocity()).expand(HIT_EXPANSION);
        Entity owner = this.getOwner();
        return this.getWorld().getOtherEntities(this, searchBox, entity ->
                        entity.isAlive()
                                && entity.canHit()
                                && entity != owner
                                && !(owner != null && entity.isConnectedThroughVehicle(owner))
                                && entity.getBoundingBox().expand(HIT_EXPANSION).raycast(start, end).isPresent()
                )
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(start)))
                .map(target -> {
                    Vec3d hitPos = target.getBoundingBox().getCenter();
                    DamageSource source = this.getDamageSources().thrown(this, owner);
                    target.damage((ServerWorld) this.getWorld(), source, impactDamage());
                    spawnImpactSmoke(hitPos);
                    this.discard();
                    return true;
                })
                .orElse(false);
    }

    private float impactDamage() {
        return (float) Math.max(this.damage, Math.ceil(this.getVelocity().length() * this.damage));
    }

    private boolean handleBlockHit(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = this.getWorld().getBlockState(pos);
        if (state.isIn(BlockTags.LEAVES)) {
            this.getWorld().breakBlock(pos, false, this);
            spawnBlockImpactSmoke(hit.getPos());
            this.setPosition(hit.getPos().add(this.getVelocity().normalize().multiply(0.08)));
            return false;
        }
        spawnBlockImpactSmoke(hit.getPos());
        this.discard();
        return true;
    }

    private void spawnImpactSmoke(Vec3d pos) {
        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 18, 0.18, 0.18, 0.18, 0.035);
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 12, 0.12, 0.12, 0.12, 0.02);
        }
    }

    private void spawnBlockImpactSmoke(Vec3d pos) {
        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 46, 0.42, 0.32, 0.42, 0.065);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 26, 0.34, 0.24, 0.34, 0.045);
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 34, 0.28, 0.20, 0.28, 0.035);
        }
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        this.damage = view.getDouble("damage", 2.0);
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putDouble("damage", this.damage);
    }
}
