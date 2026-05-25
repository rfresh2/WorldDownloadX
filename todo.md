### Entities

edge cases that need testing:
- varying entity unload distances
- differentiate entity being killed and unloaded (we want to save unloaded)
- revisiting entities
- should remove entity chunk if it previously had entities but now doesn't

### Block Entities & Containers

need to capture and inject their contents when opened ingame

e.g. chests, shulkers, lectern, etc.

### Ender Chest

need to capture contents when opened ingame

### Maps

need to track which we actually see during the wdl and then only save those

### Player Statistics

server will send us statistics data on request packet

need to check if player already requested them before wdl start, and if not do so at start

### Backup

zip a copy of wdl contents after stop

### Debug 

toggleable logging about wdl progress and state

### Threading

carefully decide what actions can be done off the render thread

client level access is not thread safe

if copying is less costly than in-thread operations, could be worth it to create a copy of data on the render thread and send that offthread

main thing we want to avoid is unnecessary freezes when too much work is done per frame

so if the client level is not in a state change, it is acceptable to only split up work across multiple frames, which will minimize freezes

### Dimension Switches

continue world download when switching dimensions

but should still stop world download automatically if we exit the server (switch to null dimension)

### Container ESP

esp to indicate which containers have their contents saved

needs more optimization than worldtools to be acceptable

test on a 1k dub stash

### Download Progress GUI

in-game hud showing download progress

### Download Statistics

track what data has been downloaded

### Save Browser GUI

when starting a WDL, show a gui that includes a previous saves browser

if a previous save is selected, the download continues on that save

save browser should show the save name, icon, last modified date, and maybe statistics

### Multiple MC Versions

create branches for:
- 1.21.1
- 1.21.4
- 1.21.5
- 1.21.8
- 1.21.10
- 1.21.11
- 26.1.2

long term there will be >1 actively supported version.

will use same maintainence strategy as XaeroPlus:
- one base branch where feature development occurs
- changes get merged up from base branch
- releases include builds from all supported versions

### NeoForge

low priority

would need more testing for compatibility with modpacks. otherwise this is mostly useless

### Release Workflow

github actions workflow to build and publish releases to github, modrinth, curseforge

### Modrinth + CurseForge

set up project page on modrinth and curseforge

### Unit Tests

- create test case worlds
- run a local dummy mc server
- start client and connect to server
- start wdl
- split world into packets locally and send to client
- stop wdl
- compare contents of wdl and test world

can possibly use the fabric gametest api, or may need custom harness

