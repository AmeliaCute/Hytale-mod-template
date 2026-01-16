# Hytale Plugin Development Template

A simple Hytale plugin development template built with Gradle (Kotlin DSL) and Java 25.
> Enhanced Fork: this project is an improved fork of the [original template](https://github.com/CodyAdam/hytale-plugin-template) from CodyAdam, with additional features for easier development and server execution.

#### WHAT'S NEW IN THIS FORK?????
- **Gradle `runServer` task**: no python dependency, pure gradle workflow 🤡
- **Automatic Hytale detection**: No need to manually copy server files anymore :3
- **Dev mode with symlinks**: Instant asset updates without rebuilding
- **Manifest templating**: Dynamic manifest generation from `gradle.properties`

### Prerequisites:
- JDK 25 (Adoptium, Temurin, whatever you want i dont care)
- Hytale installed via the official launcher

### Quick start

#### 1. Configure your plugin
Edit `gradle.properties` to customize your plugin:

```properties
# Game Configuration
gamePatchline=release

# Plugin Information
pluginName=MyAwesomePlugin
pluginVersion=1.0.0
pluginGroup=com.yourname
pluginDescription=My awesome Hytale plugin
pluginMain=com.yourname.myawesomeplugin.MainPlugin
```

Edit `app/src/main/generated/manifest.json` to set your plugin metadata (uses variables from gradle.properties):

```json
{
  "Group": "$pluginGroup",
  "Name": "$pluginName",
  "Version": "$pluginVersion",
  "Description": "$pluginDescription",
  "Authors": [
    {
      "Name": "YourName",
      "Email": "your.email@example.com",
      "Url": "https://yourwebsite.com"
    }
  ],
  "Website": "https://yourplugin.com",
  "Main": "$pluginMain"
}
```

#### 2. Build your plugin
```bash
$ ./gradlew build
```

JAR output: `app/build/libs/`

#### 3. Run the server
```bash
$ ./gradlew runServer
```

##### This automatically
- Detects your Hytale installation (Windows/Linux/MacOS)
- Copies `HytaleServer.jar` and `Assets.zip` to `run/` folder
- Builds your plugin
- Installs it to `run/mods/` with symlinked assets
- Starts the server

### Customization

#### Update package name
1. Rename `net.example` (inside `app/src/main/java`) to your package path
2. Update package declaration in `Plugin.java`
3. Update `pluginMain` in `gradle.properties` to match your main class
4. Update `pluginGroup` in `gradle.properties`

#### Custom Hytale installation path
If Hytale is not in the default location, set the `hytale_home` property:

```bash
$ ./gradlew runServer -Phytale_home="/path/to/your/hytale"
```

Or add it to `gradle.properties`:
```properties
hytale_home=/custom/path/to/hytale
```

Default locations checked:
- **Windows**: `%appdata%\Hytale\install\release\package\game\latest`
- **Linux**: `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale` or `~/.local/share/Hytale`
- **MacOS**: `~/Library/Application Support/Hytale`

### Development workflow

#### Dev mode (recommended for faster iteration)
```bash
$ ./gradlew installDevMod
```

This creates:
- **Code JAR**: Compiled classes only
- **Symlinked assets**: Direct link to your `src/main/resources` folder

**Benefits**: Asset changes are reflected immediately without rebuilding!

#### Available tasks
- `build` - Build the final release JAR
- `runServer` - Build, install, and run the server
- `installDevMod` - Install dev version (code + symlinked assets)
- `buildRelease` - Build the distribution JAR
- `cleanMods` - Clean only the mods folder