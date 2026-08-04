package com.kingdomcomecombat.client.ui;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.client.hardship.ClientHardshipState;
import com.kingdomcomecombat.hardship.HardshipConfig;
import com.kingdomcomecombat.network.SubmitHardshipSelectionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class HardshipSelectionScreen extends Screen {
    private static final int ICON_SIZE = 32;
    private static final int SHEET_COLUMNS = 20;
    private static final Identifier FALLBACK_ICON_SHEET = Identifier.of(
            KingdomComeCombat.MOD_ID,
            "textures/gui/hardships/debuff_sheet.png"
    );
    private final List<HardshipConfig> hardships;
    private final int minimum;
    private final Set<String> selected = new LinkedHashSet<>();
    private final List<IconEntry> icons = new ArrayList<>();
    private ButtonWidget beginButton;

    public HardshipSelectionScreen(int minimum) {
        super(Text.translatable("screen.kingdom_come_combat.hardships.title"));
        this.hardships = ClientHardshipState.definitions().stream().limit(20).toList();
        this.minimum = Math.max(1, minimum);
    }

    @Override
    protected void init() {
        icons.clear();
        int columns = 10;
        int gap = Math.max(2, Math.min(10, (width - columns * ICON_SIZE - 20) / (columns - 1)));
        int totalWidth = columns * ICON_SIZE + (columns - 1) * gap;
        int startX = (width - totalWidth) / 2;
        int startY = Math.max(74, height / 2 - 64);
        for (int i = 0; i < hardships.size(); i++) {
            HardshipConfig hardship = hardships.get(i);
            int x = startX + (i % columns) * (ICON_SIZE + gap);
            int y = startY + (i / columns) * (ICON_SIZE + 16);
            ButtonWidget button = addDrawableChild(ButtonWidget.builder(Text.empty(), ignored -> toggle(hardship.id()))
                    .dimensions(x, y, ICON_SIZE, ICON_SIZE).build());
            icons.add(new IconEntry(i, hardship, button));
        }
        beginButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.kingdom_come_combat.hardships.begin"),
                ignored -> submit()
        ).dimensions(width / 2 - 90, height - 32, 180, 20).build());
        updateBeginButton();
    }

    private void toggle(String id) {
        if (!selected.add(id)) selected.remove(id);
        updateBeginButton();
    }

    private void updateBeginButton() {
        if (beginButton != null) {
            beginButton.active = selected.size() >= minimum;
            beginButton.setMessage(Text.translatable(
                    "screen.kingdom_come_combat.hardships.begin_count", selected.size(), minimum));
        }
    }

    private void submit() {
        if (selected.size() < minimum) return;
        beginButton.active = false;
        ClientPlayNetworking.send(new SubmitHardshipSelectionPayload(List.copyOf(selected)));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0xFF000000);
        super.render(context, mouseX, mouseY, deltaTicks);

        // Screen.render draws the widget layer. Draw all explanatory text after it so
        // neither a widget backdrop nor a compatibility screen effect can soften it.
        drawCenteredText(context, title, 18, 0xFFFF5555);
        drawCenteredWrapped(context, Text.translatable("screen.kingdom_come_combat.hardships.description"), 38, Math.min(620, width - 32), 0xFFC9C9C9);
        IconEntry hovered = null;
        for (IconEntry entry : icons) {
            int x = entry.button.getX();
            int y = entry.button.getY();
            int border = selected.contains(entry.hardship.id()) ? 0xFFE0B45E : 0xFF555555;
            context.drawBorder(x - 1, y - 1, ICON_SIZE + 2, ICON_SIZE + 2, border);
            Identifier icon = iconId(entry.hardship.icon());
            if (icon != null) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0,
                        ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            } else if (entry.index < SHEET_COLUMNS) {
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        FALLBACK_ICON_SHEET,
                        x,
                        y,
                        entry.index * ICON_SIZE,
                        0,
                        ICON_SIZE,
                        ICON_SIZE,
                        ICON_SIZE,
                        ICON_SIZE,
                        SHEET_COLUMNS * ICON_SIZE,
                        ICON_SIZE
                );
            }
            if (entry.button.isHovered()) hovered = entry;
        }
        if (hovered != null) renderDetails(context, hovered.hardship);
    }

    private void renderDetails(DrawContext context, HardshipConfig hardship) {
        String language = client == null ? "" : client.options.language;
        String heading = hardship.title(language).isBlank() ? hardship.id() : hardship.title(language);
        int y = height - 84;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(heading), width / 2, y, 0xFFE0B45E);
        drawCenteredWrapped(context, Text.literal(hardship.description(language)), y + 14,
                Math.min(620, width - 32), 0xFFD0D0D0);
    }

    private void drawCenteredWrapped(DrawContext context, Text text, int y, int lineWidth, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(text, lineWidth);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            OrderedText line = lines.get(i);
            context.drawText(textRenderer, line, (width - textRenderer.getWidth(line)) / 2, y + i * 10, color, false);
        }
    }

    private void drawCenteredText(DrawContext context, Text text, int y, int color) {
        context.drawText(textRenderer, text, (width - textRenderer.getWidth(text)) / 2, y, color, false);
    }

    private Identifier iconId(String value) {
        if (value == null || value.isBlank()) return null;
        Identifier parsed = Identifier.tryParse(value);
        if (parsed != null && value.contains(":")) return parsed;
        String path = value.endsWith(".png") ? value.substring(0, value.length() - 4) : value;
        return Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/hardships/" + path + ".png");
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void close() {}
    @Override public boolean shouldPause() { return true; }

    private record IconEntry(int index, HardshipConfig hardship, ButtonWidget button) {}
}
