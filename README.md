# Global Data minecraft plugin

A plugin created for the play.minermen.net minecraft server.
It is used to share certain scoreboards between servers

## Table of Contents
- [Installation](https://github.com/Nathan-Andrews/networkDataSharingMCPlugin/blob/master/README.md#installation)
- [Features](https://github.com/Nathan-Andrews/networkDataSharingMCPlugin/blob/master/README.md#features)
- [Behaviors](https://github.com/Nathan-Andrews/networkDataSharingMCPlugin/blob/master/README.md#behaviors)

## Installation
- This is meant to be used on paper 1.20.4 servers.
- ### Build
    - Run `maven clean package` to build the project
- ### Server
    - Place the *.jar* file in the *./plugins/* directory of your minecraft server.
    - Reload or restart the server
- ### Config
    - After reloading, a config file for the plugin will be created automatically in *./plugins/GlobalData/config.yml*.
    - You need to set up the config to continue using the plugin
    - `objectives: "..."` the scoreboards which are shared between servers.  Should be formatted as a regex pattern.
    - `directory_path: "..."` path to the shared storage directory.  This is where your database will be stored.  Should be in a directory that all of the servers can access
    - `storage_filename: "..."` the name of the database file.  Usually *database.db*

- Repeat for all servers you want to connect

## Features
- All objectives are stored in a SQLite database.  You can access it with the sqlite command line looks using `sqlite3 database.db`. 

## Behaviors
- Only scoreboards with names matching `objectives` pattern will be effected by this plugin.  By default this is any objective starting with `global`
- `/scoreboard players set <name> <objective> <score>` The score of `<name>` in `<objective>` will be updated to match `<score>` on all connnected servers
- `/scoreboard players add/remove/operation <player> ...` All connected servers will be updated to match the change
- `/scoreboard players reset <name> <objective>` The score of `<name>` in `<objective>` will be reset in all connected servers
- `/scoreboard objectives add <objective> <criteria> [<displayname>]` Tes scoreboard `<objective>` will be created in all connected servers.
    - The `<criteria>` on all connected servers will always be set to `dummy`. Except on the server where the objective was created, where the `<criteria>` will be whatever it was created as (`dummy`,`deathCount`,...).
    - The `[<displayname>]` on all connected servers will always be set to match `<objective>`.  Excepton the server where the objective was created, where the `[<displayname>]` will match whatever it was created as
- `/scoreboard objectives remove <objective>` The scoreboard `<objective>` will be removed on all servers
- `/scoreboard objectives setdisplay ...` Only applies to the current server
- `/scoreboard objectives modify ...` Only applies to the current server
- If a scoreboard that does *not* exist in the database is in the world when plugin is loaded in, that scoreboard will *not* be removed will be added to the database.
    - This is so that old scoreboards can be added in without being erased
- If a scoreboard that *does* exists in the database is in the world when the plugin is loaded in, then that scoreboard will updated to match the contents of the database.
    - This is so that when a server goes temporarily offline, it doesn't overwrite all of the score updates that may have happened in the meantime, when it comes back online
- If two servers are trying to set the same score, then the plugin will add whichever update was performed last to the database.  Avoid doing this since it could cause unexpected behavior
- If a scoreboard is removed using `/objective remove ...` and also has a value set the same tick, the scoreboard may be reset instead of removed.  Avoid doing this since it could cause unexpected behavior
