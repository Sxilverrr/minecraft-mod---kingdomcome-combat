# Kingdom Come Combat 1.6.1 build status

Successfully built release artifacts:

- Fabric 1.21
- Fabric 1.21.1
- Fabric 1.21.6
- Fabric 1.21.7-1.21.8 (one merged compatibility jar)
- Fabric 1.21.10
- Fabric 1.21.11
- NeoForge 1.21.1

Targets tested but not released:

- Fabric 1.21.2-1.21.5: the legacy source transformer does not cover the
  intermediate item, damage, attribute, recipe, and renderer API transitions.
- Fabric 1.21.9: the renderer and particle submission API adapter is incomplete.
- Fabric 26.2: the separate Java 25/Mojmap port is still a scaffold and cannot
  compile the shared Yarn-named sources directly.

All jars in this directory report mod version `1.6.1`. The merged Fabric jar
declares Minecraft `>=1.21.7 <=1.21.8`; every other jar declares one exact
Minecraft version.
