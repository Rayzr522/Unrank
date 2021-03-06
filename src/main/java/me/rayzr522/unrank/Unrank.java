package me.rayzr522.unrank;

import me.rayzr522.unrank.command.CommandUnrank;
import me.rayzr522.unrank.data.TierManager;
import me.rayzr522.unrank.listeners.PlayerListener;
import me.rayzr522.unrank.utils.MessageHandler;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * @author Rayzr
 */
public class Unrank extends JavaPlugin {
    private static Unrank instance;
    private final MessageHandler messages = new MessageHandler();
    private final TierManager tierManager = new TierManager(this);
    private Permission permissions;

    /**
     * @return The current instance of Unrank.
     */
    public static Unrank getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        if (!setupPermissions()) {
            Bukkit.getPluginManager().disablePlugin(this);
            getLogger().severe("Failed to connect to any permissions plugins via Vault!");
            return;
        }

        instance = this;

        // Load configs
        reload();

        // Set up commands
        getCommand("unrank").setExecutor(new CommandUnrank(this));

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    /**
     * (Re)loads all configs from the disk
     */
    public void reload() {
        saveDefaultConfig();
        reloadConfig();

        messages.load(getConfig("messages.yml"));
        tierManager.load();
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> serviceProvider = getServer().getServicesManager().getRegistration(Permission.class);
        permissions = serviceProvider.getProvider();
        return permissions != null;
    }

    /**
     * If the file is not found and there is a default file in the JAR, it saves the default file to the plugin types folder first
     *
     * @param path The path to the config file (relative to the plugin types folder)
     * @return The {@link YamlConfiguration}
     */
    public YamlConfiguration getConfig(String path) {
        if (!getFile(path).exists() && getResource(path) != null) {
            saveResource(path, true);
        }
        return YamlConfiguration.loadConfiguration(getFile(path));
    }

    /**
     * Attempts to save a {@link YamlConfiguration} to the disk, with any {@link IOException}s being printed to the console.
     *
     * @param config The config to save
     * @param path   The path to save the config file to (relative to the plugin types folder)
     */
    public void saveConfig(YamlConfiguration config, String path) {
        try {
            config.save(getFile(path));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save config", e);
        }
    }

    /**
     * @param path The path of the file (relative to the plugin types folder)
     * @return The {@link File}
     */
    public File getFile(String path) {
        return new File(getDataFolder(), path.replace('/', File.separatorChar));
    }

    /**
     * Translates a message from the language file.
     *
     * @param key     The key of the message to translate
     * @param objects The formatting objects to use
     * @return The formatted message
     */
    public String tr(String key, Object... objects) {
        return messages.tr(key, objects);
    }

    /**
     * Checks a target {@link CommandSender} for a given permission, and optionally sends a message if they don't.
     * <br>
     * This will automatically prefix any permission with the name of the plugin.
     *
     * @param target      The target {@link CommandSender} to check
     * @param permission  The permission to check, excluding the permission base (which is the plugin name)
     * @param sendMessage Whether or not to send a no-permission message to the target
     * @return Whether or not the target has the given permission
     */
    public boolean checkPermission(CommandSender target, String permission, boolean sendMessage) {
        String fullPermission = String.format("%s.%s", getName(), permission);

        if (!target.hasPermission(fullPermission)) {
            if (sendMessage) {
                target.sendMessage(tr("no-permission", fullPermission));
            }

            return false;
        }

        return true;
    }

    /**
     * @return The {@link MessageHandler} instance for this plugin
     */
    public MessageHandler getMessages() {
        return messages;
    }

    public TierManager getTierManager() {
        return tierManager;
    }

    public Permission getPermissions() {
        return permissions;
    }

    /**
     * Updates the last reset time to the current time.
     */
    public void updateLastResetTime() {
        getConfig().set("last-reset", System.currentTimeMillis());
        saveConfig();
    }

    /**
     * @return The last time that Unrank was activated and user ranks were reset.
     */
    public long getLastResetTime() {
        return getConfig().getLong("last-reset", 0L);
    }
}
