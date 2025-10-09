# Repository Guidelines

## Project Structure & Module Organization
DesktopNotify-KT is a multiplatform Gradle build with three active modules. `knotify` hosts the core API and native bridges in `knotify/src/commonMain/kotlin` and `knotify/src/jvmMain/kotlin`, with shared assets in `knotify/src/jvmMain/resources`. `knotify-compose` extends the core module to render Compose UI into notifications. The sample app lives in `demo/composeApp` and is the right place for manual experiments. Native helper sources and build scripts sit under `linuxlib`, `maclib`, and `winlib`, while marketing assets stay in `assets/`.

## Build, Test, and Development Commands
- `./gradlew build` compiles every module and runs available unit tests.
- `./gradlew :knotify:buildNativeLibraries` rebuilds the platform binaries; invoke on the host OS before publishing.
- `./gradlew :knotify:check` focuses checks on the core library when iterating locally.
- `./gradlew :demo:composeApp:run` launches the demo client for interactive verification.
- `./gradlew publishToMavenLocal` installs both artifacts for downstream integration testing.

## Coding Style & Naming Conventions
Follow the official Kotlin code style (4-space indents, trailing commas on multiline lists, explicit visibility on public APIs). Public classes, DSL builders, and Compose components use PascalCase; functions, properties, and lambda receivers use camelCase; callback lambdas start with `on`. Keep DSL functions fluent and side-effect free, prefer extension functions over utility singletons, and isolate platform-specific code inside the JVM source set. Run Android Studio or IntelliJ “Reformat Code” using Kotlin defaults before committing.

## Testing Guidelines
Place multiplatform tests in `knotify/src/commonTest/kotlin`; JVM-only scenarios can use `knotify/src/jvmTest/kotlin`. Use `kotlin.test` assertions and the coroutine test dispatcher from `kotlinx.coroutines.test`. Name tests after the behavior under validation (`functionName_condition_expectedResult`). Execute `./gradlew :knotify:check` ahead of every pull request and add regression coverage whenever fixing bugs or changing native bindings.

## Commit & Pull Request Guidelines
Keep commits small, focused, and written in the imperative mood (e.g., `Add Windows toast scheduler guard`). Prefix with the module when it clarifies scope (`knotify-compose: ...`). Pull requests should summarize intent, list platform verification (Windows/macOS/Linux), and link related issues. Capture screenshots or logs when altering notification visuals or interaction flows, and update README/demo snippets when public APIs change.

## Native Library Notes
Platform bridges are produced by the scripts in `linuxlib/build.sh`, `maclib/build.sh`, and `winlib/WinToastLibC/build.bat`. Run the matching script on its native host so the resulting binaries align with toolchain expectations. When changing headers or C++ sources, rebuild, verify loading through the demo app on each platform, and bump embedded library versions if distribution artifacts depend on the output.
