package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.util.Identifier;

public class CombatAnimationResource {
    private static final String PLAYER_ANIMATION_DIRECTORY = "player_animations/";
    private static final String JSON_EXTENSION = ".json";

    private CombatAnimationResource() {
    }

    public static Resolved resolve(String animationName) {
        String name = animationName == null ? "" : animationName.trim();
        if (name.isEmpty()) {
            return new Resolved(
                    KingdomComeCombat.MOD_ID,
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        if (name.indexOf(':') >= 0) {
            Identifier id = Identifier.tryParse(name);
            if (id != null) {
                return resolve(id.getNamespace(), id.getPath());
            }
        }

        return resolve(KingdomComeCombat.MOD_ID, name);
    }

    public static Identifier playerAnimationId(String animationName) {
        Resolved resolved = resolve(animationName);
        return Identifier.of(resolved.namespace(), resolved.animationKey());
    }

    private static Resolved resolve(String namespace, String animationName) {
        String fileName = CombatAnimationNames.fileName(animationName);
        return new Resolved(
                namespace,
                fileName,
                CombatAnimationNames.animationKey(animationName),
                CombatAnimationNames.fallbackAnimationKey(animationName),
                animationName,
                PLAYER_ANIMATION_DIRECTORY + fileName + JSON_EXTENSION
        );
    }

    public record Resolved(
            String namespace,
            String fileName,
            String animationKey,
            String fallbackAnimationKey,
            String fileAnimationKey,
            String resourcePath
    ) {
        public Identifier resourceId() {
            return Identifier.of(namespace, resourcePath);
        }

        public String classpathResourcePath() {
            return "assets/" + namespace + "/" + resourcePath;
        }
    }
}
