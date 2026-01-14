# Hytale Plugin Development Template

A simple Hytale plugin development template built with Gradle (Kotlin DSL) and Java 25.

## ✨ Features

- **VS Code Java Extension Pack Compatible** - Full IntelliSense, debugging, and code navigation support
- **Automatic Server Running** - Run your plugin with the Hytale server directly using a single command
- **Modern Build System** - Gradle with Kotlin DSL for type-safe, maintainable builds, shadowJar for easy distribution
- **Minimalist & Simple** - Clean project structure without unnecessary complexity
- **Java 25** - Built with the latest Java features and toolchain
- **Hot Reload Ready** - Quick iteration with automatic builds and server restarts

## Prerequisites

- **Java Development Kit (JDK) 25** or higher
- **Python 3** (for running the server)
- **Hytale Server JAR and Assets** - Place `HytaleServer.jar` and `Assets.zip` in the `libs/` folder (see [libs/README.md](./libs/README.md))

## Quick Start

1. **Get the server files** - Place `HytaleServer.jar` and `Assets.zip` in the `libs/` folder
2. **Build your plugin:**

   ```bash
   ./gradlew build
   ```

   The JAR will be created at `app/build/libs/`

3. **Run the server with your plugin:**
   ```bash
   ./run.py
   ```
   This automatically builds your plugin and starts the Hytale server.

## Commands

### Build Plugin

```bash
./gradlew build
```

Builds your plugin JAR to `app/build/libs/`

### Clean Build

```bash
./gradlew clean
```

Removes all build artifacts and the `run/` directory.

### Run Server

```bash
./run.py
```

Builds your plugin and starts the Hytale server with your plugin loaded.

## Recommended Project Structure

```
src/main/java/com/yourname/yourplugin/
├── Plugin.java          # Main plugin class
├── commands/            # Command handlers
├── listeners/           # Event listeners
├── services/            # Business logic
├── storage/             # Data persistence
├── config/              # Configuration
└── utils/               # Utility classes
```

## Customization

### Update Package Name

1. Rename `app/src/main/java/com/example/demo/` to your package path
2. Update the package declaration in `Plugin.java`
3. Update `Main` field in `app/src/main/resources/manifest.json`
4. Update `pluginGroup` in `gradle.properties`

### Update Plugin Metadata

Edit `app/src/main/resources/manifest.json` to set your plugin name, version, description, etc.

## Building for Production

1. Update version in `app/build.gradle.kts`:

   ```kotlin
   version = "1.0.0"
   ```

2. Build the JAR:

   ```bash
   ./gradlew build
   ```

3. Find your plugin at `app/build/libs/YourPluginName-1.0.0.jar`

## Troubleshooting

- **Build errors:** Ensure you have JDK 25 installed
- **Server won't start:** Check that `HytaleServer.jar` and `Assets.zip` are in `libs/` folder
- **Python not found:** Ensure Python 3 is installed and in your PATH
- **Java Runtime version mismatch:** Ensure you have JDK 25 installed and configured, run `> Java: Configure 
Java Runtime` to set to JDK 25.
