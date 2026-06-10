package org.showen.livecoverageplugin.license;

import org.jetbrains.annotations.NotNull;

/**
 * Central gate for paid-plugin functionality.
 */
public final class LicenseSupport {

    private static final String LICENSE_REQUIRED_MESSAGE =
            "Live Coverage requires a valid license or active 30-day trial.";

    private LicenseSupport() {
    }

    /**
     * @return {@code true} when the feature may run.
     *         Returns {@code true} while LicensingFacade is not initialized yet.
     */
    public static boolean isFeatureAllowed() {
        Boolean licensed = CheckLicense.isLicensed();
        return licensed == null || licensed;
    }

    /**
     * Blocks unlicensed usage and opens the registration dialog when needed.
     */
    public static boolean requireLicense() {
        return requireLicense(LICENSE_REQUIRED_MESSAGE);
    }

    public static boolean requireLicense(@NotNull String message) {
        Boolean licensed = CheckLicense.isLicensed();
        if (licensed == null || licensed) {
            return true;
        }
        CheckLicense.requestLicense(message);
        return false;
    }
}
