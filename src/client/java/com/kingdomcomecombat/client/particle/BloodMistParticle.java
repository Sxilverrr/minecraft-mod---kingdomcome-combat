package com.kingdomcomecombat.client.particle;

import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class BloodMistParticle extends SpriteBillboardParticle {
    private static final float BASE_ALPHA = 0.22F * 1.8F;
    private final SpriteProvider spriteProvider;
    private final float initialAlpha;

    private BloodMistParticle(
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
        this.spriteProvider = spriteProvider;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.scale = 0.16F + world.random.nextFloat() * 0.12F;
        this.maxAge = 18 + world.random.nextInt(10);
        this.gravityStrength = 0.0F;
        this.velocityMultiplier = 0.90F;
        this.collidesWithWorld = false;
        this.setColor(0.62F, 0.05F, 0.045F);
        this.initialAlpha = Math.min(1.0F, BASE_ALPHA * (float) CombatClientConfig.bloodMistOpacity());
        this.setAlpha(this.initialAlpha);
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.dead) {
            float life = this.age / (float) this.maxAge;
            this.setAlpha(this.initialAlpha * (1.0F - life));
            this.setSpriteForAge(this.spriteProvider);
        }
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
            return new BloodMistParticle(
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
