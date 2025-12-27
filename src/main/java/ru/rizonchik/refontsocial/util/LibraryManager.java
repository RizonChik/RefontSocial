package ru.rizonchik.refontsocial.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;

public final class LibraryManager {

    private final JavaPlugin plugin;

    public LibraryManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void ensureDriverPresent(String driverClass, String mavenPath, String fileName) {
        if (!plugin.getConfig().getBoolean("libraries.enabled", true)) return;

        if (isClassPresent(driverClass)) {
            plugin.getLogger().info("Driver found: " + driverClass);
            return;
        }

        File libsDir = new File(plugin.getDataFolder(), plugin.getConfig().getString("libraries.folder", "libs"));
        if (!libsDir.exists() && !libsDir.mkdirs()) {
            throw new RuntimeException("Cannot create libs folder: " + libsDir.getAbsolutePath());
        }

        File jar = new File(libsDir, fileName);
        if (!jar.exists()) {
            downloadFromRepos(mavenPath, jar);
        }

        addJarToClasspath(jar);

        if (!isClassPresent(driverClass)) {
            throw new RuntimeException("Driver still not found after loading jar: " + driverClass);
        }

        plugin.getLogger().info("Loaded driver: " + driverClass);
    }

    private void downloadFromRepos(String mavenPath, File target) {
        List<String> repos = plugin.getConfig().getStringList("libraries.repositories");
        if (repos == null || repos.isEmpty()) {
            throw new RuntimeException("No repositories configured in libraries.repositories");
        }

        IOException last = null;

        for (String repo : repos) {
            if (repo == null || repo.trim().isEmpty()) continue;
            if (!repo.endsWith("/")) repo = repo + "/";

            String url = repo + mavenPath;
            plugin.getLogger().info("Downloading: " + url);

            try {
                download(url, target);
                plugin.getLogger().info("Downloaded to: " + target.getAbsolutePath() + " (" + target.length() + " bytes)");
                return;
            } catch (IOException e) {
                last = e;
                plugin.getLogger().warning("Failed: " + url + " -> " + e.getMessage());
            }
        }

        throw new RuntimeException("Failed to download dependency: " + mavenPath, last);
    }

    private void download(String url, File target) throws IOException {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        if (tmp.exists()) tmp.delete();

        URL u = new URL(url);
        try (InputStream in = u.openStream();
             FileOutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
        }

        if (target.exists() && !target.delete()) {
            throw new IOException("Cannot replace existing jar: " + target.getAbsolutePath());
        }
        if (!tmp.renameTo(target)) {
            throw new IOException("Cannot move temp file to target: " + target.getAbsolutePath());
        }
    }

    private boolean isClassPresent(String name) {
        try {
            Class.forName(name, false, plugin.getClass().getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void addJarToClasspath(File jar) {
        try {
            ClassLoader cl = plugin.getClass().getClassLoader();

            if (cl instanceof URLClassLoader) {
                URLClassLoader urlCl = (URLClassLoader) cl;
                Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrl.setAccessible(true);
                addUrl.invoke(urlCl, jar.toURI().toURL());
                return;
            }

            Method addUrl = cl.getClass().getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(cl, jar.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Failed to add jar to classpath: " + jar.getAbsolutePath(), e);
        }
    }
}