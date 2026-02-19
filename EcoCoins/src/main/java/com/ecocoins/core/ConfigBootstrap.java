package com.ecocoins.core;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * Exporta configs (Coins/, Languages/) y assets (Items/, Common/) desde el JAR a disco,
 * para que el usuario los edite fuera del .jar.
 *
 * Regla "anti-bug" (auto-recuperación) + "no overwrite":
 * - Siempre intenta exportar, pero SOLO copia archivos que NO existan en disco.
 * - Si el primer export quedó incompleto o con carpetas vacías, se repara solo.
 * - Si el usuario ya editó archivos, NO se pisan.
 *
 * IMPORTANTE: Exportar JAMÁS debe tumbar el plugin.
 * Si falla, se loggea WARNING y el plugin sigue (para que /change se registre).
 */
public final class ConfigBootstrap {

    // Dentro del JAR: Items/, Common/ en la raíz
    private static final String ASSETPACK_PREFIX = "";

    // Dentro del JAR: EcoCoins/Coins/, EcoCoins/Languages/
    private static final String ASSETPACK_PLUGIN_PREFIX = "EcoCoins/";

    private final HytaleLogger logger;
    private final Path dataDir;

    // dataDir/EcoCoins/
    private final Path externalPluginFolder;

    // dataDir/ExportedAssetPack/
    private final Path externalAssetPackFolder;

    public ConfigBootstrap(HytaleLogger logger, Path dataDir) {
        this.logger = logger;
        this.dataDir = dataDir;

        this.externalPluginFolder = dataDir.resolve("EcoCoins");
        this.externalAssetPackFolder = dataDir.resolve("ExportedAssetPack");
    }

    public Path getExternalFolder() {
        return externalPluginFolder;
    }

    public Path getExternalAssetPackFolder() {
        return externalAssetPackFolder;
    }

    /** Exporta configs editables del plugin (Coins/, Languages/) desde el JAR, sin pisar. */
    public void ensureEditableExternalFolder() {
        try {
            Files.createDirectories(externalPluginFolder);
            copyFromJarNoOverwriteWithFallback(ASSETPACK_PLUGIN_PREFIX + "Coins/", externalPluginFolder.resolve("Coins"));
            copyFromJarNoOverwriteWithFallback(ASSETPACK_PLUGIN_PREFIX + "Languages/", externalPluginFolder.resolve("Languages"));
            copyFromJarNoOverwriteWithFallback(ASSETPACK_PLUGIN_PREFIX + "Sounds/", externalPluginFolder.resolve("Sounds"));
            copyFromJarNoOverwriteWithFallback("Server/Audio/SoundEvents/", externalPluginFolder.resolve("Server/Audio/SoundEvents"));
        } catch (Exception e) {
            logger.at(Level.WARNING).log(
                    "[EcoCoins] No pude exportar configs (Coins/Languages/Sounds + Server/Audio/SoundEvents). Continúo igual. Causa: %s",
                    e.toString()
            );
        }
    }

 // Exporta solo las carpetas necesarias, no Items/ y Common/
    public void ensureEditableExportedAssetPack() {
        try {
            // Crear la carpeta para los assets exportados si no existe
            Files.createDirectories(externalAssetPackFolder);

            // Exportar carpetas editables de EcoCoins
            copyFromJarNoOverwriteWithFallback(ASSETPACK_PLUGIN_PREFIX + "Coins/", externalAssetPackFolder.resolve("EcoCoins/Coins"));
            copyFromJarNoOverwriteWithFallback(ASSETPACK_PLUGIN_PREFIX + "Languages/", externalAssetPackFolder.resolve("EcoCoins/Languages"));
            copyFromJarNoOverwriteWithFallback(ASSETPACK_PLUGIN_PREFIX + "Sounds/", externalAssetPackFolder.resolve("EcoCoins/Sounds"));
            copyFromJarNoOverwriteWithFallback("Server/Audio/SoundEvents/", externalAssetPackFolder.resolve("Server/Audio/SoundEvents"));

        } catch (Exception e) {
            logger.at(Level.WARNING).log(
                    "[EcoCoins] No pude exportar assetpack (EcoCoins/Coins/Languages/Sounds + Server/Audio/SoundEvents). Continúo igual. Causa: %s",
                    e.toString()
            );
        }
    }    


    private void copyFromJarNoOverwriteWithFallback(String jarPrefix, Path targetDir) throws Exception {
        int copied = copyFromJarNoOverwrite(jarPrefix, targetDir);
        if (copied > 0 || jarPrefix.startsWith("assetpack/")) return;

        String fallbackPrefix = "assetpack/" + jarPrefix;
        copyFromJarNoOverwrite(fallbackPrefix, targetDir);
    }

    private int copyFromJarNoOverwrite(String jarPrefix, Path targetDir) throws Exception {
        Files.createDirectories(targetDir);

        Path jarPath = locateOwnJar();
        if (jarPath == null) {
            logger.at(Level.WARNING).log(
                    "[EcoCoins] No pude ubicar el JAR del plugin; no exportaré: %s",
                    jarPrefix
            );
            return 0;
        }

        int matched = 0;
        int copied = 0;

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                if (entry.isDirectory()) continue;

                String name = entry.getName();
                if (!name.startsWith(jarPrefix)) continue;

                matched++;

                String relative = name.substring(jarPrefix.length());
                if (relative.isEmpty()) continue;

                Path out = targetDir.resolve(relative);

                // ✅ NO OVERWRITE
                if (Files.exists(out)) continue;

                try {
                    Path parent = out.getParent();
                    if (parent != null) Files.createDirectories(parent);

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }

                    copied++;
                } catch (Exception e) {
                    logger.at(Level.WARNING).log(
                            "[EcoCoins] No pude copiar %s -> %s (%s)",
                            name, out, e.toString()
                    );
                }
            }
        }

        if (matched == 0) {
            logger.at(Level.WARNING).log(
                    "[EcoCoins] No encontré entradas en el JAR para el prefijo: %s (¿path mal escrito?)",
                    jarPrefix
            );
            return 0;
        }

        // Log útil: muestra si realmente copió algo o si ya estaba todo
        if (copied > 0) {
            logger.at(Level.INFO).log(
                    "[EcoCoins] Exportado: %s -> %s (copiados=%d, ya_existían=%d)",
                    jarPrefix, targetDir, copied, (matched - copied)
            );
        } else {
            logger.at(Level.INFO).log(
                    "[EcoCoins] Export OK (sin cambios): %s -> %s (todo ya existía)",
                    jarPrefix, targetDir
            );
        }

        return copied;
    }

    /**
     * Intenta ubicar el jar real del plugin.
     * Debe ser robusto: nunca debe tumbar el plugin si falla.
     */
    private Path locateOwnJar() {
        try {
            var url = ConfigBootstrap.class.getProtectionDomain().getCodeSource().getLocation();
            if (url == null) return null;

            Path p = Path.of(url.toURI());
            if (Files.isRegularFile(p) && p.toString().endsWith(".jar")) return p;
        } catch (Exception ignored) {
            // ignore
        }

        // Fallback: buscar en la carpeta de plugins (best-effort)
        try {
            Path pluginsDir = dataDir.getParent();
            if (pluginsDir == null) return null;

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(pluginsDir, "*EcoCoins*.jar")) {
                for (Path p : ds) return p;
            }
        } catch (Exception ignored) {
            // ignore
        }

        return null;
    }
}