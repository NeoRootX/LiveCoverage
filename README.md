# Live Coverage Plugin

**Languages:** [English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

An IntelliJ IDEA plugin for real-time code coverage visualization using JaCoCo.

## Features

### Functionality
- **Real-time Coverage Visualization**: Automatically highlights executed code as your application runs
- **Cumulative Coverage Mode**: Continuously highlights all executed code. Use "Clear Coverage" to reset and start fresh
- **Integrated JaCoCo Agent**: The agent JAR is bundled with the plugin - no need to download separately
- **Tool Window**: Displays coverage statistics and method-level information
- **Manual Controls**: Refresh, clear (reset), and pause/resume polling actions
- **Multi-module Support**: Add multiple `Source output path` + `Classes output path` pairs

### Solves Real Business Problems
- Complex and hard-to-maintain business code in legacy systems
- Impact scope analysis for incremental development and requirement changes
- Test-data preparation and bug localization before regression testing
- Quickly answer: "Which business code paths did this request actually execute?"

### Core Value
- **Request-to-code path visibility with lower understanding cost**: Quickly locate executed code blocks
- **Higher regression-testing efficiency**: Analyze code and requirement impact using real execution paths, then prepare test data
- **Business bug localization**: Understand legacy behavior faster and pinpoint failing business code fragments

## Install

Install from [JetBrains Marketplace](https://plugins.jetbrains.com) (search for **Live Coverage**). A 30-day trial and monthly subscription are available.

For local development, see [Building](#building) below.

## Setup

1. **Configure Plugin Settings**:
   - Go to `Settings` -> `Tools` -> `Live Coverage`
   - Set TCP Address (default: 127.0.0.1)
   - Set TCP Port (default: 6300)
   - Add one or more path pairs:
     - `Source output path` (absolute path to Java source root)
     - `Classes output path` (absolute path to compiled `.class` files)

2. **Run Your Application**:
   - **Automatic**: JVM arguments are automatically added to run configurations
   - The plugin automatically injects JaCoCo JVM argument when you run/debug Java applications
   - No manual setup needed

## Usage

### Automatic Mode
The plugin automatically starts polling when you open a project. Coverage accumulates in real-time as your code executes.

### Manual Controls
Access via `Tools` -> `Live Coverage`:
- **Refresh Coverage**: Manually trigger a coverage update
- **Clear Coverage**: Clear all highlights and reset JaCoCo agent - use this to start fresh
- **Pause/Resume Coverage**: Start/stop automatic polling

### Tool Window
Open the "Request Coverage" tool window (bottom panel) to see:
- Coverage statistics
- Method-level execution information
- Connection status
- `Clear Coverage` and `Pause/Resume Coverage` buttons

### Key Features
- ✅ **Integrated Agent**: JaCoCo agent JAR is bundled - no manual download needed
- ✅ **Auto JVM Arguments**: Automatically adds JVM arguments to run configurations - zero manual setup
- ✅ **Cumulative Mode**: Automatically accumulates coverage over time
- ✅ **Manual Reset**: Clear coverage anytime to start fresh
- ✅ **Real-time Updates**: See code execution as it happens
- ✅ **Production-oriented**: Thread-safe, resource-managed architecture

## Building

```bash
./gradlew buildPlugin
```

The plugin will be built to `build/distributions/`.

## Development

### Project Structure
- **Source Code**: `src/main/java/`
- **Resources**: `src/main/resources/`
- **Build Config**: `build.gradle.kts`

### Code Style
- Follow IntelliJ Platform coding conventions
- Use Java 21
- All services are thread-safe
- Proper null-safety with `@NotNull`/`@Nullable` annotations

## Troubleshooting

### No Coverage Highlighted
1. Verify JaCoCo agent is running and accessible
2. Check TCP address and port in settings
3. Ensure classes output path is correct
4. Verify source output path matches your source structure

### Connection Errors
- Check that the JaCoCo agent is started with correct TCP settings
- Verify firewall/network settings
- Check logs in IDE (Help -> Show Log in Finder/Explorer)

## License

Copyright (c) 2026 Showen. End-user terms: [EULA.md](EULA.md).

## Acknowledgments

Built on:
- IntelliJ Platform SDK
- JaCoCo Code Coverage Library
