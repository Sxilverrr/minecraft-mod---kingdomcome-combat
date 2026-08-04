# Version compatibility

Select any Minecraft 1.21 release through one Gradle property:

```sh
./gradlew -Ptarget_mc=1.21.8 clean build
```

The root build contains Yarn and Fabric API coordinates for all twelve stable
1.21 releases. Coordinates alone do not guarantee that the current source
adapter covers every intermediate API transition. Each successful selected
build writes an exact Minecraft dependency into `fabric.mod.json`; the default
build advertises only the range verified to share the same source and bundled
dependencies.

## Current matrix

| Minecraft | Status | Known boundary |
| --- | --- | --- |
| 1.21-1.21.1 | Compiles with legacy adapter | Predates `ItemRenderState`, armed render states, and equipment assets |
| 1.21.2-1.21.5 | Adapter incomplete | Intermediate item, damage, attribute, recipe, and renderer APIs require a dedicated adapter |
| 1.21.6 | Compiles | Shared modern source group |
| 1.21.7 | Stable | Release target; shared source and bundled dependency set verified |
| 1.21.8 | Stable | Release target; shared source and bundled dependency set verified |
| 1.21.9 | Adapter incomplete | Renderer and particle submission API changed |
| 1.21.10-1.21.11 | Compiles with new-renderer adapter | Entity accessors and rendering APIs converted |
| 26.2 | Incomplete separate port | Unobfuscated names and Java 25; shared Yarn sources do not compile directly |

Compilation is only the first gate. Every published target also needs a client
launch test because Mixin targets and bundled animation/rendering dependencies
can fail at runtime even when Java compilation succeeds.
