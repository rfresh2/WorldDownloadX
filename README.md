# WorldDownloadX

Download multiplayer Minecraft worlds into singleplayer worlds.

This project is in early development, and not recommended for general use yet

## Usage

### GUI

Toggle WDL button is in the pause menu.

Settings can be accessed through ModMenu

### Commands

`wdlx:start <world_name>` - start WDL
`wdlx:stop` - stop WDL

## Tech Details

Other world downloaders operate directly on packets, region files, and other associated files.

WDLX instead runs a local MC server that gets client world data injected into it.

no custom logic has to be written for reading/writing world data formats, it uses the same code as a normal mc server.

this should make it more portable across MC versions, allow API's for querying saved chunks, and eliminate serialization bugs.

## Local Development

### Build

`./gradlew build`

the output jar will be at: `build/libs/WorldDownloadX-<version>.jar`

### Local Dev Run

`./gradlew run`
