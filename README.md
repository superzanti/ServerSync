[![Build Status](https://travis-ci.org/rheimus/ServerSync.svg?branch=master)](https://travis-ci.org/rheimus/ServerSync)

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
  * This version of serversync is run independant of minecraft (minecraft should be closed when running serversync), I did this as minecraft did not need to be running for the program to work and the previous method required you to open and close minecraft several times which if you have any more than 2-3 mods then loading time gets very taxing.
  * Run serversync.jar from your minecraft folder or create a shortcut, serversync will auto populate server details from the config if present
* "I can't connect to my server"
  * Check that the ip/port details are correct on both server and client
  * Are you using your external/internal IP address apropriately?
  * Are you trying to transfer a file larger than your max file size?
  * Are you ignoring a file that is neccecary to connect to the server?
* "This is so insecure I hate it!"
  * As per the disclaimer this is not intended to be a super secure system, it's more for personal use. Want to play with your kids/partner but you dont feel like teaching all of them exactly how to update mods.
  * Please direct any useful security material to the issues, shall look into it
* "Can you add feature X? Or fix bug Y?"
  * Probably. Submit it to the issues and I'll check it out.
* "I have files such as optifine that I don't want the server to delete"
  * Specify this in the configs IGNORE_LIST
  * Add client only mods to the clientmods directory on the server if you are a server admin, note that clients can refuse these
  * Mods in the clientmods folder are automatically ignored in the deletion phase

What does it do exactly?
-----------

* The server starts up and begins listening on the port defined in the config file
* When a connection is made to a client the server listens for messages generated for this client
* On receiving a message the server will react appropriately or send an error back to the client
* After iterating through all of the server files the client will then iterate through all of it's own files
* If the client has a file that the server does not barring ignore rules, it will delete it.

What you should expect to see
--------------

When the program starts up you will see a very lightweight console with general information on progress etc.

Client GUI: (server has no GUI at the moment)

![Information Console](http://s31.postimg.org/bxc807u63/ss_snap.png)

Error messages will appear if the server cannot be found or there is some exception in the mod while downloading updates. All these errors should be fairly self explanatory and easy to fix.


Compiling
--------------

Simply git clone the repo, cd into the folder and run 
```
./gradlew build
```

If you would like to setup a workspace to work on these files simply run
```
./gradlew eclipse
```

Then change the working directory of your eclipse to 
```
./eclipse
```

Have at it.
