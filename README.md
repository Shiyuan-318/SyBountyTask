# SyBountyTask
A better Minecraft server quest system, allowing every player to publish quests and accept quests. Currently supported: publishing quests, quest claiming, quest categorization, quest panel, handling fees, etc.

## SyBountyTask Commands

**Basic Commands:**
- `/sybt open` - Open the task center GUI
- `/sybt task <type> <title> <details> <reward>` - Publish a task（**Task Types:** 1-Material 2-Construction 3-Monster）
- `/sybt get <taskID>` - Claim a task
- `/sybt br <taskID>` - Broadcast and promote a task
- `/sybt complete <taskID>` - Submit task completion
- `/sybt confirm <taskID>` - Confirm task acceptance
- `/sybt delete <taskID>` - Delete an unclaimed task

**Admin Commands:**
- `/sybt reload` - Reload configuration files

![GUI](https://cdn.modrinth.com/data/cached_images/70e8f6d8e5a4341f0dd7d194ac31a783773b7780.png)

![help](https://cdn.modrinth.com/data/cached_images/fa532e4c99d617c0cc1424f0dc652c74badb233c.png)
