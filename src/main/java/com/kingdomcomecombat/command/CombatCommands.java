package com.kingdomcomecombat.command;

import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.PlayerComboProgress;
import com.kingdomcomecombat.item.SkillBookItem;
import com.kingdomcomecombat.passive.PlayerPassiveSkillProgress;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.stream.Collectors;

public final class CombatCommands {
    private static final SuggestionProvider<ServerCommandSource> COMBO_SUGGESTIONS =
            (context, builder) -> CommandSource.suggestMatching(
                    ComboMoveConfigs.all().stream().map(ComboMoveConfig::id),
                    builder
            );

    private CombatCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("kcc")
                        .then(CommandManager.literal("combo")
                                .then(CommandManager.literal("unlock")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("combo", StringArgumentType.word())
                                                        .suggests(COMBO_SUGGESTIONS)
                                                        .executes(context -> unlockCombo(
                                                                EntityArgumentType.getPlayer(context, "player"),
                                                                StringArgumentType.getString(context, "combo")
                                                        )))))
                                .then(CommandManager.literal("learn_from_book")
                                        .executes(context -> learnFromHeldBook(context.getSource().getPlayerOrThrow())))
                                .then(CommandManager.literal("list")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> listCombos(
                                                        EntityArgumentType.getPlayer(context, "player")
                                                ))))))
        );
    }

    private static int unlockCombo(ServerPlayerEntity player, String comboId) {
        boolean exists = ComboMoveConfigs.all().stream()
                .map(ComboMoveConfig::id)
                .anyMatch(comboId::equals);
        if (!exists) {
            player.sendMessage(Text.literal("未知连招：" + comboId), false);
            return 0;
        }

        boolean changed = PlayerComboProgress.unlock(player, comboId);
        player.sendMessage(
                Text.literal(changed ? "已解锁连招：" + comboId : "已经拥有连招：" + comboId),
                false
        );
        return changed ? 1 : 0;
    }

    private static int learnFromHeldBook(ServerPlayerEntity player) {
        ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
        if (SkillBookItem.comboId(stack).isEmpty() && SkillBookItem.passiveId(stack).isEmpty()) {
            stack = player.getStackInHand(Hand.OFF_HAND);
        }

        String passiveId = SkillBookItem.passiveId(stack).orElse("");
        if (!passiveId.isBlank()) {
            return SkillBookItem.learnPassive(player, passiveId) ? 1 : 0;
        }

        String comboId = SkillBookItem.comboId(stack).orElse("");
        if (comboId.isBlank()) {
            player.sendMessage(Text.literal("手上没有可学习的技能书"), false);
            return 0;
        }

        ComboMoveConfig combo = ComboMoveConfigs.all().stream()
                .filter(candidate -> candidate.id().equals(comboId))
                .findFirst()
                .orElse(null);
        if (combo == null) {
            player.sendMessage(Text.literal("技能书记录的招式不存在：" + comboId), false);
            return 0;
        }

        return SkillBookItem.learn(player, comboId) ? 1 : 0;
    }

    private static int listCombos(ServerPlayerEntity player) {
        String combos = PlayerComboProgress.unlocked(player.getUuid()).stream()
                .sorted()
                .collect(Collectors.joining(", "));
        String passiveSkills = PlayerPassiveSkillProgress.unlocked(player.getUuid()).stream()
                .sorted()
                .collect(Collectors.joining(", "));
        player.sendMessage(Text.literal("玩家已解锁连招：" + combos), false);
        player.sendMessage(Text.literal("玩家已解锁被动：" + passiveSkills), false);
        return 1;
    }
}
