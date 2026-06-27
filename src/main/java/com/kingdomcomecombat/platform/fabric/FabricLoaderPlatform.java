package com.kingdomcomecombat.platform.fabric;

import com.kingdomcomecombat.platform.LoaderPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class FabricLoaderPlatform implements LoaderPlatform {
    @Override
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
