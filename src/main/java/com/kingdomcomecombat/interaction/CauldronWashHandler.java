package com.kingdomcomecombat.interaction;

import com.kingdomcomecombat.equipment.BloodiedEntityAccess;
import com.kingdomcomecombat.equipment.BloodiedEquipment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class CauldronWashHandler {
    private CauldronWashHandler() {
    }

    public static boolean canWash(PlayerEntity player, World world, BlockPos pos) {
        if (player == null || world == null || pos == null || !player.isSneaking()) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        return state.isOf(Blocks.WATER_CAULDRON) && state.get(LeveledCauldronBlock.LEVEL) > 0;
    }

    public static boolean wash(ServerPlayerEntity player, BlockPos pos) {
        if (!(player.getWorld() instanceof ServerWorld world)
                || !canWash(player, world, pos)
                || player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 4.0 * 4.0) {
            return false;
        }

        BloodiedEquipment.clearBlood(player);
        if (player instanceof BloodiedEntityAccess bloodied) {
            bloodied.kingdomcomecombat$clearBodyBlood();
        }

        BlockState state = world.getBlockState(pos);
        int level = state.get(LeveledCauldronBlock.LEVEL);
        if (level <= 1) {
            world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
        } else {
            world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, level - 1));
        }
        return true;
    }
}
