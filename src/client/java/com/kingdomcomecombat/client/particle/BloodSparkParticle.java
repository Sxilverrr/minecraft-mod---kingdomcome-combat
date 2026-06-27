package com.kingdomcomecombat.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class BloodSparkParticle extends SpriteBillboardParticle {
    private static final float TWO_PIXEL_SCALE = 1.0F / 16.0F;

    private BloodSparkParticle(
            ClientWorld world,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteProvider spriteProvider
    ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        // Billboard width is scale * 2, so this renders as 2/16 of a block.
        this.scale = TWO_PIXEL_SCALE;
        this.maxAge = 16 + world.random.nextInt(9);
        this.gravityStrength = 0.025F;
        this.velocityMultiplier = 0.88F;
        this.collidesWithWorld = false;
        this.setBoundingBoxSpacing(2.0F / 16.0F, 2.0F / 16.0F);
        this.setColor(0.72F, 0.015F, 0.01F);
        this.setAlpha(1.0F);
        this.setSprite(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
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
            return new BloodSparkParticle(
                    world,
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ,
                    spriteProvider
            );
        }
    }
}
