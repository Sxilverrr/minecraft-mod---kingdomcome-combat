package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.combat.ActiveServerAttack;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.ServerCombatState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AiAttackCoordinator {
    private static final Map<UUID, AttackTurn> ATTACK_TURNS_BY_TARGET = new HashMap<>();

    private AiAttackCoordinator() {
    }

    public static void tick(long worldTime) {
        Iterator<Map.Entry<UUID, AttackTurn>> iterator = ATTACK_TURNS_BY_TARGET.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().busyUntilTick < worldTime) {
                iterator.remove();
            }
        }
    }

    public static boolean canStartAttack(MobEntity attacker, LivingEntity target) {
        if (attacker == null || target == null) {
            return false;
        }

        if (isOtherMobProtectedByPlayerSuction(attacker, target)) {
            return false;
        }

        if (ServerCombatState.getAttack(target.getUuid()) != null) {
            return false;
        }

        long worldTime = attacker.getWorld().getTime();
        AttackTurn turn = ATTACK_TURNS_BY_TARGET.get(target.getUuid());
        if (turn == null || turn.busyUntilTick < worldTime) {
            return true;
        }

        if (turn.attackerUuid.equals(attacker.getUuid())) {
            return true;
        }

        return false;
    }

    public static void recordAttackStart(MobEntity attacker, LivingEntity target, int rawAttackTicksAtNormalSpeed) {
        if (attacker == null || target == null) {
            return;
        }

        long busyUntil = attacker.getWorld().getTime() + Math.max(1, rawAttackTicksAtNormalSpeed);
        ATTACK_TURNS_BY_TARGET.put(target.getUuid(), new AttackTurn(attacker.getUuid(), busyUntil));
    }

    private static boolean isOtherMobProtectedByPlayerSuction(MobEntity attacker, LivingEntity target) {
        if (!(target instanceof PlayerEntity)) {
            return false;
        }

        ActiveServerAttack playerAttack = ServerCombatState.getAttack(target.getUuid());
        if (playerAttack == null || playerAttack.targetEntityId == attacker.getId()) {
            return false;
        }

        return isSuctionCombo(playerAttack) || isMasterCounterAttack(playerAttack);
    }

    private static boolean isSuctionCombo(ActiveServerAttack attack) {
        return attack.comboMove != null && attack.comboMove.suctionCombo();
    }

    private static boolean isMasterCounterAttack(ActiveServerAttack attack) {
        AttackMoveConfig moveConfig = attack.moveConfig;
        if (moveConfig == null) {
            return false;
        }

        return moveConfig.id().startsWith("master_counter")
                || moveConfig.animationName().startsWith("master_counter");
    }

    private record AttackTurn(UUID attackerUuid, long busyUntilTick) {
    }
}
