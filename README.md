# Hytale Plugin Development Template

A simple Hytale plugin development template built with Gradle (Kotlin DSL) and Java 25.
> Enhanced Fork: this project is an improved fork of the [original template](https://github.com/CodyAdam/hytale-plugin-template) from CodyAdam. with additional features for easier development and server execution.

#### WHAT'S NEW IN THIS FORK?????
- Gradle `runServer` task: no python dependency, pure gradle workflow 🤡

### Prerequisites:
- JDK 25 (Adoptium, Temurin, whatever you want i dont care)
- Hytale Server Files (Place `HytaleServer.jar` and `Assets.zip` in the `libs/` folder or do a symlink)

### Quick start

#### 1. Get the server files
You need to place `HytaleServer.jar` and `Assets.zip` in the `libs/` folder 

*Best for quick testing*: you'll need to recopy when udates drop:
Default locations:

- **Windows**: `%appdata%\Hytale\install\release\package\game\latest`
- **Linux**: `$XDG_DATA_HOME/Hytale/install/release/package/game/latest`
- **MacOS**: `~/Application Support/Hytale/install/release/package/game/latest`

Copy `HytaleServer.jar` and `Assets.zip` to the `libs/` folder.

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
- Verifies required files exist
- Sets up the `run/` folder
- Builds your plugin
- Copies it to `run/mods` 
- Start the server

### Customization

#### Update package name
1. Rename `com.example.demo` (inside `app/src/main/java`) to you package path
2. Update package declaration in `Plugin.java`
3. Update `Main` field in `app/src/main/resources/manifest.json`
4. Update `pluginGroup` and `pluginName` inside `gradle.properties`

