#!/usr/bin/env python3
import os
import shutil
import subprocess
import sys
from pathlib import Path

# Change working directory to the script's directory
script_dir = os.path.dirname(os.path.abspath(__file__))
os.chdir(script_dir)

# Paths
SERVER_PATH = "./libs/HytaleServer.jar"
ASSETS_PATH = "./libs/Assets.zip"
PLUGIN_JAR_FOLDER = "./app/build/libs"
RUN_FOLDER = "./run"
MODS_FOLDER = "./run/mods"


def run_command(cmd, cwd=None):
    """Run a shell command and check for errors."""
    print(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd, cwd=cwd, check=False)
    if result.returncode != 0:
        print(f"Error: Command failed with exit code {result.returncode}")
        sys.exit(1)
    return result


def main():
    # Step 0: Check if the required files exist in the libs folder
    print("Step 0: Checking for required files in ./libs/")
    missing_files = []
    if not os.path.isfile(SERVER_PATH):
        missing_files.append("HytaleServer.jar")
    if not os.path.isfile(ASSETS_PATH):
        missing_files.append("Assets.zip")
    if missing_files:
        print("ERROR: The following required files are missing in ./libs/:")
        for f in missing_files:
            print(f"  - {f}")
        print(
            "\nPlease read ./libs/README.md for instructions on how to obtain these files before running this script."
        )
        sys.exit(1)

    # Step 1: Build the latest jar using gradlew jar
    print("Step 1: Building jar with gradlew build...")
    run_command(["./gradlew", "build"])

    # Step 2: Ensure run folder exists
    print(f"\nStep 2: Ensuring run folder exists ({RUN_FOLDER})...")
    os.makedirs(RUN_FOLDER, exist_ok=True)

    # Step 3: Copy server and assets to run folder (only if they don't exist)
    print(f"\nStep 3: Copying server and assets to {RUN_FOLDER}...")

    server_dest = os.path.join(RUN_FOLDER, "HytaleServer.jar")
    if not os.path.exists(server_dest):
        if not os.path.exists(SERVER_PATH):
            print(f"Error: Server jar not found at {SERVER_PATH}")
            sys.exit(1)
        shutil.copy2(SERVER_PATH, server_dest)
        print(f"  Copied {SERVER_PATH} -> {server_dest}")
    else:
        print(f"  Server already exists at {server_dest}, skipping copy")

    assets_dest = os.path.join(RUN_FOLDER, "Assets.zip")
    if not os.path.exists(assets_dest):
        if not os.path.exists(ASSETS_PATH):
            print(f"Error: Assets zip not found at {ASSETS_PATH}")
            sys.exit(1)
        shutil.copy2(ASSETS_PATH, assets_dest)
        print(f"  Copied {ASSETS_PATH} -> {assets_dest}")
    else:
        print(f"  Assets already exist at {assets_dest}, skipping copy")

    # Step 4: Copy plugin into run folder into mods subfolder
    print(f"\nStep 4: Copying plugin to {MODS_FOLDER}...")

    # Delete the mods folder if it exists before copying new ones
    if os.path.exists(MODS_FOLDER):
        print(f"  Deleting existing mods folder: {MODS_FOLDER}")
        shutil.rmtree(MODS_FOLDER)

    # Find the plugin jar (it might have a different name)
    plugin_libs_dir = PLUGIN_JAR_FOLDER
    if not os.path.exists(plugin_libs_dir):
        print(f"Error: Plugin build directory not found at {plugin_libs_dir}")
        print("  Make sure gradlew jar completed successfully")
        sys.exit(1)

    # Find the jar file (excluding -sources.jar, -javadoc.jar, etc.)
    jar_files = [
        f
        for f in os.listdir(plugin_libs_dir)
        if f.endswith(".jar")
        and not f.endswith("-sources.jar")
        and not f.endswith("-javadoc.jar")
    ]

    if not jar_files:
        print(f"Error: No plugin jar found in {plugin_libs_dir}")
        print("  Make sure gradlew jar completed successfully")
        sys.exit(1)

    # Move all jars (excluding -sources.jar, -javadoc.jar, etc.) to the mods folder
    os.makedirs(MODS_FOLDER, exist_ok=True)
    for jar_filename in jar_files:
        src_path = os.path.join(plugin_libs_dir, jar_filename)
        dest_path = os.path.join(MODS_FOLDER, jar_filename)
        shutil.copy2(src_path, dest_path)
        print(f"  Copied {src_path} -> {dest_path}")

    # Step 5: Run the server
    print(f"\nStep 5: Running server...")
    print(f"Command: java -jar HytaleServer.jar --assets Assets.zip")
    print(f"Working directory: {os.path.abspath(RUN_FOLDER)}\n")

    run_command(
        ["java", "-jar", "HytaleServer.jar", "--assets", "Assets.zip"], cwd=RUN_FOLDER
    )


if __name__ == "__main__":
    main()
