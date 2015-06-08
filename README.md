ServerSync
=========

This is an open source mod that allows for easy server management. It simply syncs the mods folder and the config folder from the server to the client. The client will always be able to connect to your server and you will never again have to send them the new files and tell them to update. My method avoids a lot of complaining. I have a server which runs all of my own mods, as a developer I'm constatly updating these mods, fixing bugs, and making changes to configs. For the past fiew years I've been going around to the users on my server with a USB stick instructing them on how to use mods. Recently I've had some people join my server that don't know how to find the .minecraft folder. So explaining this to them and getting them the new updates can be a real pain.

**Currently for Minecraft 1.7.10 Forge 1403**

If you don't feel like compiling from source and simply want to download the jar file, navigate around this repo's folders. If you're a developer you'll be able to find it easy. Please read the disclaimer before downloading.

DISCLAIMER:
-----------

This mod is only meant to be used for developers that constantly push their OWN mods. Other developers work very hard on their mods and simply visiting their website, forum post, or github is just a common courtesy. Please don't use this to distribute other people's mods.

Not to mention, depending on the copyright and/or pattent laws in your area using my mod with other developer's mods could be ILLEGAL.

Don't trust anyone with my mod. This mod allows ANY server running it to put ANY jar file in your mods folder. Any mod means any function of java, such as making a virus or a keylogger. So if you are a client please make sure you trust your server administrator.


RECENT UPDATES:
-----------
Version 2.1:
* Added feature to ask the server if there has been any updates before ever updating
  * This allows to update all configs regardless of if they change
  * Has almost made the IGNORE_LIST property worthless, but is stil handy to have for client/server side only things like GLSL shaders


FREQUENTLY ASKED QUESTIONS:
-----------
* "This mod isn't doing anything!"
  * I require you to have CustomMainMenu. Create a main menu with a button that runs this mod. Use the buttonid of that button in the config file.
* "I can't connect to my server"
  * Did you check that the config file was the same between both the client and the server?
  * Everything in the config file MUST be the same on both sides.
  * Are you using your external/internal IP address apropriately?
  * Are you trying to transfer a file larger than your max file size?
  * Are you ignoring a file that is neccecary to connect to the server?
* "This is so insecure I hate it!"
  * Go read the disclaimer. It will always be insecure and I don't plan on making it super secure.
  * The config file allows you to put in your own hashes for the server client commands. This would take a real genious to pull files off the server or send files to you.
* "Can you add feature X? Or fix bug Y?"
  * I don't know. Go submit it to the issues and I'll check it out.
* "You're a horrible programmer"
  * I'm an Electrical Engineer not a computer scientist. Please submit a bug report and help me improve.
* "Can you make this work without using a custom main menu?"
  * I'm not sure. I haven't looked into it, but if you have an informed suggestion please let me know and I'll do what I can.
* "Why does this mod spit out so much 'junk' in my console?"
  * It's simply to help users know that they're not being attacked. It will tell them what IP they're connected to, what mod is being downloaded and more. My hope is that people will actually see this while it's running to know for sure that they can trust their admin. Hey, not everyone reads this.
* "I have files such as optifine that I don't want the server to delete"
  * Well then specify that in the config file. It can ignore a download or deletion of any file you want.
* "I want to change how the UI looks so it doesn't say 'The Verse' "
  * This mod requires CustomMainMenu, read up on the CustomMainMenu documents for all of that.

What does it do exactly?
-----------

* The server starts up and begins listening on the port defined in the config file
* If the server receives the recursive command the server will send a packet to the client containing all the files in the mods folder and the config folder.
* If the server receives the checksum command it will send back a md5 checksum of the file requested by the client.
* If the server receives the update command the server will start a file transfer of the file that the client requested.
* If the server receives the exists command the server will will return a boolean value of weather or not the file requested exists on the server.
* If the server receieves the exit command it will close the connection and destroy the thread. However, this will also happen automatically after X ammount of time as defined in the config file
 
* When the client clicks the button which has the ID defined in the config file the client will start the update script
* The client will first request the name of all the files on the server.
* The client will then iterate through each of these files
* If this file exists on the client it will take a checksum of it and ask the server if they are the same
* If the files are differnt it will send the update command to pull a new file
* If the client does not have the file it will send the update command to download it
* After iterating through all of the server files the client will then iterate through all of it's own files
* If the client has a file that the server does not, it will delete it.
* When all this finishes if there were updated files the client will ask the user if she/he wants to restart the client to apply the changes
* If there are no files to update the client will join the server that is defined in the IP field of the config file

Compiling
--------------

Simply git clone my repo, cd into the folder and run 
```
./gradlew build
```
or for windows
```
gradlew.bat build
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

I don't expect these instructionis to change, but there will undoubtedly come a point where everyone has moved on from minecraft, forge makes some major changes that I don't want to keep up with, or I simply lose interest in maintaining this project.

What you should expect to see
--------------

When the game starts up you will see a very lightweight menu.

![Start Screen](/previewImages/startScreen.bmp)

Upon clicking the connect button, the client will start checking for updates.

![Downloading](/previewImages/downloading.bmp)

If there was a download to be had, the mod will notify the user.

![Relaunch](/previewImages/relaunch.bmp)

If the user decides to click no and then connect again it will tell the user an update is pending.

![Click Twice](/previewImages/clickTwice.bmp)

A similar error message will appear if the server cannot be found or there is some exception in the mod while downloading updates. All these errors are solved simply by restarting minecraft and making sure the server is up. If there is nothing to download it will automatically connect to user to the server.

![Connected](/previewImages/connected.bmp)
