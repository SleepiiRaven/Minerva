package net.minervamc.minerva.lib;

/*
A utility library to simplify and streamline code writing for various tasks.

1. Command Class:
   - Use annotations to define commands and handle subcommands with reflection.
   - Example:
     @ICommand(permission = "example.permission", noArgs = true)
     public void exampleCommand(CommandContext context) {
         // Command logic
     }
   - Supports automatic permission checks, custom error messages, and argument handling.
   - ITabComplete works similarly for tab completion:
     @ITabComplete(name = "example")
     public List<String> exampleTabComplete(CommandContext context) {
         return Arrays.asList("option1", "option2");
     }

2. ItemCreator:
   - Provides utility methods and a builder pattern for easily modifying item data.
   - Example:
     ItemStack item = new ItemCreator(Material.DIAMOND_SWORD)
                        .name("Epic Sword")
                        .lore("A sword of legends")
                        .build();

3. Menu Class:
   - Uses functional interfaces to handle clicked slots automatically.
   - Example:
     Menu menu = new Menu("Example Menu", 3);
     menu.setButton(0, new MenuButton(itemStack, click -> {
         // Handle click event
     }));
     - Extending the class is usually best.

4. TextContext:
   - A wrapper for easy use of Components, enabling fluent text styling and formatting.
   - Example:
     Component message = TextContext.get()
                          .red("This is a red message")
                          .black(" and black!")
                          .build();

5. Config Class:
   - Create and manage config files within the plugin's data folder.
   - Example:
     Config config = new Config("settings.yml");
     config.set("setting.path", "value");
     config.save();
   - ConfigManager loads all .yml files into memory and provides access via file paths.
   - Example:
     ConfigManager.getManager().load(plugin);
     Config config = ConfigManager.getManager().getConfig("players/data/example.yml");

6. GlobalEventHandler:
   - A wrapper to create event listeners using lambdas for a more streamlined approach.
   - Example:
     GlobalEventHandler.register(PlayerJoinEvent.class, event -> {
         // Handle player join event
     });

Overall, these classes are designed to reduce boilerplate code and make common tasks easier to implement.
*/

// USED GPT TO WRITE THIS, I GOTTA SLEEP, some methods might be wrong but close
// Have a great rest of ur day :)