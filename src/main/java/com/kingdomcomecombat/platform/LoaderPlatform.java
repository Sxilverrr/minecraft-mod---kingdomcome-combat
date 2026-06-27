package com.kingdomcomecombat.platform;

import java.nio.file.Path;

/**
 * Small loader boundary shared by gameplay code.
 *
 * <p>Keep Fabric/NeoForge APIs out of consumers of this interface. A future
 * NeoForge source set only needs to provide its own implementation.</p>
 */
public interface LoaderPlatform {
    Path configDirectory();

    boolean isModLoaded(String modId);
}
