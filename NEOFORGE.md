# NeoForge support with minimal duplicated code

The first native NeoForge target is **Minecraft 1.21.1**. The module under
`neoforge/` shares gameplay sources and uses the NeoForge builds of Cloth
Config and Player Animation Library. Forgified Fabric API keeps the existing
event and networking code reusable without Sinytra Connector.

NeoForge's patched Minecraft jar cannot currently be remapped directly to Yarn:
its added `getInventory` and `renderHotbar` methods collide with Yarn target
names. The native module therefore uses Mojmap and generates its source tree
from the canonical Yarn sources during the build.

## Build

Build the native NeoForge jar with:

```sh
JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
  ./gradlew -p neoforge build
```

Use the same `JAVA_HOME` prefix for `runClient` and `runServer`. Java 25 is not
supported by the ASM version in NeoForge 1.21.1.

This automatically generates the 1.21.1 compatibility sources, migrates their
Yarn names to Mojmap, applies the small deterministic migration fixes, and
builds `neoforge/build/libs/kingdom_come_combat-neoforge-<version>.jar`.

The native instance requires Forgified Fabric API 0.116.7, Cloth Config NeoForge
15.0.140, and Player Animation Library NeoForge 1.1.4. GeckoLib NeoForge 4.8.2
is optional. It does not require Sinytra Connector.

### Connector fallback

Use a separate Gradle invocation so the version compatibility sources are
regenerated for 1.21.1:

```sh
./gradlew -Ptarget_mc=1.21.1 clean build
```

The distributable jar is `build/libs/kingdom_come_combat-<version>.jar`.
Despite running on NeoForge through Connector, it must retain `fabric.mod.json`;
Connector reads and transforms that metadata at launch.

## NeoForge 1.21.1 test instance

Install these in the instance's `mods` directory:

1. NeoForge 21.1.219 or newer 21.1.x build.
2. Forgified Fabric API 0.116.7+2.2.4+1.21.1.
3. Cloth Config NeoForge 15.0.140.
4. Player Animation Library NeoForge 1.1.4+mc.1.21.1.
5. The native Kingdom Come Combat 1.21.1 jar produced above.

Start with only that set. GeckoLib, FirstPerson, Modonomicon, and Guard
Villagers are optional integrations and should be introduced one at a time
after a world can be joined. In particular, do not use the bundled 1.21.7 or
1.21.8 library jars in the 1.21.1 instance.

Both client and server need Forgified Fabric API, Cloth Config, the animation
library, and Kingdom Come Combat.

## Acceptance check

Before calling a build usable, verify in this order:

1. Dedicated server reaches `Done` without a mixin or missing-class error.
2. Client joins a new world and receives the server config sync.
3. Directional attack, block, dodge, lock-on, stamina, and one combo work.
4. A hostile humanoid attacks and sends its warning indicator.
5. Rejoin the world and confirm injuries and learned skills persist.
6. Only then add optional integrations one by one.

## When to make a native port

A native NeoForge module becomes worthwhile only if Connector fails on a core
mixin/network path or a publishing platform requires a native NeoForge jar.
That second phase should keep gameplay code shared and replace only loader
boundaries (entrypoints, events, networking, registries, config paths, and
client registration). Converting the whole codebase to Mojmap first would make
the Fabric branch expensive to maintain and is deliberately not phase one.
