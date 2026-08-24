# GameCafeManager Windows release

GameCafeManager is packaged with the JDK `jpackage` tool. The result is a native
Windows launcher and installer containing a private Java runtime, the JavaFX Windows
modules, the application JAR, and the SQLite JDBC driver. A customer does not need
Java, JavaFX, Maven, NetBeans, SQLite, or any development tool.

## Release configuration

- Application name: `GameCafeManager`
- Application version: `1.0.0` (read from `pom.xml`)
- Vendor: `Ehab Salah`
- Main module: `com.gamecafe.gamecafemanager`
- Main class: `com.gamecafe.gamecafemanager.presentation.GameCafeApplication`
- Installer type: per-user Windows EXE
- Shortcuts: Windows desktop and Start Menu
- Installer output: `release\windows\GameCafeManager-Setup.exe`
- Self-contained app image: `release\windows\app-image\GameCafeManager`

The release keeps the existing Java 11 bytecode target, JavaFX 13, and SQLite JDBC
3.53.2.1. The build JDK can be newer because its `jpackage`/`jlink` tools create the
private runtime that ships with the application.

## Build-computer prerequisites

These tools are needed only on the computer that creates the release:

1. A Windows JDK containing `jpackage` (JDK 14 or newer).
2. Maven.
3. WiX Toolset 3.0 or newer for the EXE/MSI step only.

The official portable WiX 3.14.1 archive is named `wix314-binaries.zip`. Extract it
under `.build-tools\wix314`, or install WiX and add it to `PATH`. The `.build-tools`
folder and generated `release` folder are intentionally ignored by Git.

To use the portable build tool, run this once from the project root:

```powershell
New-Item -ItemType Directory -Force .\.build-tools\wix314 | Out-Null
Invoke-WebRequest `
  -Uri "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip" `
  -OutFile .\.build-tools\wix314-binaries.zip
$wixHash = (Get-FileHash -Algorithm SHA256 .\.build-tools\wix314-binaries.zip).Hash
if ($wixHash -ne "6AC824E1642D6F7277D0ED7EA09411A508F6116BA6FAE0AA5F2C7DAA2FF43D31") {
    throw "WiX archive checksum did not match the expected release."
}
Expand-Archive -Force .\.build-tools\wix314-binaries.zip .\.build-tools\wix314
```

## Build commands

Run these commands from the project root in PowerShell:

```powershell
mvn clean verify
```

The full build above compiles the project, runs every automated test, and creates the
application JAR.

Create both the application image and installer:

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\windows\build-release.ps1
```

Create only the self-contained application image (WiX is not required):

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\windows\build-release.ps1 -PackageType AppImage
```

Create the installer after WiX is available:

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\windows\build-release.ps1 -PackageType Installer
```

`-SkipTests` is available for local packaging iterations, but it must not be used for
a final customer release.

## SQLite database

The packaged application uses the existing database infrastructure and stores its
database at:

```text
C:\Users\<WindowsUser>\.game-cafe-manager\game-cafe.db
```

The folder and database are created automatically on first application startup. This
location is writable by the signed-in user and remains outside the installed program
directory, so installing under `Program Files` or `AppData` does not prevent writes.
The database is not removed when the application closes and is reopened on the next
launch. SQLite itself does not need to be installed because the packaged SQLite JDBC
JAR contains the Windows native SQLite component.

For an isolated verification database, set `GAME_CAFE_DATABASE_PATH` before launching
the application. This override is intended for testing and is not required for normal
customer use.

## Customer installation from USB

1. Copy `GameCafeManager-Setup.exe` to the USB drive.
2. On the customer computer, open the USB drive and double-click the installer.
3. Complete the installer. It creates desktop and Start Menu shortcuts.
4. Launch **GameCafeManager** from either shortcut.
5. On the first launch, create the initial administrator account and begin using the
   application.

No command prompt or additional software installation is required.

## Clean Windows machine test

Use a spare Windows computer or a fresh Windows virtual machine snapshot:

1. Confirm that Java, JavaFX, Maven, NetBeans, and SQLite are not installed. In a new
   PowerShell window, `Get-Command java,mvn,sqlite3 -ErrorAction SilentlyContinue`
   should return nothing.
2. Copy only `GameCafeManager-Setup.exe` from the USB drive to the machine.
3. Run the installer and accept the default per-user installation location.
4. Launch the desktop shortcut and create the initial administrator.
5. Create a station, edit its hourly rate, create a product, and update its price and
   stock.
6. Start a session, wait briefly, and confirm that elapsed time and current gaming
   cost update.
7. Add the product, confirm stock decreases, finish the session, confirm checkout,
   and open the invoice preview.
8. Close and reopen the application. Confirm the station, product, completed session,
   invoice data, reports, settings, and remembered login still exist.
9. Start another session, note its start time, close the application while it remains
   ACTIVE, wait at least one minute, and reopen it. Confirm the same session is active
   and its elapsed time includes the period while the application was closed.
10. Uninstall from Windows **Installed apps**, reinstall the same installer, and verify
    the per-user database is still available. Delete the `.game-cafe-manager` folder
    only if a deliberate full data reset is required.

## Release limitations

- The generated installer is not digitally signed unless a separate Windows code-
  signing certificate is supplied. Windows SmartScreen can therefore show an
  unrecognized-publisher warning.
- Windows packages must be built on Windows; `jpackage` does not cross-package native
  installers for another operating system.
- Backup/restore, automatic updates, remote databases, and server features are not
  part of this release phase.
