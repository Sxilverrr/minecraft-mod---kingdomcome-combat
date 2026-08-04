package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(NamespaceResourceManager.class)
public abstract class NamespaceResourceManagerWeaponModelMixin {
    private static final Set<String> KCC_WEAPONS = Set.of(
            "wooden_longsword", "stone_longsword", "copper_longsword", "golden_longsword",
            "iron_longsword", "diamond_longsword", "netherite_longsword",
            "copper_fighting_mace", "golden_fighting_mace", "iron_fighting_mace",
            "diamond_fighting_mace", "netherite_fighting_mace", "fighting_mace", "hand_cannon"
    );
    private static final Set<String> VANILLA_WEAPONS = Set.of(
            "wooden_sword", "stone_sword", "golden_sword", "iron_sword", "diamond_sword", "netherite_sword",
            "wooden_axe", "stone_axe", "golden_axe", "iron_axe", "diamond_axe", "netherite_axe",
            "wooden_pickaxe", "stone_pickaxe", "golden_pickaxe", "iron_pickaxe", "diamond_pickaxe", "netherite_pickaxe",
            "wooden_shovel", "stone_shovel", "golden_shovel", "iron_shovel", "diamond_shovel", "netherite_shovel",
            "wooden_hoe", "stone_hoe", "golden_hoe", "iron_hoe", "diamond_hoe", "netherite_hoe",
            "mace", "trident", "wooden_spear", "stone_spear", "copper_spear", "iron_spear",
            "golden_spear", "diamond_spear", "netherite_spear", "bow", "crossbow", "shield"
    );
    private static final Set<String> VANILLA_WEAPON_PARENTS = Set.of(
            "handheld", "handheld_rod"
    );

    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void kingdomcomecombat$keepBuiltInWeaponModel(
            Identifier id,
            CallbackInfoReturnable<Optional<Resource>> cir
    ) {
        if (!isLockedWeaponModel(id)) return;
        List<Resource> resources = ((NamespaceResourceManager) (Object) this).getAllResources(id);
        if (!resources.isEmpty()) cir.setReturnValue(Optional.of(resources.getFirst()));
    }

    private static boolean isLockedWeaponModel(Identifier id) {
        String path = id.getPath();
        String prefix;
        if (path.startsWith("models/item/") && path.endsWith(".json")) {
            prefix = "models/item/";
        } else if (path.startsWith("items/") && path.endsWith(".json")) {
            prefix = "items/";
        } else {
            return false;
        }
        String item = path.substring(prefix.length(), path.length() - ".json".length());
        return id.getNamespace().equals(KingdomComeCombat.MOD_ID)
                ? KCC_WEAPONS.contains(item)
                : id.getNamespace().equals("minecraft")
                && (VANILLA_WEAPONS.contains(item) || VANILLA_WEAPON_PARENTS.contains(item));
    }
}
