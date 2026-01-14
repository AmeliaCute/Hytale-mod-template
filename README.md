# Hytale Plugin Development Template

A modern, minimalist Hytale plugin development template with automatic server integration, built with Gradle (Kotlin DSL) and Java 25.

## ✨ Features

- **VS Code Java Extension Pack Compatible** - Full IntelliSense, debugging, and code navigation support
- **Automatic Server Running** - Run your plugin with the Hytale server directly using a single command
- **Modern Build System** - Gradle with Kotlin DSL for type-safe, maintainable builds
- **Minimalist & Simple** - Clean project structure without unnecessary complexity
- **Java 25** - Built with the latest Java features and toolchain
- **Hot Reload Ready** - Quick iteration with automatic builds and server restarts

## 📋 Prerequisites

- **Java Development Kit (JDK) 25** or higher
- **Python 3** (for running the server script)
- **VS Code** with the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) (recommended)
- **Hytale Server JAR and Assets** - See [libs/README.md](./libs/README.md) for instructions on obtaining these files

## 🚀 Quick Start

1. **Clone or download this template**
2. **Obtain the required server files** - Place `HytaleServer.jar` and `Assets.zip` in the `libs/` folder (see [libs/README.md](./libs/README.md))
3. **Open in VS Code** - The Java Extension Pack will automatically configure the project
4. **Customize your plugin** - Rename packages and update manifest (see [Customization](#customization) below)
5. **Build and run** - Use the commands below to build and test your plugin

## 📁 Project Structure

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

## 🛠️ Available Commands

### Build Commands

#### Build JAR

```bash
./gradlew jar
```

Builds your plugin into a JAR file located at `app/build/libs/`. This is the main command you'll use to compile your plugin.

#### Clean Build

```bash
./gradlew clean
```

Removes all build artifacts and the `run/` directory. Use this to start fresh or free up disk space.

### Development Commands

#### Run Plugin with Server

```bash
./run.py
```

**Requirements:**

- Python 3 installed
- `HytaleServer.jar` and `Assets.zip` in the `libs/` folder (see [libs/README.md](./libs/README.md))

This script automatically:

1. Builds your plugin JAR using `./gradlew jar`
2. Creates a `run/` directory with the server setup
3. Copies the server JAR and assets to the run directory
4. Copies your plugin JAR to `run/mods/`
5. Starts the Hytale server with your plugin loaded

The server will run in the foreground. Press `Ctrl+C` to stop it.

### Other Gradle Commands

You can also use standard Gradle commands:

- `./gradlew build` - Full build including tests
- `./gradlew test` - Run unit tests
- `./gradlew clean build` - Clean and rebuild everything

## 🎨 Customization

### Renaming Your Package

The template uses `com.example.demo` as the default package. To customize it:

1. **Update package directories:**

   - Rename `app/src/main/java/com/example/demo/` to `app/src/main/java/com/yourname/yourplugin/`
   - Move `Plugin.java` to the new location

2. **Update package declaration in Plugin.java:**

   ```java
   package com.yourname.yourplugin;
   ```

3. **Update manifest.json:**

   - Edit `app/src/main/resources/manifest.json`
   - Change the `"Main"` field to your new package:

   ```json
   {
     "Main": "com.yourname.yourplugin.Plugin",
     ...
   }
   ```

4. **Update gradle.properties:**

   - Edit `gradle.properties` in the root directory
   - Update `pluginGroup`:

   ```properties
   pluginGroup=com.yourname.yourplugin
   ```

5. **Update settings.gradle.kts (optional):**

   - Edit `settings.gradle.kts` to change the project name:

   ```kotlin
   rootProject.name = "YourPluginName"
   ```

6. **Update manifest.json metadata:**
   - Edit `app/src/main/resources/manifest.json`:
   ```json
   {
     "Group": "YourGroup",
     "Name": "YourPluginName",
     "Version": "1.0.0",
     "Description": "Your plugin description",
     "Authors": [
       {
         "Name": "Your Name",
         "Email": "your@email.com",
         "Url": "https://yourwebsite.com"
       }
     ],
     "Website": "https://github.com/yourusername/yourplugin",
     ...
   }
   ```

### Adding Dependencies

Edit `app/build.gradle.kts` to add dependencies:

```kotlin
dependencies {
    compileOnly(files("../libs/HytaleServer.jar"))

    // Add your dependencies here
    implementation("com.example:library:1.0.0")
}
```

For version management, add entries to `gradle/libs.versions.toml`:

```toml
[versions]
myLibrary = "1.0.0"

[libraries]
myLibrary = { module = "com.example:library", version.ref = "myLibrary" }
```

Then reference it in `build.gradle.kts`:

```kotlin
implementation(libs.myLibrary)
```

## 📝 How to Use

### Development Workflow

1. **Make changes** to your plugin code in `app/src/main/java/`
2. **Build the plugin:**
   ```bash
   ./gradlew jar
   ```
3. **Test with server:**
   ```bash
   ./run.py
   ```
4. **Iterate** - Make changes, rebuild, and restart the server

### Plugin Structure

Your main plugin class (`Plugin.java`) extends `JavaPlugin` and provides lifecycle hooks:

- **Constructor** - Called when the plugin is loaded
- **onEnable()** - Called when the plugin is enabled (register commands, listeners, etc.)
- **onDisable()** - Called when the plugin is disabled (cleanup, save data, etc.)

Example structure for organizing your code:

```
src/main/java/com/yourname/yourplugin/
├── Plugin.java              # Main class
├── commands/
│   ├── HelloCommand.java    # Command handlers
│   └── AdminCommand.java
├── listeners/
│   ├── PlayerListener.java # Event listeners
│   └── BlockListener.java
├── services/
│   ├── DatabaseService.java # Business logic
│   └── ConfigService.java
├── storage/
│   └── DataManager.java     # Data persistence
├── config/
│   └── Config.java          # Configuration
└── utils/
    └── Utils.java           # Utility classes
```

### VS Code Integration

With the Java Extension Pack installed:

- **IntelliSense** - Auto-completion for Hytale API
- **Debugging** - Set breakpoints and debug your plugin
- **Code Navigation** - Jump to definitions, find references
- **Error Detection** - Real-time compilation errors
- **Refactoring** - Rename symbols, extract methods, etc.

The project is automatically detected and configured when you open it in VS Code.

## 📦 Building for Production

1. **Update version** in `app/build.gradle.kts`:

   ```kotlin
   version = "1.0.0"
   ```

2. **Build the JAR:**

   ```bash
   ./gradlew jar
   ```

3. **Find your plugin** at `app/build/libs/YourPluginName-1.0.0.jar`

4. **Distribute** - Share the JAR file with others or upload to a plugin repository

## 🔧 Configuration

### Gradle Properties

Edit `gradle.properties` to configure build behavior:

```properties
# Project Information
pluginGroup=com.yourname.yourplugin
pluginVersion=1.0.0
pluginDescription=Your plugin description

# Gradle Configuration
org.gradle.configuration-cache=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

### Java Version

The project is configured for Java 25. To change the version, edit `app/build.gradle.kts`:

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25)) // Change this
    }
    sourceCompatibility = JavaVersion.VERSION_25 // Change this
    targetCompatibility = JavaVersion.VERSION_25 // Change this
}
```

## 📚 Additional Resources

- **Server Files Setup:** See [libs/README.md](./libs/README.md) for instructions on obtaining `HytaleServer.jar` and `Assets.zip`
- **Hytale API Documentation:** Refer to the Hytale development documentation
- **Gradle Documentation:** [gradle.org/docs](https://docs.gradle.org)

## 🐛 Troubleshooting

### Build Errors

- **Java version mismatch:** Ensure you have JDK 25 installed and configured
- **Missing dependencies:** Run `./gradlew build --refresh-dependencies` to refresh

### Server Won't Start

- **Missing server files:** Ensure `HytaleServer.jar` and `Assets.zip` are in `libs/` (see [libs/README.md](./libs/README.md))
- **Python not found:** Ensure Python 3 is installed and in your PATH
- **Port already in use:** Check if another server instance is running

### VS Code Issues

- **No IntelliSense:** Ensure Java Extension Pack is installed and the project is properly indexed
- **Java Runtime version mismatch:** Ensure you have JDK 25 installed and configured, run `> Java: Configure Java Runtime` to set to JDK 25.
- **Import errors:** Run `./gradlew build` to ensure dependencies are resolved

## 📄 License

This template is provided as-is for Hytale plugin development.

---

**Happy Plugin Development! 🎮**
