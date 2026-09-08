## ssg
SpeedSG (SSG) is a fast-paced version of classic survival games built atop the [CXYZ](https://github.com/javaustin/cxyz) plugin.

---

## Important Notes
- This project depends on the [CXYZ](https://github.com/javaustin/cxyz) for its core plugin.
- **NO** generative AI was used to write or modify any code in this project.

--- 

## Features
- **Auto Join**: Allow players to automatically join a game upon joining the world or server
- **Team Support**: Supports both solo and team-based games seamlessly
- **Scoreboard GUI**: Lobby and in-game scoreboards with configurable info
- **Loot Tables**: Custom loot tables for chests with refilling
- **Event-based automation**: Run commands on death, on kill, game start, etc.
- **Click action-based automation**: Run commands when right/left-clicking with a specific item
- **Configurable Respawns**: Configure respawn amount per player, respawn times, or disable respawns all-together
- **Unlimited Customization**: Customize every single message in the plugin using the [messages.yml](https://github.com/javaustin/ssg/blob/main/src/main/resources/messages.yml) file

--- 

## Installation

### Requirements

- A Bukkit server running version `1.21` or above
- [Apache Maven](https://projects.apache.org/project.html?maven) on your local machine (if building the project)
- The [**CXYZ**](https://github.com/javaustin/cxyz) plugin installed on the server

### Build
###### *Note: This step is not required. You can use the already-provided jars in the `/target` directory*

Run the following Maven command in the project directory:
```bash
mvn clean package
```

The shaded plugin jar will be created in `target/`.

### Install on a server

1. Build the plugin jar or use the provided jars in `target/`.
2. Copy the generated jar into your server’s `plugins/` directory.
3. Install the **CXYZ** plugin on the same server, since SSG depends on it at runtime.
4. Start or restart your server to install the plugin. 
5. Once finished, modify `config.yml` and `maps.yml` to support your server. Restart to apply changes.

### Configuration
[View config.yml](https://github.com/javaustin/ssg/blob/main/src/main/resources/config.yml)  
[View loot.yml](https://github.com/javaustin/ssg/blob/main/src/main/resources/loot.yml)  
[View maps.yml](https://github.com/javaustin/ssg/blob/main/src/main/resources/maps.yml)  
[View messages.yml](https://github.com/javaustin/ssg/blob/main/src/main/resources/messages.yml)  
