package com.kingdomcomecombat.client.ui;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.combat.ClientComboUnlockState;
import com.kingdomcomecombat.client.passive.ClientPassiveSkillUnlockState;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.item.SkillBookTexts;
import com.kingdomcomecombat.network.LearnExperiencePassiveSkillPayload;
import com.kingdomcomecombat.passive.PassiveSkillConfig;
import com.kingdomcomecombat.passive.PassiveSkillConfigs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ComboKnowledgeScreen extends Screen {
    private static final Identifier CROSS_BASE = Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_base.png");
    private static final Identifier CROSS_LEFT = Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_left.png");
    private static final Identifier CROSS_RIGHT = Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_right.png");
    private static final Identifier CROSS_UP = Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_up.png");
    private static final Identifier CROSS_DOWN = Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/cross_down.png");
    private static final int ROW_HEIGHT = 92;
    private static final int PASSIVE_ROW_HEIGHT = 74;
    private static final int LEARNABLE_ROW_HEIGHT = 86;
    private static final long LEARN_HOLD_MS = 900L;
    private static final List<Tab> TABS = List.of(
            new Tab("short_swords", "screen.kingdom_come_combat.skills.short_swords"),
            new Tab("longswords", "screen.kingdom_come_combat.skills.longswords"),
            new Tab("heavy_weapons", "screen.kingdom_come_combat.skills.heavy_weapons"),
            new Tab("", "screen.kingdom_come_combat.skills.general"),
            new Tab("passive", "screen.kingdom_come_combat.skills.passive"),
            new Tab("learnable", "screen.kingdom_come_combat.skills.learnable")
    );

    private int tabIndex;
    private int scrollOffset;
    private String heldSkillId = "";
    private long holdStartedMs;
    private boolean holdSent;

    public ComboKnowledgeScreen() {
        super(Text.translatable("screen.kingdom_come_combat.skills.title"));
    }

    @Override
    protected void init() {
        int tabSpacing = 68;
        int tabsWidth = (TABS.size() - 1) * tabSpacing + 64;
        int x = (width - tabsWidth) / 2;
        for (int i = 0; i < TABS.size(); i++) {
            final int index = i;
            addDrawableChild(ButtonWidget.builder(Text.translatable(TABS.get(i).label()), button -> {
                        tabIndex = index;
                        scrollOffset = 0;
                        clearHold();
                    })
                    .dimensions(x + i * tabSpacing, 28, 64, 20)
                    .build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);

        int panelX = width / 2 - 210;
        int panelY = 56;
        int panelW = 420;
        int panelH = height - 76;
        Text experienceText = Text.translatable(
                "screen.kingdom_come_combat.skills.current_experience",
                currentExperience()
        );
        context.drawTextWithShadow(
                textRenderer,
                experienceText,
                panelX + panelW - textRenderer.getWidth(experienceText),
                13,
                0xFFE0B45E
        );
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC161616);
        context.drawBorder(panelX, panelY, panelW, panelH, 0xFF6B5A43);

        Tab tab = TABS.get(tabIndex);
        if (tab.passive()) {
            renderPassiveTab(context, panelX, panelY, panelW, panelH);
            return;
        }
        if (tab.learnable()) {
            renderLearnableTab(context, panelX, panelY, panelW, panelH);
            return;
        }

        List<ComboMoveConfig> combos = combosForTab(tab);
        if (combos.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.kingdom_come_combat.skills.no_combos"), width / 2, panelY + 28, 0xFFD8D0C4);
            return;
        }
        int contentTop = panelY + 8;
        int contentBottom = panelY + panelH - 8;
        int y = contentTop - scrollOffset;
        context.enableScissor(panelX + 1, contentTop, panelX + panelW - 1, contentBottom);
        for (ComboMoveConfig combo : combos) {
            if (y + 84 >= contentTop && y <= contentBottom) {
                renderCombo(context, combo, panelX + 12, y, panelW - 24);
            }
            y += ROW_HEIGHT;
        }
        context.disableScissor();
    }

    private void renderLearnableTab(DrawContext context, int panelX, int panelY, int panelW, int panelH) {
        List<PassiveSkillConfig> skills = learnableSkills();
        if (skills.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.kingdom_come_combat.skills.no_learnable"), width / 2, panelY + 28, 0xFFD8D0C4);
            return;
        }

        maybeCompleteHeldSkill();
        int contentTop = panelY + 8;
        int contentBottom = panelY + panelH - 8;
        int y = contentTop - scrollOffset;
        context.enableScissor(panelX + 1, contentTop, panelX + panelW - 1, contentBottom);
        for (PassiveSkillConfig skill : skills) {
            if (y + LEARNABLE_ROW_HEIGHT - 8 >= contentTop && y <= contentBottom) {
                renderLearnableSkill(context, skill, panelX + 12, y, panelW - 24);
            }
            y += LEARNABLE_ROW_HEIGHT;
        }
        context.disableScissor();
    }

    private void renderPassiveTab(DrawContext context, int panelX, int panelY, int panelW, int panelH) {
        List<PassiveSkillConfig> skills = passiveSkills();
        if (skills.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.kingdom_come_combat.skills.no_passives"), width / 2, panelY + 28, 0xFFD8D0C4);
            return;
        }

        int contentTop = panelY + 8;
        int contentBottom = panelY + panelH - 8;
        int y = contentTop - scrollOffset;
        context.enableScissor(panelX + 1, contentTop, panelX + panelW - 1, contentBottom);
        for (PassiveSkillConfig skill : skills) {
            if (y + 66 >= contentTop && y <= contentBottom) {
                renderPassiveSkill(context, skill, panelX + 12, y, panelW - 24);
            }
            y += PASSIVE_ROW_HEIGHT;
        }
        context.disableScissor();
    }

    private void renderCombo(DrawContext context, ComboMoveConfig combo, int x, int y, int width) {
        context.fill(x, y, x + width, y + 84, 0xAA24211D);
        renderEmptyIcon(context, x + 8, y + 8);
        int textX = x + 46;
        context.drawText(textRenderer, bold(comboName(combo)), textX, y + 8, 0xFFF2E4C8, false);
        renderSequence(context, combo.sequence(), textX, y + 24);
        drawWrapped(context, description(combo), textX, y + 44, width - 56, 0xFFCFC5B4, 3);
    }

    private void renderEmptyIcon(DrawContext context, int x, int y) {
        context.fill(x, y, x + 28, y + 28, 0xFF3B3328);
        context.drawBorder(x, y, 28, 28, 0xFF8B7554);
    }

    private void renderPassiveSkill(DrawContext context, PassiveSkillConfig skill, int x, int y, int width) {
        context.fill(x, y, x + width, y + 66, 0xAA24211D);
        renderPassiveIcon(context, skill, x + 8, y + 11);
        int textX = x + 46;
        context.drawText(textRenderer, bold(skillName(skill)), textX, y + 8, 0xFFF2E4C8, false);
        drawWrapped(context, passiveDescription(skill), textX, y + 24, width - 56, 0xFFCFC5B4, 3);
    }

    private void renderLearnableSkill(DrawContext context, PassiveSkillConfig skill, int x, int y, int width) {
        double progress = holdProgress(skill.id());
        int shake = progress > 0.0 && !holdSent
                ? (int) Math.round(Math.sin(System.currentTimeMillis() / 24.0) * 3.0 * progress)
                : 0;
        int rowX = x + shake;
        context.fill(rowX, y, rowX + width, y + 78, 0xAA24211D);
        renderPassiveIcon(context, skill, rowX + 8, y + 13);
        int textX = rowX + 46;
        context.drawText(textRenderer, bold(skillName(skill)), textX, y + 8, 0xFFF2E4C8, false);
        int costColor = hasEnoughExperience(skill) ? 0xFFBBD68F : 0xFFE28D78;
        context.drawText(textRenderer, Text.translatable("screen.kingdom_come_combat.skills.experience_cost", skill.experienceCost()), textX, y + 22, costColor, false);
        drawWrapped(context, passiveDescription(skill), textX, y + 36, width - 56, 0xFFCFC5B4, 2);
        context.fill(rowX + 8, y + 66, rowX + width - 8, y + 70, 0xFF3B3328);
        int progressW = (int) Math.round((width - 16) * progress);
        if (progressW > 0) {
            context.fill(rowX + 8, y + 66, rowX + 8 + progressW, y + 70, 0xFFE0B45E);
        }
    }

    private void renderPassiveIcon(DrawContext context, PassiveSkillConfig skill, int x, int y) {
        context.fill(x, y, x + 28, y + 28, 0xFF3B3328);
        context.drawBorder(x, y, 28, 28, 0xFF8B7554);
        Identifier icon = iconId(skill.icon());
        if (icon != null) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, 28, 28, 28, 28, 28, 28);
        }
    }

    private void renderSequence(DrawContext context, List<CombatDirection> sequence, int x, int y) {
        int step = 18;
        for (int i = 0; i < sequence.size(); i++) {
            int iconX = x + i * step;
            int iconY = y;
            drawCross(context, iconX, iconY, sequence.get(i));
        }
    }

    private void drawCross(DrawContext context, int x, int y, CombatDirection direction) {
        drawTexture(context, CROSS_BASE, x, y, 16, 16);
        drawTexture(context, switch (direction) {
            case LEFT -> CROSS_LEFT;
            case RIGHT -> CROSS_RIGHT;
            case UP -> CROSS_UP;
            case DOWN -> CROSS_DOWN;
        }, x, y, 16, 16);
    }

    private void drawTexture(DrawContext context, Identifier id, int x, int y, int w, int h) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, w, h, w, h, w, h);
    }

    private void drawWrapped(DrawContext context, String text, int x, int y, int width, int color, int maxLines) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(text), width);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            context.drawText(textRenderer, lines.get(i), x, y + i * 10, color, false);
        }
    }

    private String description(ComboMoveConfig combo) {
        SkillBookTexts.Entry text = SkillBookTexts.get(combo.id());
        String description = text.firstPage(clientLanguage());
        return description.isBlank() ? combo.displayName() : description;
    }

    private String comboName(ComboMoveConfig combo) {
        String title = SkillBookTexts.get(combo.id()).title(clientLanguage());
        return title.isBlank() ? combo.displayName() : title;
    }

    private Text bold(String text) {
        return Text.literal(text).setStyle(Style.EMPTY.withBold(true));
    }

    private String passiveDescription(PassiveSkillConfig skill) {
        String description = skill.description(clientLanguage());
        if (!description.isBlank()) {
            return description;
        }
        return SkillBookTexts.get(skill.id()).firstPage(clientLanguage());
    }

    private String skillName(PassiveSkillConfig skill) {
        return skill.name(clientLanguage());
    }

    private String clientLanguage() {
        return client == null ? "" : client.options.language;
    }

    private Identifier iconId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Identifier parsed = Identifier.tryParse(value);
        if (parsed != null && value.contains(":")) {
            return parsed;
        }
        String path = value.endsWith(".png") ? value.substring(0, value.length() - 4) : value;
        return Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/passive_skills/" + path + ".png");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelX = width / 2 - 210;
        int panelY = 56;
        int panelW = 420;
        int panelH = height - 76;
        if (mouseX < panelX || mouseX > panelX + panelW || mouseY < panelY || mouseY > panelY + panelH) {
            return false;
        }

        Tab tab = TABS.get(tabIndex);
        int contentHeight;
        if (tab.passive()) {
            contentHeight = passiveSkills().size() * PASSIVE_ROW_HEIGHT;
        } else if (tab.learnable()) {
            contentHeight = learnableSkills().size() * LEARNABLE_ROW_HEIGHT;
        } else {
            contentHeight = combosForTab(tab).size() * ROW_HEIGHT;
        }
        int maxScroll = Math.max(0, contentHeight - (panelH - 16));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.round(verticalAmount * 24.0)));
        clearHold();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && TABS.get(tabIndex).learnable()) {
            PassiveSkillConfig skill = learnableSkillAt(mouseX, mouseY);
            if (skill != null) {
                heldSkillId = skill.id();
                holdStartedMs = System.currentTimeMillis();
                holdSent = false;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && !heldSkillId.isBlank()) {
            clearHold();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private List<ComboMoveConfig> combosForTab(Tab tab) {
        List<ComboMoveConfig> combos = new ArrayList<>();
        for (ComboMoveConfig combo : ComboMoveConfigs.all()) {
            if (!ClientComboUnlockState.isUnlocked(combo)) {
                continue;
            }
            if (matchesTab(combo, tab)) {
                combos.add(combo);
            }
        }
        combos.sort(Comparator.comparing(ComboMoveConfig::displayName));
        return combos;
    }

    private boolean matchesTab(ComboMoveConfig combo, Tab tab) {
        String tag = combo.requiredWeaponTag();
        if (tab.weaponTag().isBlank()) {
            return tag == null || tag.isBlank();
        }
        String comboTag = normalizedTag(tag);
        String tabTag = normalizedTag(tab.weaponTag());
        if ("sword".equals(comboTag)) {
            return "short_sword".equals(tabTag) || "longsword".equals(tabTag);
        }
        return comboTag.equals(tabTag);
    }

    private List<PassiveSkillConfig> passiveSkills() {
        List<PassiveSkillConfig> skills = new ArrayList<>();
        for (PassiveSkillConfig skill : PassiveSkillConfigs.all()) {
            if (ClientPassiveSkillUnlockState.isUnlocked(skill.id())) {
                skills.add(skill);
            }
        }
        skills.sort(Comparator.comparing(skill -> skill.name(clientLanguage())));
        return skills;
    }

    private List<PassiveSkillConfig> learnableSkills() {
        List<PassiveSkillConfig> skills = new ArrayList<>();
        for (PassiveSkillConfig skill : PassiveSkillConfigs.all()) {
            if (skill.source() == PassiveSkillConfig.Source.EXPERIENCE
                    && !ClientPassiveSkillUnlockState.isUnlocked(skill.id())) {
                skills.add(skill);
            }
        }
        skills.sort(Comparator.comparing(skill -> skill.name(clientLanguage())));
        return skills;
    }

    private PassiveSkillConfig learnableSkillAt(double mouseX, double mouseY) {
        int panelX = width / 2 - 210;
        int panelY = 56;
        int panelW = 420;
        int panelH = height - 76;
        int contentTop = panelY + 8;
        int contentBottom = panelY + panelH - 8;
        if (mouseX < panelX + 12 || mouseX > panelX + panelW - 12 || mouseY < contentTop || mouseY > contentBottom) {
            return null;
        }
        int relativeY = (int) mouseY - contentTop + scrollOffset;
        int index = relativeY / LEARNABLE_ROW_HEIGHT;
        int rowY = relativeY % LEARNABLE_ROW_HEIGHT;
        List<PassiveSkillConfig> skills = learnableSkills();
        return index >= 0 && index < skills.size() && rowY <= 78 ? skills.get(index) : null;
    }

    private void maybeCompleteHeldSkill() {
        if (heldSkillId.isBlank() || holdSent || holdProgress(heldSkillId) < 1.0) {
            return;
        }
        holdSent = true;
        ClientPlayNetworking.send(new LearnExperiencePassiveSkillPayload(heldSkillId));
    }

    private double holdProgress(String skillId) {
        if (heldSkillId.isBlank() || !heldSkillId.equals(skillId)) {
            return 0.0;
        }
        return Math.min(1.0, (System.currentTimeMillis() - holdStartedMs) / (double) LEARN_HOLD_MS);
    }

    private boolean hasEnoughExperience(PassiveSkillConfig skill) {
        return client != null && client.player != null && client.player.totalExperience >= skill.experienceCost();
    }

    private int currentExperience() {
        return client == null || client.player == null ? 0 : client.player.totalExperience;
    }

    private void clearHold() {
        heldSkillId = "";
        holdStartedMs = 0L;
        holdSent = false;
    }

    private String normalizedTag(String tag) {
        String value = tag == null ? "" : tag.toLowerCase();
        int namespace = value.indexOf(':');
        if (namespace >= 0) {
            value = value.substring(namespace + 1);
        }
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        return value.replace("long_sword", "longsword")
                .replace("short_sword", "short_sword").replace("short_swords", "short_sword")
                .replace("longswords", "longsword")
                .replace("swords", "sword");
    }

    private record Tab(String weaponTag, String label) {
        boolean passive() {
            return "passive".equals(weaponTag);
        }

        boolean learnable() {
            return "learnable".equals(weaponTag);
        }
    }
}
