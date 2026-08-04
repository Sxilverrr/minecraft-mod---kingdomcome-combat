# Kingdom Come Combat 1.7.1 build status

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
- Every release JAR passed `unzip -t`.
- All Fabric metadata reports mod version `1.7.1` and the expected Minecraft dependency.
- NeoForge metadata reports mod version `1.7.1` and Minecraft `1.21.1`.
- SHA-256 checksums are recorded in `SHA256SUMS`.

Not release targets:

- Fabric 1.21.2-1.21.5: intermediate compatibility adapter is incomplete.
- Fabric 1.21.9: renderer and particle compatibility adapter is incomplete.
- Fabric 26.2: separate Java 25 port remains an incomplete scaffold.
