- Screenshots taking via F2 now takes a screenshot of all splitscreen instances.
- `options.txt`, resource packs, configs, and shaderpacks are now transferred when an instance is created. If Legacy4J is installed then resource assorts would also be transferred.
- (Legacy4J) Player usernames are now rendered in the bottom left corner of an instance when splitscreen is active to help distinguish it from other players.
- The original instance now correctly resizes if it was closed via Cmd+Q / Alt+F4.
- Fixed the GitHub link.
- Updated the wiki.
- (Legacy4J) Opening up the user selection menu now temporarily sets the active controller to the controller that opened the menu.
- Instances are now disconnected if it fails to send its UUID.
- Corrected `AuthMe` to `Auth Me` in an error message.
- The server now uses port 4447 instead of port 4444 for networking.
- The player's UUID is now used for the instance's folder name.
- Opened instances now use the same Java version the main instance is using.
- Added the `/hotjoin instances abort <uuid>` command to disconnect any clients, which is useful if they are in a limbo state or aren't starting.
- The limit for maximum instances has been raised from 2 to 4.
- (Legacy4J) Fixed user selection screen opening when it definitely shouldn't.