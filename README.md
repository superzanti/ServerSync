ServerSync
=========
This is an open source mod that allows for easy server management. The client will always be able to connect to your server and you will never again have to send them the new files and tell them to update. This method avoids a lot of complaining. As a server admin constantly changing configs/updating mods it can get to be quite a pain pushing these updates. Some users have trouble finding the minecraft folder let alone putting mods in the right place.

Currently you can sync:
* Client-side mods
* Mods
* Flans content packs
* Configs

**Currently for Minecraft 1.7.10 Forge 1614+**

If you don't feel like compiling from source and simply want to download a jar file see the releases tab, I update these periodically when theres a large enough addition/change. Please read the disclaimer before downloading.


DISCLAIMER:
-----------
This mod is only meant for personal use or for developers that constantly push their OWN mods. Other developers work very hard on their mods and simply visiting their website, forum post, or github is just a common courtesy. Please don't use this to distribute other people's mods.

Depending on the copyright and/or pattent laws in your area using this mod with other developer's mods for a commercial purpose could be ILLEGAL.

Don't trust anyone with this mod. This mod allows ANY server running it to put ANY jar file in your mods folder. Any mod means any function of java, such as making a virus or a keylogger. So if you are a client please make sure you trust your server administrator.


RECENT UPDATES:
-----------
Version 2.5.2
* Fixed missing class JsonReader should work properly now
* Fixed minor issue with file deletion

Version 2.5.1
* Fixed bug when interacting with zip/jar files

Version 2.5.0
* Added more functionality to the GUI
* Changed Ignoring rules to whitelist for configs

Version 2.4.7
* Added support for flans mod content packs 

Version 2.4.6
* Added the ability to push client-only mods
* Various code optimization/cleanup

Version 2.4.5
* Added the ability to sync files without opening minecraft
* Fixed windows deletion issue, not an issue if using "offline" sync
* Added GUI for "offline" mode

FREQUENTLY ASKED QUESTIONS:
-----------
* "This mod isn't doing anything!"
  * This version of serversync is run independant of minecraft (minecraft should be closed when running serversync), I did this as minecraft did not need to be running for the program to work and the previous method required you to open and close minecraft several times which if you have any more than 2-3 mods then loading time gets very taxing.
  * Run serversync.jar from your mods folder or create a shortcut, serversync will auto populate server details from the config if present
* "I can't connect to my server"
  * Did you check that the config files ip/port details are correct on both server and client
  * Are you using your external/internal IP address apropriately?
  * Are you trying to transfer a file larger than your max file size?
  * Are you ignoring a file that is neccecary to connect to the server?
* "This is so insecure I hate it!"
  * As per the disclaimer this is not intended to be a super secure system, it's more for personal use. Want to play with your kids/partner but you dont feel like teaching all of them exactly how to update mods.
  * The config file allows you to put in your own hashes for the server client commands. This would take a real genious to pull files off the server or send files to you.
* "Can you add feature X? Or fix bug Y?"
  * I don't know. Go submit it to the issues and I'll check it out.
* "You're a horrible programmer"
  * You're entitled to that opinion.
* "Can you make this work without using a custom main menu?"
  * This fork of serversync is run without opening minecraft and in fact is no longer supporting the in-game feature of the previous incarnation.
* "Why does this mod spit out so much 'junk' in my console?"
  * It's simply to help users know that they're not being attacked. It will tell them what IP they're connected to, what mod is being downloaded and more. My hope is that people will actually see this while it's running to know for sure that they can trust their admin. Hey, not everyone reads this. Also now that serversync is it's own entity the entire console is there for debugging purposes.
* "I have files such as optifine that I don't want the server to delete"
  * Specify this in the config files IGNORE_LIST
  * Add client only mods to the clientmods directory on the server
  * Mods in the clientmods folder are automatically added to the ignore list

What does it do exactly?
-----------

* The server starts up and begins listening on the port defined in the config file
* If the server receives the recursive command the server will send a packet to the client containing all the files in the mods folder and the config folder.
* If the server receives the checksum command it will send back a md5 checksum of the file requested by the client.
* If the server receives the update command the server will start a file transfer of the file that the client requested.
* If the server receives the exists command the server will will return a boolean value of weather or not the file requested exists on the server.
* If the server receieves the exit command it will close the connection and destroy the thread. However, this will also happen automatically after X ammount of time as defined in the config file
 
* When the client runs serversync the update script starts
* The client will first request the name of all the files on the server.
* The client will then iterate through each of these files
* If this file exists on the client it will take a checksum of it and ask the server if they are the same
* If the files are different it will send the update command to pull a new file
* If the client does not have the file it will send the update command to download it
* After iterating through all of the server files the client will then iterate through all of it's own files
* If the client has a file that the server does not, it will delete it.

What you should expect to see
--------------

When the program starts up you will see a very lightweight console with general information on progress etc.

![Information Console](http://s31.postimg.org/bxc807u63/ss_snap.png)

Error messages will appear if the server cannot be found or there is some exception in the mod while downloading updates. All these errors should be fairly self explanatory and easy to fix.


Compiling
--------------

Simply git clone the repo, cd into the folder and run 
```
./gradlew shadowJar
```
or for windows
```
gradlew.bat shadowJar
```

If you would like to setup a workspace to work on these files simply run
```
./gradlew setupDecompWorkspace
./gradlew eclipse
```

Then change the working directory of your eclipse to 
```
./eclipse
```

Have at it.