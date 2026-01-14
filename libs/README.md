# 📂 `libs/` Folder

This folder should contain the core game files required to run your Hytale-based server and plugins:

- **`HytaleServer.jar`** – The game server JAR
- **`Assets.zip`** – The game asset archive

**These files are not provided in this repository. You must obtain them yourself from your own game installation or from official sources.**

---

## How to Obtain the Required Files

You have two options:

### 1. **Manually Copy from the Hytale Launcher**

Best for quick testing – you'll need to re-copy whenever game updates are released.

**Default Locations:**

- **Windows:**  
  `%appdata%\Hytale\install\release\package\game\latest`
- **Linux:**  
  `$XDG_DATA_HOME/Hytale/install/release/package/game/latest`
- **MacOS:**  
  `~/Application Support/Hytale/install/release/package/game/latest`

Inside this folder you should find:

- `HytaleServer.jar`
- `Assets.zip`
- (Optionally, you may see `Client/` and `Server/` directories.)

Copy `HytaleServer.jar` and `Assets.zip` into this `libs/` directory.

---

### 2. **Use the [Hytale Downloader CLI](https://example.com/hytale-downloader-link)**

Best for production servers and automatic updates.

- Download: `hytale-downloader.zip` (for Linux & Windows)
- See included `QUICKSTART.md` for full instructions.

#### Example Commands

| Command                                      | Description                           |
| -------------------------------------------- | ------------------------------------- |
| `./hytale-downloader`                        | Download the latest server & assets   |
| `./hytale-downloader -print-version`         | Show game version without downloading |
| `./hytale-downloader -version`               | Show hytale-downloader version        |
| `./hytale-downloader -check-update`          | Check for downloader updates          |
| `./hytale-downloader -download-path FILE`    | Download to specific file             |
| `./hytale-downloader -patchline pre-release` | Download pre-release version          |
| `./hytale-downloader -skip-update-check`     | Skip automatic updater                |

---

**Place the downloaded `HytaleServer.jar` and `Assets.zip` into this `libs/` directory before running or building the project.**
