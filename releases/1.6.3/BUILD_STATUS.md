# Kingdom Come Combat 1.6.3 build status

Successfully built release artifacts:

- Fabric 1.21
- Fabric 1.21.1
- Fabric 1.21.6
- Fabric 1.21.7-1.21.8 (one merged compatibility jar)
- Fabric 1.21.10
- Fabric 1.21.11
- NeoForge 1.21.1

Verification performed:

- Every Fabric target completed `clean build`.
- NeoForge 1.21.1 completed `clean build`.
- Fabric 1.21.1 reached the dedicated-server `Done` state.
- NeoForge 1.21.1 reached the dedicated-server `Done` state with Epic Knights,
  FirstPerson, and NotEnoughAnimations installed.
- All packaged metadata reports mod version `1.6.3` and the expected Minecraft
  dependency range.
- Source JSON files pass `jq` validation and `git diff --check` passes.

Known compatibility/parity notes:

- Fabric 1.21 and 1.21.1 retain villager skill-book trades but do not inject
  skill books into chest loot. Their Fabric Loot API predates the v3 post-drop
  callback used by the newer targets.
- Fabric 1.21.2-1.21.5 are not release targets: the compatibility transformer
  does not cover all intermediate item, damage, attribute, recipe, and renderer
  API transitions.
- Fabric 1.21.9 is not a release target: its renderer and particle submission
  compatibility adapter is incomplete.
- Fabric 26.2 remains an unreleased Java 25/Mojmap scaffold and cannot yet build
  the shared Yarn-named code or resolve all third-party libraries.

The merged Fabric jar declares Minecraft `>=1.21.7 <=1.21.8`; every other jar
declares one exact Minecraft version.
