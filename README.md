![Java CI](https://github.com/superzanti/ServerSync/workflows/Java%20Build/badge.svg)

- [Quick start guide](https://github.com/superzanti/ServerSync/wiki/Quick-start)
- [Releases](https://github.com/superzanti/ServerSync/releases)
- [Experimental Releases](https://github.com/rheimus/ServerSync/releases)

# Requirements

- Java 16+

# ServerSync

A utility for easy mod management. The player will always be able to connect to your server with less instruction required on how to do so.

Players will need to run serversync.jar before starting Minecraft to sync with their desired server (can be [automated](https://github.com/superzanti/ServerSync/wiki/Automation)).

For pre compiled artifacts see [Releases](https://github.com/superzanti/ServerSync/releases).

## DISCLAIMER:

This utility is only intended for personal use. Mod authors work hard on their craft; please support them by visiting their forums, websites or other project sources.

Using this mod for commercial purposes could violate license(s) make sure to check the terms.

This utility allows servers running it to put **ANY** file in your game folder, such as a virus or a keylogger. So if you are a player please make sure you trust the server you are connecting to, having virus monitoring on the games directory would be a good idea.

## FREQUENTLY ASKED QUESTIONS:

- "This mod isn't doing anything!"
  - This version of serversync is run independent of minecraft for performance (minecraft should be closed).
- "I can't connect to my server"
  - Check the ip and port details are correct on both server and client.
  - Are you using your external/internal IP address?
  - Are you trying to transfer a file larger than your max file size?
  - Are you ignoring a file that is necessary to connect to the server?
- "Can you add feature X? Or fix bug Y?"
  - Probably. Submit it to the issues.
- "I have files such as Optifine that I don't want the server to delete"
  - check out the wiki, there are docs on how to [ignore files](https://github.com/superzanti/ServerSync/wiki/Ignore-&-include-lists-examples)

## What does it do?

ServerSync is a Server <-> Client app, server admins can run the server side while players run the client side.

Server admins configure the files required in order to connect. 

Players run the client to check if they need to download updates or new files before starting the game.

## How do I use it?

ServerSync can be used either as a command line tool or a user interface.

See: [CLI Wiki](https://github.com/superzanti/ServerSync/wiki/Command-line-arguments) & [Quick Start](https://github.com/superzanti/ServerSync/wiki/Quick-start)

> 💡 the default configuration for your java install may allow 'double click' running for jar files.

### Server Admins
Releases include uber jars and windows executables.
```bash
java -jar serversync-server.jar <args>
```

### Players
Releases include uber jars and windows executables.
```bash
java -jar serversync-client.jar <args>
```


## Working with the code

### Building a Jar

```shell script
./gradlew build
```

Find the output in `./<project>/build/libs`

### Building exe files

```shell script
./gradlew build createAllExecutables
```

Find the output in `./<project>/build/launch4j`

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

## Special Thanks

- [P3rf3ctXZer0](https://github.com/P3rf3ctXZer0): Extremely useful feedback & testing
