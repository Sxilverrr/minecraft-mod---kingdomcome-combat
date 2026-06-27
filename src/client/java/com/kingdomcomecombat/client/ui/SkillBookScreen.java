package com.kingdomcomecombat.client.ui;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.network.LearnSkillBookPayload;
import com.kingdomcomecombat.network.OpenSkillBookPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class SkillBookScreen extends Screen {
    private static final int BOOK_WIDTH = 320;
    private static final int BOOK_HEIGHT = 190;
    private static final int PAGE_WIDTH = 132;
    private static final int PAGE_HEIGHT = 150;

    private final OpenSkillBookPayload payload;
    private int page;
    private boolean learnSent;
    private ButtonWidget previousButton;
    private ButtonWidget nextButton;

    public SkillBookScreen(OpenSkillBookPayload payload) {
        super(Text.literal(payload.title()));
        this.payload = payload;
        this.learnSent = payload.learned();
    }

    @Override
    protected void init() {
        int y = height / 2 + BOOK_HEIGHT / 2 + 8;
        previousButton = addDrawableChild(ButtonWidget.builder(
                Text.literal("<"),
                button -> setPage(0)
        ).dimensions(width / 2 - 52, y, 42, 20).build());
        nextButton = addDrawableChild(ButtonWidget.builder(
                Text.literal(">"),
                button -> setPage(1)
        ).dimensions(width / 2 + 10, y, 42, 20).build());
        updateButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        int bookX = (width - BOOK_WIDTH) / 2;
        int bookY = (height - BOOK_HEIGHT) / 2;
        int leftX = bookX + 22;
        int rightX = bookX + 170;
        int pageY = bookY + 20;

        context.fill(bookX, bookY, bookX + BOOK_WIDTH, bookY + BOOK_HEIGHT, 0xFFE7D8B0);
        context.fill(bookX + BOOK_WIDTH / 2 - 2, bookY + 8, bookX + BOOK_WIDTH / 2 + 2, bookY + BOOK_HEIGHT - 8, 0xFFB3915B);
        context.fill(leftX - 8, pageY - 8, leftX + PAGE_WIDTH + 8, pageY + PAGE_HEIGHT + 8, 0xFFF5E8C8);
        context.fill(rightX - 8, pageY - 8, rightX + PAGE_WIDTH + 8, pageY + PAGE_HEIGHT + 8, 0xFFF5E8C8);

        if (page == 0) {
            renderIllustration(context, leftX, pageY);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(payload.title()), rightX + PAGE_WIDTH / 2, pageY + 10, 0xFF5B3515);
            drawWrapped(context, payload.firstPage(), rightX, pageY + 36, PAGE_WIDTH, 0xFF3A2A18);
        } else {
            drawWrapped(context, payload.secondPage(), leftX, pageY + 16, PAGE_WIDTH * 2 + 16, 0xFF3A2A18);
            String statusKey = payload.learned() || learnSent
                    ? "screen.kingdom_come_combat.skill_book.learned"
                    : "screen.kingdom_come_combat.skill_book.learning";
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable(statusKey), bookX + BOOK_WIDTH / 2, pageY + PAGE_HEIGHT - 16, 0xFF5B3515);
        }
    }

    private void renderIllustration(DrawContext context, int x, int y) {
        Identifier illustration = illustrationId(payload.illustration());
        context.fill(x, y, x + PAGE_WIDTH, y + PAGE_HEIGHT, 0xFFD9C291);
        if (illustration == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(payload.title()), x + PAGE_WIDTH / 2, y + PAGE_HEIGHT / 2 - 5, 0xFF5B3515);
            return;
        }

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                illustration,
                x,
                y,
                0.0F,
                0.0F,
                PAGE_WIDTH,
                PAGE_HEIGHT,
                PAGE_WIDTH,
                PAGE_HEIGHT,
                PAGE_WIDTH,
                PAGE_HEIGHT
        );
    }

    private void drawWrapped(DrawContext context, String text, int x, int y, int width, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(text), width);
        int lineY = y;
        for (OrderedText line : lines) {
            context.drawText(textRenderer, line, x, lineY, color, false);
            lineY += 10;
        }
    }

    private Identifier illustrationId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Identifier parsed = Identifier.tryParse(value);
        if (parsed != null && value.contains(":")) {
            return parsed;
        }

        String path = value.endsWith(".png") ? value.substring(0, value.length() - 4) : value;
        return Identifier.of(KingdomComeCombat.MOD_ID, "textures/gui/skill_books/" + path + ".png");
    }

    private void setPage(int page) {
        this.page = Math.max(0, Math.min(1, page));
        if (this.page == 1 && !learnSent
                && (!payload.comboId().isBlank() || !payload.passiveId().isBlank())) {
            learnSent = true;
            ClientPlayNetworking.send(new LearnSkillBookPayload(payload.comboId(), payload.passiveId()));
        }
        updateButtons();
    }

    private void updateButtons() {
        if (previousButton != null) {
            previousButton.active = page > 0;
        }
        if (nextButton != null) {
            nextButton.active = page < 1;
        }
    }
}
