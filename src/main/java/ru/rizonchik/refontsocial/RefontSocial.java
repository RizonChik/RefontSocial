package ru.rizonchik.refontsocial;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.command.ReputationCommand;
import ru.rizonchik.refontsocial.gui.GuiService;
import ru.rizonchik.refontsocial.listener.InteractionTracker;
import ru.rizonchik.refontsocial.listener.SeenListener;
import ru.rizonchik.refontsocial.placeholder.ReputationExpansion;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.storage.Storage;
import ru.rizonchik.refontsocial.storage.StorageType;
import ru.rizonchik.refontsocial.storage.sql.MysqlStorage;
import ru.rizonchik.refontsocial.storage.sql.SqliteStorage;
import ru.rizonchik.refontsocial.storage.yaml.YamlStorage;
import ru.rizonchik.refontsocial.util.LibraryManager;
import ru.rizonchik.refontsocial.util.YamlUtil;

import java.util.Locale;

public final class RefontSocial extends JavaPlugin {

    private Storage storage;
    private ReputationService reputationService;
    private GuiService guiService;
    private InteractionTracker interactionTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        YamlUtil.saveResourceIfNotExists(this, "messages.yml");
        YamlUtil.saveResourceIfNotExists(this, "gui.yml");

        reloadPlugin();

        ReputationCommand cmd = new ReputationCommand(this);
        if (getCommand("reputation") != null) {
            getCommand("reputation").setExecutor(cmd);
            getCommand("reputation").setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(guiService, this);
        getServer().getPluginManager().registerEvents(new SeenListener(this), this);

        if (interactionTracker != null) {
            getServer().getPluginManager().registerEvents(interactionTracker, this);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ReputationExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        } else {
            getLogger().info("PlaceholderAPI not found (softdepend). Placeholders disabled.");
        }

        getLogger().info("Enabled.");
    }

    @Override
    public void onDisable() {
        if (interactionTracker != null) {
            interactionTracker.shutdown();
        }
        if (guiService != null) {
            guiService.shutdown();
        }
        if (reputationService != null) {
            reputationService.shutdown();
        }
        if (storage != null) {
            storage.close();
        }
        getLogger().info("Disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();

        String storageTypeStr = getConfig().getString("storage.type", "SQLITE")
                .toUpperCase(Locale.ROOT);

        LibraryManager libs = new LibraryManager(this);

        if (storageTypeStr.equals("SQLITE")) {
            boolean enabled = getConfig().getBoolean("libraries.sqlite.enabled", true);
            if (enabled) {
                String gid = getConfig().getString("libraries.sqlite.groupId", "org.xerial");
                String aid = getConfig().getString("libraries.sqlite.artifactId", "sqlite-jdbc");
                String ver = getConfig().getString("libraries.sqlite.version", "3.46.0.0");
                String path = gid.replace('.', '/') + "/" + aid + "/" + ver + "/" + aid + "-" + ver + ".jar";
                libs.ensureDriverPresent("org.sqlite.JDBC", path, aid + "-" + ver + ".jar");
            }
        }

        if (storageTypeStr.equals("MYSQL")) {
            boolean enabled = getConfig().getBoolean("libraries.mysql.enabled", true);
            if (enabled) {
                String gid = getConfig().getString("libraries.mysql.groupId", "com.mysql");
                String aid = getConfig().getString("libraries.mysql.artifactId", "mysql-connector-j");
                String ver = getConfig().getString("libraries.mysql.version", "8.0.33");
                String path = gid.replace('.', '/') + "/" + aid + "/" + ver + "/" + aid + "-" + ver + ".jar";
                libs.ensureDriverPresent("com.mysql.cj.jdbc.Driver", path, aid + "-" + ver + ".jar");
            }
        }

        YamlUtil.reloadMessages(this);
        YamlUtil.reloadGui(this);

        if (interactionTracker != null) {
            interactionTracker.shutdown();
            interactionTracker = null;
        }
        if (guiService != null) {
            guiService.shutdown();
        }
        if (reputationService != null) {
            reputationService.shutdown();
        }
        if (storage != null) {
            storage.close();
        }

        StorageType storageType;
        try {
            storageType = StorageType.valueOf(storageTypeStr);
        } catch (Exception e) {
            storageType = StorageType.SQLITE;
        }

        if (storageType == StorageType.MYSQL) {
            storage = new MysqlStorage(this);
        } else if (storageType == StorageType.YAML) {
            storage = new YamlStorage(this);
        } else {
            storage = new SqliteStorage(this);
        }

        storage.init();

        reputationService = new ReputationService(this, storage);
        guiService = new GuiService(this, reputationService);

        boolean requireInteraction = getConfig().getBoolean("antiAbuse.requireInteraction.enabled", true);
        if (requireInteraction) {
            interactionTracker = new InteractionTracker(this);
            interactionTracker.start();
            reputationService.setInteractionTracker(interactionTracker);
        } else {
            reputationService.setInteractionTracker(null);
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public ReputationService getReputationService() {
        return reputationService;
    }

    public GuiService getGuiService() {
        return guiService;
    }
}