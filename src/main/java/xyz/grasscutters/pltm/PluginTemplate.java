package xyz.grasscutters.pltm;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.plugin.Plugin;
import xyz.grasscutters.pltm.objects.*;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * The Grasscutter plugin template.
 * This is the main class for the plugin.
 */
@SuppressWarnings("deprecation")
public final class PluginTemplate extends Plugin {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /* Turn the plugin into a singleton. */
    private static PluginTemplate instance;
    private static int CurrentBanner;

    /**
     * Gets the plugin instance.
     * @return A plugin singleton.
     */
    public static PluginTemplate getInstance() {
        return instance;
    }
    
    /* The plugin's configuration instance. */
    private PluginConfig configuration;

    // Save State for the current banner that the program is on
    private String STATE_FILE_PATH;

    // Initilize int for state
    int lastProcessedIndex = 0;

    /**
     * This method is called immediately after the plugin is first loaded into system memory.
     */
    @Override public void onLoad() {
        // Set the plugin instance.
        instance = this;

        // Get the configuration file.
        var config = new File(this.getDataFolder(), "config.json");

        //Set location of the state file
        STATE_FILE_PATH = this.getDataFolder().getPath() + "/state.json";

        //Load state from file
        lastProcessedIndex = loadState();

        // Load the configuration.
        try {
            if(config.exists()) {
                this.getLogger().info("[Banner Replacer] Loading config file");
            } else if (!config.createNewFile()) {
                this.getLogger().error("Failed to create config file.");
            } else {
                try (FileWriter writer = new FileWriter(config)) {
                    InputStream configStream = this.getResource("config.json");
                    if(configStream == null) {
                        this.getLogger().error("Failed to save default config file.");
                    } else {
                        writer.write(new BufferedReader(
                                new InputStreamReader(configStream)).lines().collect(Collectors.joining("\n"))
                        ); writer.close();

                        this.getLogger().info("Saved default config file.");
                    }
                }
            }

            // Put the configuration into an instance of the config class.
            this.configuration = gson.fromJson(new FileReader(config), PluginConfig.class);


            System.out.println("Set Reload time to: " + this.configuration.ReloadTime);

        } catch (Exception exception) {
            this.configuration.ReloadTime = 30; //Set to 30 mins if the JSON is invalid.
            this.getLogger().error("Unable to load configuration file. Defaulting to 30 minutes");
        }
        
        // Log a plugin status message.
        this.getLogger().info("The Banner Replacer plugin was loaded");
    }

    /**
     * This method is called before the servers are started, or when the plugin enables.
     */
    @Override public void onEnable() {
        String sourceFolderPath = "./bannersData/";
        String destinationFolderPath = "./data";

        if (lastProcessedIndex == -1) {
            System.err.println("Failed to load state. Resuming from the beginning.");
        } else {
            System.out.println("Resuming from file index: " + lastProcessedIndex);
        }

        // Schedule the task to run every 10 seconds once the loop is done
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                replaceBannerJSON(sourceFolderPath, destinationFolderPath, lastProcessedIndex);
            }
        }, 0, 10 * 1000);

        this.getLogger().info("The Banner Replacer plugin has been enabled.");
    }

    /**
     * This method is called when the plugin is disabled.
     */
    @Override public void onDisable() {
        // Log a plugin status message.
        this.getLogger().info("The example plugin has been disabled.");
    }

    /**
     * Gets the plugin's configuration.
     * @return A plugin config instance.
     */
    public PluginConfig getConfiguration() {
        return this.configuration;
    }

    private void replaceBannerJSON(String sourceFolderPath, String destinationFolderPath, int startIndex) {
        File sourceFolder = new File(sourceFolderPath);
        File[] files = sourceFolder.listFiles();

        if (files == null) {
            System.err.println("Source folder is empty or does not exist.");
            return;
        }

        // Iterate over all files in the source
        for (int i = startIndex; i < files.length; i++) {
            File file = files[i];

            if (file.isFile()) {
                String sourceFilePath = file.getAbsolutePath();

                // Read contents from current file
                StringBuilder fileContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line).append("\n");
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file: " + e.getMessage());
                }

                try {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();

                    JsonParser parser = new JsonParser();
                    JsonElement jsonElement = parser.parse(new JsonReader(new StringReader(fileContent.toString())));

                    updateEndTime(jsonElement);

                    // Write modified JSON content to 'Banners.json' file in destination folder
                    String destinationFilePath = destinationFolderPath + "/Banners.json";
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFilePath))) {
                        gson.toJson(jsonElement, writer);
                    } catch (IOException e) {
                        System.err.println("Error writing Banners.json file: " + e.getMessage());
                    }

                    Grasscutter.getGameServer().getGachaSystem().load();
                    Grasscutter.getGameServer().getShopSystem().load();

                    //Send a messaged to all online user
                    for (Player p : Grasscutter.getGameServer().getPlayers().values()) {
                        CommandHandler.sendMessage(p, "Wish shop updated!");
                    }

                    //Debug
                    this.getLogger().info("Replaced the banner with " + file.getName() +". Sleeping for "+ this.configuration.ReloadTime + " minutes.");
                    lastProcessedIndex = i;
                    saveState(lastProcessedIndex);

                } catch (JsonParseException e) {
                    System.err.println("Error parsing JSON: " + e.getMessage());
                }


                try {
                    long SleepTime;
                    if (this.configuration.ReloadTime <= 0) {
                        SleepTime = 1;
                    } else {
                        SleepTime = this.configuration.ReloadTime;
                    }
                    Thread.sleep(SleepTime * 60 * 1000);  // sleep for configured minutes
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // Set the lastProcessedIndex to 0
        lastProcessedIndex = 0;
    }

    private void updateEndTime(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            for (String key : jsonObject.keySet()) {
                JsonElement value = jsonObject.get(key);
                if (value.isJsonObject() || value.isJsonArray()) {
                    updateEndTime(value);
                } else if (key.equals("endTime")) {
                    long endTimeUnix = (System.currentTimeMillis() / 1000) + this.configuration.ReloadTime * 60; // Current Unix time + configured minutes
                    jsonObject.addProperty(key, endTimeUnix);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray jsonArray = element.getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                updateEndTime(jsonElement);
            }
        }
    }

    private void saveState(int lastProcessedIndex) {
        try (FileWriter writer = new FileWriter(STATE_FILE_PATH)) {
            writer.write(String.valueOf(lastProcessedIndex));
        } catch (IOException e) {
            System.err.println("Error saving state: " + e.getMessage());
        }
    }

    private int loadState() {
        assert STATE_FILE_PATH != null;
        File stateFile = new File(STATE_FILE_PATH);
        if (!stateFile.exists()) {
            try {
                if (!stateFile.createNewFile()) {
                    this.getLogger().error("Failed to create state file.");
                    return 0;
                }
                // Initialize state file with initial index value (0)
                saveState(0);
            } catch (IOException e) {
                this.getLogger().error("Error creating state file: " + e.getMessage());
                return 0;
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE_PATH))) {
            String indexStr = reader.readLine();
            return Integer.parseInt(indexStr);
        } catch (IOException | NumberFormatException e) {
            this.getLogger().error("Error loading state: " + e.getMessage());
            return 0;
        }
    }
}