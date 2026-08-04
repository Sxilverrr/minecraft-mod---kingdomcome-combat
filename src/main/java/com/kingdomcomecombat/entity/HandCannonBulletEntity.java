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
import com.kingdomcomecombat.item.HandCannonItem;

import java.util.Comparator;

public class HandCannonBulletEntity extends ProjectileEntity {
    private static final int MAX_AGE = 100;
    private static final double HIT_EXPANSION = 0.3;
    private double damage = 2.0;
    private HandCannonItem.AmmoType ammoType = HandCannonItem.AmmoType.NORMAL;
    private double distanceTravelled;
    private int flameLevel;
    private int punchLevel;

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

    public void setAmmoType(HandCannonItem.AmmoType ammoType) {
        this.ammoType = ammoType == null ? HandCannonItem.AmmoType.NORMAL : ammoType;
    }

    public HandCannonItem.AmmoType getAmmoType() {
        return this.ammoType;
    }

    public void setFlameLevel(int flameLevel) { this.flameLevel = Math.max(0, flameLevel); }
    public void setPunchLevel(int punchLevel) { this.punchLevel = Math.max(0, punchLevel); }

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
        if (this.ammoType == HandCannonItem.AmmoType.BUCKSHOT && this.distanceTravelled >= 20.0) {
            this.discard();
            return;
        }
        double rayLength = velocity.length();
        if (this.ammoType == HandCannonItem.AmmoType.BUCKSHOT) {
            rayLength = Math.min(rayLength, 20.0 - this.distanceTravelled);
        }
        Vec3d end = start.add(velocity.normalize().multiply(rayLength));
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
        this.distanceTravelled += velocity.length();
        double gravity = this.ammoType == HandCannonItem.AmmoType.HEAVY ? 0.065 : 0.015;
        this.setVelocity(velocity.multiply(0.992).add(0.0, -gravity, 0.0));
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
                    Vec3d hitPos = target.getBoundingBox().expand(HIT_EXPANSION).raycast(start, end)
                            .orElse(target.getBoundingBox().getCenter());
                    DamageSource source = this.getDamageSources().thrown(this, owner);
                    target.damage((ServerWorld) this.getWorld(), source,
                            impactDamage(this.distanceTravelled + start.distanceTo(hitPos)));
                    if (this.flameLevel > 0) {
                        target.setOnFireFor(5.0F);
                    }
                    if (this.punchLevel > 0) {
                        Vec3d punch = this.getVelocity().normalize().multiply(0.6 * this.punchLevel);
                        target.addVelocity(punch.x, 0.1 * this.punchLevel, punch.z);
                        target.velocityModified = true;
                    }
                    if (this.ammoType == HandCannonItem.AmmoType.HEAVY) {
                        Vec3d push = this.getVelocity().normalize().multiply(1.85).add(0.0, 0.28, 0.0);
                        target.addVelocity(push.x, push.y, push.z);
                        target.velocityModified = true;
                    }
                    spawnImpactSmoke(hitPos);
                    this.discard();
                    return true;
                })
                .orElse(false);
    }

    private float impactDamage(double impactDistance) {
        double speed = this.getVelocity().length();
        double result;
        if (this.ammoType == HandCannonItem.AmmoType.HEAVY) {
            result = Math.max(2.0, Math.ceil((speed / 0.7) * 2.0)) * 1.4;
        } else if (this.ammoType == HandCannonItem.AmmoType.BUCKSHOT) {
            result = Math.max(2.0, Math.ceil(speed * 2.0)) * 0.3;
        } else {
            result = Math.max(this.damage, Math.ceil(speed * this.damage));
        }
        if (this.ammoType == HandCannonItem.AmmoType.BUCKSHOT) {
            if (impactDistance >= 20.0) return 0.0F;
            if (impactDistance > 6.0 && impactDistance < 14.0) {
                result *= 1.0 - ((impactDistance - 6.0) / 8.0) * 0.8;
            } else if (impactDistance >= 14.0) {
                result *= 0.2;
            }
        }
        return (float) result;
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
        this.distanceTravelled = view.getDouble("distance_travelled", 0.0);
        this.flameLevel = view.getInt("flame_level", 0);
        this.punchLevel = view.getInt("punch_level", 0);
        try {
            this.ammoType = HandCannonItem.AmmoType.valueOf(view.getString("ammo_type", HandCannonItem.AmmoType.NORMAL.name()));
        } catch (IllegalArgumentException ignored) {
            this.ammoType = HandCannonItem.AmmoType.NORMAL;
        }
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putDouble("damage", this.damage);
        view.putDouble("distance_travelled", this.distanceTravelled);
        view.putInt("flame_level", this.flameLevel);
        view.putInt("punch_level", this.punchLevel);
        view.putString("ammo_type", this.ammoType.name());
    }
}
