# ServerSync
Sync files between client and server for Minecraft Forge. 

This branch is intended for building versions of Serversync that run through 
MinecraftForge's 
server instance

This is more of a temp workaround while work is being done on separating out ServerSync from forge entirely, in theory the 1.9.4 version should work 
on 1.8+ until the forge team decide to change packaging again.

ServerSync in it's current state uses forge's sided proxy to decern between running as a client or a server, that's about it.

## To add another build artifact:
- Copy one of the project folders e.g. 1-7-10
- Rename to desired version/reason e.g. 1-6-4
- Add folder name to __settings.gradle__
- Make any desired changes in the new directory/project
- Artifacts by default will be put in the ~/publish directory

### Run builds from the root directory
