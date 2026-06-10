package org.showen.livecoverageplugin.coverage.service;

import com.intellij.openapi.diagnostic.Logger;
import org.showen.livecoverageplugin.constants.CoverageConstants;
import org.jacoco.core.tools.ExecDumpClient;
import org.jacoco.core.tools.ExecFileLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service responsible for communicating with JaCoCo agent via TCP.
 * Handles connection, data dumping, and thread safety.
 */
public final class CoverageConnectionService {
    
    private static final Logger LOG = Logger.getInstance(CoverageConnectionService.class);
    private final ReentrantLock dumpLock = new ReentrantLock();

    /**
     * Dumps coverage data from JaCoCo agent.
     * This method is thread-safe and uses a lock to prevent concurrent access.
     * 
     * @param address TCP server address
     * @param port TCP server port
     * @param reset Whether to reset the agent after dumping
     * @return ExecFileLoader containing coverage data, or null if connection failed
     */
    @Nullable
    public ExecFileLoader dumpCoverageData(@NotNull String address, int port, boolean reset) {
        // Use tryLock with timeout to prevent deadlocks
        boolean acquired = false;
        try {
            // Try to acquire lock with timeout (5 seconds)
            acquired = dumpLock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!acquired) {
                LOG.warn("Timeout waiting for dump lock - another operation may be in progress");
                return null;
            }
            
            ExecDumpClient client = new ExecDumpClient();
            client.setReset(reset);
            client.setDump(true);
            
            LOG.debug("Dumping coverage data from " + address + ":" + port + " (reset=" + reset + ")");
            ExecFileLoader loader = client.dump(address, port);
            
            if (loader != null) {
                LOG.debug("Successfully dumped coverage data from " + address + ":" + port);
            }
            
            return loader;
            
        } catch (IOException e) {
            LOG.warn(String.format(CoverageConstants.MSG_CONNECTION_FAILED, address, port), e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for dump lock", e);
            return null;
        } catch (Exception e) {
            LOG.error("Unexpected error dumping coverage data", e);
            return null;
        } finally {
            if (acquired) {
                dumpLock.unlock();
            }
        }
    }

    /**
     * Saves coverage data to a file.
     * @param loader The ExecFileLoader containing coverage data
     * @param destFile The destination file
     * @throws IOException If saving fails
     */
    public void saveCoverageData(@NotNull ExecFileLoader loader, @NotNull File destFile) throws IOException {
        loader.save(destFile, false);
    }
}
