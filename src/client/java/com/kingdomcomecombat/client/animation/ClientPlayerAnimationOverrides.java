package com.kingdomcomecombat.client.animation;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Map;

public final class ClientPlayerAnimationOverrides {
    private static final String PLAYER_ANIMATION_PATH = "player_animations";
    private static final String AGGREGATE_FILE = "player_animations/none.json";

    private ClientPlayerAnimationOverrides() {
    }

    @SuppressWarnings("unchecked")
    public static void applyStandaloneFileOverrides(ResourceManager manager) {
        Map<Identifier, Animation> playerAnimations;
        try {
            Field animationsField = PlayerAnimResources.class.getDeclaredField("ANIMATIONS");
            animationsField.setAccessible(true);
            playerAnimations = (Map<Identifier, Animation>) animationsField.get(null);
        } catch (ReflectiveOperationException e) {
            System.out.println("[KCC] Failed to access player animation table: " + e);
            return;
        }

        for (Map.Entry<Identifier, Resource> entry : manager.findResources(
                PLAYER_ANIMATION_PATH,
                ClientPlayerAnimationOverrides::isStandaloneAnimationFile
        ).entrySet()) {
            try (var inputStream = entry.getValue().getInputStream()) {
                Map<String, Animation> loadedAnimations =
                        UniversalAnimLoader.loadAnimations(inputStream);

                for (Map.Entry<String, Animation> animationEntry : loadedAnimations.entrySet()) {
                    Identifier animationId = Identifier.of(
                            entry.getKey().getNamespace(),
                            animationEntry.getKey()
                    );
                    playerAnimations.put(animationId, animationEntry.getValue());
                }
            } catch (Exception e) {
                System.out.println(
                        "[KCC] Failed to override standalone player animation "
                                + entry.getKey()
                                + ": "
                                + e
                );
            }
        }
    }

    private static boolean isStandaloneAnimationFile(Identifier id) {
        String path = id.getPath();
        return path.endsWith(".json") && !AGGREGATE_FILE.equals(path);
    }
}
