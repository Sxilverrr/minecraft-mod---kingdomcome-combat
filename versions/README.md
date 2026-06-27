# Version compatibility

Select any Minecraft 1.21 release through one Gradle property:

```sh
./gradlew -Ptarget_mc=1.21.8 clean build
```

The root build contains the Yarn and Fabric API coordinates for all twelve
stable 1.21 releases. Each selected build writes an exact Minecraft dependency
into `fabric.mod.json`; the default build advertises only the range verified to
share the same source and bundled dependencies.

## Current matrix

| Minecraft | Status | Known boundary |
| --- | --- | --- |
| 1.21-1.21.3 | Legacy renderer required | Predates `ItemRenderState`, armed render states, and equipment assets |
| 1.21.4 | Compiles with adapter | Legacy NBT/item/render naming; generic non-humanoid head tracking and ruin-chest skill books disabled |
| 1.21.5 | Compiles with adapter | Legacy NBT; generic non-humanoid head tracking and ruin-chest skill books disabled |
| 1.21.6 | Compiles | Shared source group, not marked stable for release |
| 1.21.7 | Stable | Release target; shared source and bundled dependency set verified |
| 1.21.8 | Stable | Release target; shared source and bundled dependency set verified |
| 1.21.9-1.21.11 | New renderer required | Server compiles after entity accessor conversion; client rendering submission API changed |
| 26.2 | Separate port | Unobfuscated names and Java 25 |

Compilation is only the first gate. Every published target also needs a client
launch test because Mixin targets and bundled animation/rendering dependencies
can fail at runtime even when Java compilation succeeds.
