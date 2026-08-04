package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.game.ModGameRules;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds KCC's per-world hardcore option to the vanilla Game tab. */
@Mixin(targets = "net.minecraft.client.gui.screen.world.CreateWorldScreen$GameTab")
public abstract class CreateWorldGameTabMixin extends GridScreenTab {
    protected CreateWorldGameTabMixin() {
        super(Text.empty());
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void kingdomcomecombat$addSuperHardcoreToggle(
            CreateWorldScreen screen,
            CallbackInfo ci
    ) {
        boolean enabled = ModGameRules.hardcoreMode(screen.getWorldCreator().getGameRules());
        CyclingButtonWidget<Boolean> button = CyclingButtonWidget.onOffBuilder(enabled)
                .tooltip(value -> Tooltip.of(Text.translatable(
                        "createWorld.kingdom_come_combat.super_hardcore.tooltip"
                )))
                .build(
                        0,
                        0,
                        210,
                        20,
                        Text.translatable("createWorld.kingdom_come_combat.super_hardcore")
                                .formatted(Formatting.RED),
                        (widget, value) -> ModGameRules.setHardcoreMode(
                                screen.getWorldCreator().getGameRules(),
                                value
                        )
                );
        // Row 4 is also used by common world-creation extensions (including
        // transport mods), causing both widgets to occupy the same bounds.
        this.grid.add(button, 5, 0);
    }
}
