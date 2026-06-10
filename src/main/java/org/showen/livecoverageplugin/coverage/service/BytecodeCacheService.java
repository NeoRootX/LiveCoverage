package org.showen.livecoverageplugin.coverage.service;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.showen.livecoverageplugin.constants.CoverageConstants.*;

/**
 * Service responsible for caching compiled class bytecode.
 * Monitors class file changes and maintains an in-memory cache.
 */
public final class BytecodeCacheService {
    
    private static final Logger LOG = Logger.getInstance(BytecodeCacheService.class);
    
    private final Map<String, byte[]> classBytecodeCache = new ConcurrentHashMap<>();
    // Use volatile to ensure visibility across threads
    private volatile long lastClassesModificationTime = -1;

    /**
     * Ensures the bytecode cache is up to date by checking for file changes.
     * @param classesDir Directory containing compiled class files
     * @return true if cache was updated, false otherwise
     */
    public boolean refreshCacheIfNeeded(@Nullable File classesDir) {
        if (classesDir == null || !classesDir.exists()) {
            return false;
        }

        try {
            long lastMod = getLastModifiedTimeRecursively(classesDir.toPath());
            long currentTime = lastClassesModificationTime;
            
            // Use volatile read and compare-and-set pattern for thread safety
            if (lastMod > currentTime) {
                synchronized (this) {
                    // Double-check after acquiring lock
                    if (lastMod > lastClassesModificationTime) {
                        LOG.info(MSG_CLASS_FILES_CHANGED);
                        rebuildCache(classesDir);
                        lastClassesModificationTime = System.currentTimeMillis();
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("Error updating bytecode cache", e);
        } catch (Exception e) {
            LOG.error("Unexpected error refreshing bytecode cache", e);
        }

        return false;
    }

    /**
     * Rebuilds the entire bytecode cache from the classes directory.
     */
    private void rebuildCache(@NotNull File classesDir) {
        classBytecodeCache.clear();
        
        try (Stream<Path> stream = Files.walk(classesDir.toPath())) {
            stream
                .filter(p -> p.toString().endsWith(CLASS_EXTENSION))
                .forEach(p -> {
                    try {
                        byte[] bytes = Files.readAllBytes(p);
                        String className = extractClassName(classesDir.toPath(), p);
                        classBytecodeCache.put(className, bytes);
                    } catch (IOException e) {
                        LOG.warn("Failed reading .class file: " + p, e);
                    }
                });
        } catch (IOException e) {
            LOG.warn("Error rebuilding bytecode cache", e);
        }
    }

    /**
     * Extracts the Java class name from a class file path.
     */
    @NotNull
    private String extractClassName(@NotNull Path classesDir, @NotNull Path classFile) {
        String relative = classesDir.relativize(classFile).toString().replace(File.separatorChar, '.');
        return relative.substring(0, relative.length() - CLASS_EXTENSION.length());
    }

    /**
     * Gets the most recent modification time of any file in the directory tree.
     */
    private long getLastModifiedTimeRecursively(@NotNull Path dir) throws IOException {
        final long[] max = {0};
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.forEach(p -> {
                try {
                    long lm = Files.getLastModifiedTime(p).toMillis();
                    if (lm > max[0]) {
                        max[0] = lm;
                    }
                } catch (IOException ignored) {
                    // Ignore individual file errors
                }
            });
        }
        return max[0];
    }

    /**
     * Gets the bytecode cache.
     * @return Unmodifiable view of the cache (for read-only access)
     */
    @NotNull
    public Map<String, byte[]> getCache() {
        return Map.copyOf(classBytecodeCache);
    }

    /**
     * Clears the bytecode cache.
     * Thread-safe operation.
     */
    public void clear() {
        synchronized (this) {
            classBytecodeCache.clear();
            lastClassesModificationTime = -1;
        }
    }

    /**
     * Gets the current cache size.
     */
    public int getCacheSize() {
        return classBytecodeCache.size();
    }
}
