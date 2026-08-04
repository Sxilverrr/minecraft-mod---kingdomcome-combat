package com.kingdomcomecombat.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/** A short-lived, gravity-free blood mark emitted along an intersecting weapon trace. */
public final class BloodTraceParticle extends SpriteBillboardParticle {
    private BloodTraceParticle(ClientWorld world, double x, double y, double z,
                               double velocityX, double velocityY, double velocityZ,
                               SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.scale = 0.026F + world.random.nextFloat() * 0.016F;
        this.maxAge = 18 + world.random.nextInt(7);
        this.gravityStrength = 0.0F;
        this.velocityMultiplier = 0.94F;
        this.collidesWithWorld = false;
        this.setBoundingBoxSpacing(0.01F, 0.01F);
        this.setColor(1.0F, 1.0F, 1.0F);
        this.setAlpha(0.34F);
        this.setSprite(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientWorld world,
                double x,
                double y,
                double z,
                double velocityX,
                double velocityY,
                double velocityZ
        ) {
            return new BloodTraceParticle(world, x, y, z,
                    velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}
