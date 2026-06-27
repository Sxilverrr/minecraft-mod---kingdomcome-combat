package com.kingdomcomecombat.item;

import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.PlayerComboProgress;
import com.kingdomcomecombat.network.OpenSkillBookPayload;
import com.kingdomcomecombat.passive.PassiveSkillConfig;
import com.kingdomcomecombat.passive.PassiveSkillConfigs;
import com.kingdomcomecombat.passive.PlayerPassiveSkillProgress;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class SkillBookItem extends WrittenBookItem {
    private static final String COMBO_KEY = "combo";
    private static final String COMBO_ID_KEY = "combo_id";
    private static final String BOOK_KEY = "book";
    private static final String BOOK_ID_KEY = "book_id";
    private static final String PASSIVE_KEY = "passive";
    private static final String PASSIVE_ID_KEY = "passive_id";
    private static final String BOOK_LANGUAGE_KEY = "book_language";

    public SkillBookItem(Settings settings) {
        super(settings);
    }

    public static void registerHeldLocalization() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                refreshHeldBook(player, Hand.MAIN_HAND);
                refreshHeldBook(player, Hand.OFF_HAND);
            }
        });
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            openBook(player, stack, hand);
        }

        return ActionResult.SUCCESS;
    }

    public static ItemStack createStack(String bookId, String comboId) {
        ItemStack stack = new ItemStack(ModItems.SKILL_BOOK);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (bookId != null && !bookId.isBlank()) {
                nbt.putString(BOOK_ID_KEY, bookId);
            }
            if (comboId != null && !comboId.isBlank()) {
                nbt.putString(COMBO_ID_KEY, comboId);
            }
        });
        applyBookContent(stack, SkillBookTexts.get(bookId));
        return stack;
    }

    public static ItemStack createPassiveStack(String bookId, String passiveId) {
        ItemStack stack = new ItemStack(ModItems.SKILL_BOOK);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (bookId != null && !bookId.isBlank()) {
                nbt.putString(BOOK_ID_KEY, bookId);
            }
            if (passiveId != null && !passiveId.isBlank()) {
                nbt.putString(PASSIVE_ID_KEY, passiveId);
            }
        });
        applyBookContent(stack, SkillBookTexts.get(bookId));
        return stack;
    }

    public static Optional<String> comboId(ItemStack stack) {
        if (!stack.isOf(ModItems.SKILL_BOOK)) {
            return Optional.empty();
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }

        NbtCompound nbt = customData.copyNbt();
        String comboId = firstString(nbt, COMBO_KEY, COMBO_ID_KEY);
        return comboId.isBlank() ? Optional.empty() : Optional.of(comboId);
    }

    public static Optional<String> bookId(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return comboId(stack);
        }

        NbtCompound nbt = customData.copyNbt();
        String bookId = firstString(nbt, BOOK_KEY, BOOK_ID_KEY);
        if (!bookId.isBlank()) {
            return Optional.of(bookId);
        }
        Optional<String> comboId = comboId(stack);
        return comboId.isPresent() ? comboId : passiveId(stack);
    }

    public static Optional<String> passiveId(ItemStack stack) {
        if (!stack.isOf(ModItems.SKILL_BOOK)) {
            return Optional.empty();
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }

        NbtCompound nbt = customData.copyNbt();
        String passiveId = firstString(nbt, PASSIVE_KEY, PASSIVE_ID_KEY);
        if (!passiveId.isBlank()) {
            return Optional.of(passiveId);
        }

        String bookId = firstString(nbt, BOOK_KEY, BOOK_ID_KEY);
        return !bookId.isBlank() && PassiveSkillConfigs.get(bookId) != null
                ? Optional.of(bookId)
                : Optional.empty();
    }

    private static void openBook(ServerPlayerEntity player, ItemStack stack, Hand hand) {
        String comboId = comboId(stack).orElse("");
        String passiveId = passiveId(stack).orElse("");
        String bookId = bookId(stack).orElse(comboId.isBlank() ? passiveId : comboId);
        SkillBookTexts.Entry text = SkillBookTexts.get(bookId);
        String language = player.getClientOptions().language();
        Optional<ComboMoveConfig> combo = ComboMoveConfigs.all().stream()
                .filter(candidate -> candidate.id().equals(comboId))
                .findFirst();
        PassiveSkillConfig passive = PassiveSkillConfigs.get(passiveId);

        applyBookContent(stack, text, language);
        ServerPlayNetworking.send(
                player,
                new OpenSkillBookPayload(
                        comboId,
                        passiveId,
                        text.title(language),
                        text.firstPage(language),
                        text.secondPage(language),
                        text.illustration(language),
                        combo.isPresent() && PlayerComboProgress.isUnlocked(player, combo.get())
                                || passive != null && PlayerPassiveSkillProgress.isUnlocked(player, passive.id())
                )
        );
        player.useBook(stack, hand);
    }

    public static void applyBookContent(ItemStack stack, SkillBookTexts.Entry text) {
        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, bookContent(text));
    }

    private static void applyBookContent(ItemStack stack, SkillBookTexts.Entry text, String language) {
        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, bookContent(text, language));
        stack.set(DataComponentTypes.ITEM_NAME, Text.literal(text.title(language)));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt ->
                nbt.putString(BOOK_LANGUAGE_KEY, normalizeLanguage(language))
        );
    }

    private static WrittenBookContentComponent bookContent(SkillBookTexts.Entry text) {
        return bookContent(text, "");
    }

    private static WrittenBookContentComponent bookContent(SkillBookTexts.Entry text, String language) {
        String title = text.title(language);
        String illustration = text.illustration(language);
        boolean chinese = normalizeLanguage(language).startsWith("zh");
        String illustrationLabel = chinese ? "插图" : "Illustration";
        String firstPage = title
                + "\n\n"
                + (illustration.isBlank() ? "" : "[" + illustrationLabel + ": " + illustration + "]\n\n")
                + text.firstPage(language);
        return new WrittenBookContentComponent(
                RawFilteredPair.of(title),
                text.author(),
                0,
                List.of(
                        RawFilteredPair.of(Text.literal(firstPage)),
                        RawFilteredPair.of(Text.literal(text.secondPage(language)))
                ),
                true
        );
    }

    private static void refreshHeldBook(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModItems.SKILL_BOOK)) {
            return;
        }
        String language = normalizeLanguage(player.getClientOptions().language());
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        String appliedLanguage = customData == null
                ? ""
                : normalizeLanguage(customData.copyNbt().getString(BOOK_LANGUAGE_KEY, ""));
        if (language.equals(appliedLanguage)) {
            return;
        }
        String id = bookId(stack).orElse("");
        if (!id.isBlank()) {
            applyBookContent(stack, SkillBookTexts.get(id), language);
        }
    }

    private static String normalizeLanguage(String language) {
        return language == null ? "" : language.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static boolean learn(ServerPlayerEntity player, String comboId) {
        if (comboId == null || comboId.isBlank()) {
            return false;
        }

        ComboMoveConfig combo = ComboMoveConfigs.all().stream()
                .filter(candidate -> candidate.id().equals(comboId))
                .findFirst()
                .orElse(null);
        if (combo == null) {
            player.sendMessage(Text.literal("技能书记录的招式不存在：" + comboId), false);
            return false;
        }

        boolean changed = PlayerComboProgress.unlock(player, comboId);
        player.sendMessage(
                Text.literal(changed
                        ? "习得招式：" + combo.displayName()
                        : "你已经掌握了：" + combo.displayName()),
                false
        );
        return changed;
    }

    public static boolean learnPassive(ServerPlayerEntity player, String passiveId) {
        if (passiveId == null || passiveId.isBlank()) {
            return false;
        }

        PassiveSkillConfig passive = PassiveSkillConfigs.get(passiveId);
        if (passive == null) {
            player.sendMessage(Text.literal("技能书记录的被动技能不存在：" + passiveId), false);
            return false;
        }
        if (passive.source() != PassiveSkillConfig.Source.BOOK) {
            String source = passive.source() == PassiveSkillConfig.Source.EXPERIENCE ? "消耗经验学习" : "通过成就领会";
            player.sendMessage(Text.literal("这个被动技能需要" + source + "：" + passive.name()), false);
            return false;
        }

        boolean changed = PlayerPassiveSkillProgress.unlock(player, passiveId);
        player.sendMessage(
                Text.literal(changed
                        ? "习得被动技能：" + passive.name()
                        : "你已经掌握了：" + passive.name()),
                false
        );
        return changed;
    }

    public static boolean learnFromHeldBook(ServerPlayerEntity player, String comboId, String passiveId) {
        if (!heldBookMatches(player, Hand.MAIN_HAND, comboId, passiveId)
                && !heldBookMatches(player, Hand.OFF_HAND, comboId, passiveId)) {
            player.sendMessage(Text.literal("手上没有对应的技能书"), false);
            return false;
        }

        if (passiveId != null && !passiveId.isBlank()) {
            return learnPassive(player, passiveId);
        }
        return learn(player, comboId);
    }

    public static boolean learnFromBookScreen(ServerPlayerEntity player, String comboId, String passiveId) {
        if (passiveId != null && !passiveId.isBlank()) {
            return learnPassive(player, passiveId);
        }
        return learn(player, comboId);
    }

    public static boolean learnFromStack(ServerPlayerEntity player, ItemStack stack) {
        Optional<String> comboId = comboId(stack);
        if (comboId.isPresent()) {
            return learn(player, comboId.get());
        }
        Optional<String> passiveId = passiveId(stack);
        return passiveId.isPresent() && learnPassive(player, passiveId.get());
    }

    private static boolean heldBookMatches(ServerPlayerEntity player, Hand hand, String comboId, String passiveId) {
        ItemStack stack = player.getStackInHand(hand);
        if (passiveId != null && !passiveId.isBlank()) {
            return passiveId(stack).map(passiveId::equals).orElse(false);
        }
        return comboId(stack).map(comboId::equals).orElse(false);
    }

    private static String firstString(NbtCompound nbt, String firstKey, String secondKey) {
        String first = nbt.getString(firstKey, "");
        if (!first.isBlank()) {
            return first;
        }

        return nbt.getString(secondKey, "");
    }
}
