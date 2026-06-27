package com.kingdomcomecombat.platform;

import java.util.ServiceLoader;

public final class PlatformServices {
    private static final LoaderPlatform LOADER = ServiceLoader.load(LoaderPlatform.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                    "No Kingdom Come Combat loader platform service was found"
            ));

    private PlatformServices() {
    }

    public static LoaderPlatform loader() {
        return LOADER;
    }
}
