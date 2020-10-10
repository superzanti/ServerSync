![Java CI](https://github.com/superzanti/ServerSync/workflows/Java%20CI/badge.svg)

[Quick start guide](https://github.com/superzanti/ServerSync/wiki/Quick-start)

[Releases](https://github.com/superzanti/ServerSync/releases)  
[Experimental Releases](https://github.com/rheimus/ServerSync/releases)

ServerSync
=========
This is an open source utility that allows for easy mod management. The client will always be able to connect to your server and you will never again have to send them the new files and tell them to update. This method avoids a lot of complaining. As a server admin constantly changing configs/updating mods it can get to be quite a pain pushing these updates. Some users have trouble finding the minecraft folder let alone putting mods in the right place.

Clients will need to run serversync.jar before starting Minecraft to sync with their desired server, sync on connect from within Minecraft is desirable but is currently outside the scope of this project.

Technically you could sync any game/filesystem using serversync however it does have some specific funtionality intended for use with Minecraft.

Currently ServerSync has support for:
* Client only mods
* Configs
* Anything else, config supports adding custom directories to sync

If you don't feel like compiling from source and simply want to download a jar file see the releases tab, I update these periodically when theres a large enough addition/change. Please read the disclaimer before downloading.

Honorable Mentions:
- [P3rf3ctXZer0](https://github.com/P3rf3ctXZer0): Extremely useful feedback & testing


DISCLAIMER:
-----------
This utility is only intended for personal use. Other developers work very hard on their mods and simply visiting their website, forum post, or github is just a common courtesy. Please don't use this to mass distribute other people's mods without explicit permisson.

Depending on the copyright and/or pattent laws in your area using this mod with other developer's mods for a commercial purpose could be ILLEGAL, check licenses.

This utility allows ANY server running it to put ANY file in your game folder, such as a virus or a keylogger. So if you are a client please make sure you trust your server administrator and as a good measure make sure to scan your games folders for malicious content.


FREQUENTLY ASKED QUESTIONS:
-----------
* "This mod isn't doing anything!"
  * This version of serversync is run independent of minecraft (minecraft should be closed when running serversync), I did this as minecraft did not need to be running for the program to work and the previous method required you to open and close minecraft several times which if you have any more than 2-3 mods then loading time gets very taxing.
* "I can't connect to my server"
  * Check the ip and port details are correct on both server and client.
  * Are you using your external/internal IP address?
  * Are you trying to transfer a file larger than your max file size?
  * Are you ignoring a file that is necessary to connect to the server?
* "This is so insecure I hate it!"
  * As per the disclaimer this is not intended to be a super secure system, it's more for personal use.
  * Please direct any useful security material to the issues, shall look into it.
* "Can you add feature X? Or fix bug Y?"
  * Probably. Submit it to the issues, and I'll check it out.
* "I have files such as Optifine that I don't want the server to delete"
  * check out the wiki, there are docs on how to [ignore files](https://github.com/superzanti/ServerSync/wiki/Ignore-&-include-lists-examples)

What does it do?
-----------
ServerSync is a Server <-> Client app, both are bundled into the same file.

The server configures what is required for clients to connect and serves files.

The client requests information from the server and downloads / removes files based on what is required to connect.

How do I use it?
--------------
ServerSync can be used either as a command line tool or a user interface, running with no arguments assumes that you want to start the GUI.  
See: [CLI Wiki](https://github.com/superzanti/ServerSync/wiki/Command-line-arguments) & [Quick Start](https://github.com/superzanti/ServerSync/wiki/Quick-start)


Working with the code
--------------
### Building a Jar
```shell script
./gradlew build
```
Find the output in `./build/libs`

### Building exe files
```shell script
./gradlew build createAllExecutables
```
Find the output in `./build/launch4j`

### Clean project
```shell script
./gradlew clean
```

## IDE Setup
### IntelliJ
Open the cloned project, IntelliJ should auto detect the gradle files and do the rest for you.
### Eclipse
```
./gradlew eclipse
```

Then change the working directory of your eclipse to 
```
./eclipse
```

Have at it.
