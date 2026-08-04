package com.kingdomcomecombat.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.item.ItemStack;

public class ServerComboState {
    private static final int REQUIRED_CHAIN_WINDOW_TICKS = 2;
    private static final Map<UUID, Deque<FinishedAttack>> HISTORY = new ConcurrentHashMap<>();

    private ServerComboState() {
    }

    public static Optional<ComboMoveConfig> selectComboForStart(
            UUID attackerUuid,
            CombatDirection direction,
            long startWorldTick,
            ItemStack weapon
    ) {
        return selectComboForStart(attackerUuid, direction, startWorldTick, weapon, ignored -> true);
    }

    public static Optional<ComboMoveConfig> selectComboForStart(
            UUID attackerUuid,
            CombatDirection direction,
            long startWorldTick,
            ItemStack weapon,
            Predicate<ComboMoveConfig> validator
    ) {
        return selectComboForStart(attackerUuid, direction, startWorldTick, weapon, false, validator);
    }

    public static Optional<ComboMoveConfig> selectComboForStart(
            UUID attackerUuid,
            CombatDirection direction,
            long startWorldTick,
            ItemStack weapon,
            boolean longswordAsShortSword,
            Predicate<ComboMoveConfig> validator
    ) {
        Deque<FinishedAttack> history = HISTORY.get(attackerUuid);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }

        List<FinishedAttack> attacks = new ArrayList<>(history);
        List<FinishedAttack> chainedAttacks = new ArrayList<>();
        List<CombatDirection> chainedDirections = new ArrayList<>();
        long nextStartTick = startWorldTick;

        for (int i = attacks.size() - 1; i >= 0; i--) {
            FinishedAttack attack = attacks.get(i);
            if (nextStartTick - attack.finishWorldTick() > REQUIRED_CHAIN_WINDOW_TICKS) {
                break;
            }

            chainedAttacks.add(0, attack);
            chainedDirections.add(0, attack.direction());
            nextStartTick = attack.startWorldTick();
        }

        chainedDirections.add(direction);
        return ComboMoveConfigs.findMatchingTail(
                chainedDirections,
                weapon,
                longswordAsShortSword,
                combo -> validator.test(combo) && canTriggerCombo(chainedAttacks, combo)
        ).map(combo -> {
            history.clear();
            return combo;
        });
    }

    public static void clear(UUID attackerUuid) {
        HISTORY.remove(attackerUuid);
    }

    public static void recordFinishedAttack(
            UUID attackerUuid,
            ActiveServerAttack attack,
            long finishWorldTick
    ) {
        if (attack.comboMove != null) {
            clear(attackerUuid);
            return;
        }

        Deque<FinishedAttack> history = HISTORY.computeIfAbsent(
                attackerUuid,
                ignored -> new ArrayDeque<>()
        );
        if (attack.perfectBlocked) {
            history.clear();
        }
        history.addLast(new FinishedAttack(
                attack.direction,
                attack.startWorldTick,
                finishWorldTick,
                attack.connectedOrNormalBlocked,
                attack.perfectBlocked
        ));

        int maxHistory = Math.max(2, ComboMoveConfigs.maxSequenceLength() - 1);
        while (history.size() > maxHistory) {
            history.removeFirst();
        }
    }

    private static boolean canTriggerCombo(
            List<FinishedAttack> chainedAttacks,
            ComboMoveConfig combo
    ) {
        int previousCount = combo.sequence().size() - 1;
        if (previousCount <= 0 || chainedAttacks.size() < previousCount) {
            return false;
        }

        List<FinishedAttack> matched = chainedAttacks.subList(
                chainedAttacks.size() - previousCount,
                chainedAttacks.size()
        );

        if (matched.getFirst().perfectBlocked()) {
            return false;
        }

        return matched.getLast().canFeedCombo();
    }

    private record FinishedAttack(
            CombatDirection direction,
            long startWorldTick,
            long finishWorldTick,
            boolean connectedOrNormalBlocked,
        boolean perfectBlocked
    ) {
        boolean canFeedCombo() {
            return connectedOrNormalBlocked && !perfectBlocked;
        }
    }
}
