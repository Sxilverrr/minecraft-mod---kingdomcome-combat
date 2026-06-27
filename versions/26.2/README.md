# Minecraft 26.2 port

This directory is an isolated build entry for the 26.2 port. It reuses the
current source tree while the code is split into common and version-specific
adapters.

Build from the repository root:

```sh
JAVA_HOME=/usr/local/opt/openjdk ./gradlew -p versions/26.2 compileJava compileClientJava
```

Current migration blockers:

1. Minecraft 26.2 is distributed unobfuscated; the Yarn-named imports and
   Mixin targets in the shared source must be migrated.
2. Java 25 is required. Homebrew OpenJDK 25.0.2 is installed at
   `/usr/local/opt/openjdk` on the current host.
3. Player Animation Library, FirstPerson, TRender, TRansition, and GeckoLib
   need 26.2-compatible artifacts before the client can run. Their bundled
   1.21.x jars cannot be loaded because their access wideners use the removed
   `intermediary` namespace.
4. `fabric.mod.json` must become version-specific before publishing so the
   1.21.7 and 26.2 dependency ranges do not overlap incorrectly.

The first diagnostic compile reached the Java compiler and reported the
expected package migration failures. The current source contains 950 Minecraft
imports across 174 files and 52 Mixin targets, so this is a source migration,
not a metadata-only version bump.
