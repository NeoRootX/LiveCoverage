package org.showen.livecoverageplugin.license;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.LicensingFacade;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JetBrains Marketplace license verification.
 * Based on the official example: marketplace-makemecoffee-plugin.
 */
public final class CheckLicense {

    /**
     * Must match {@code code} in plugin.xml {@code product-descriptor}.
     */
    private static final String PRODUCT_CODE = "PLIVECOVERAGE";
    private static final String KEY_PREFIX = "key:";
    private static final String STAMP_PREFIX = "stamp:";

    private static final DataKey<String> REGISTER_PRODUCT_CODE =
            DataKey.create("register.product-descriptor.code");
    private static final DataKey<String> REGISTER_MESSAGE =
            DataKey.create("register.message");

    private static final String[] ROOT_CERTIFICATES = new String[]{
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIIFOzCCAyOgAwIBAgIJANJssYOyg3nhMA0GCSqGSIb3DQEBCwUAMBgxFjAUBgNV\n" +
                    "BAMMDUpldFByb2ZpbGUgQ0EwHhcNMTUxMDAyMTEwMDU2WhcNNDUxMDI0MTEwMDU2\n" +
                    "WjAYMRYwFAYDVQQDDA1KZXRQcm9maWxlIENBMIICIjANBgkqhkiG9w0BAQEFAAOC\n" +
                    "Ag8AMIICCgKCAgEA0tQuEA8784NabB1+T2XBhpB+2P1qjewHiSajAV8dfIeWJOYG\n" +
                    "y+ShXiuedj8rL8VCdU+yH7Ux/6IvTcT3nwM/E/3rjJIgLnbZNerFm15Eez+XpWBl\n" +
                    "m5fDBJhEGhPc89Y31GpTzW0vCLmhJ44XwvYPntWxYISUrqeR3zoUQrCEp1C6mXNX\n" +
                    "EpqIGIVbJ6JVa/YI+pwbfuP51o0ZtF2rzvgfPzKtkpYQ7m7KgA8g8ktRXyNrz8bo\n" +
                    "iwg7RRPeqs4uL/RK8d2KLpgLqcAB9WDpcEQzPWegbDrFO1F3z4UVNH6hrMfOLGVA\n" +
                    "xoiQhNFhZj6RumBXlPS0rmCOCkUkWrDr3l6Z3spUVgoeea+QdX682j6t7JnakaOw\n" +
                    "jzwY777SrZoi9mFFpLVhfb4haq4IWyKSHR3/0BlWXgcgI6w6LXm+V+ZgLVDON52F\n" +
                    "LcxnfftaBJz2yclEwBohq38rYEpb+28+JBvHJYqcZRaldHYLjjmb8XXvf2MyFeXr\n" +
                    "SopYkdzCvzmiEJAewrEbPUaTllogUQmnv7Rv9sZ9jfdJ/cEn8e7GSGjHIbnjV2ZM\n" +
                    "Q9vTpWjvsT/cqatbxzdBo/iEg5i9yohOC9aBfpIHPXFw+fEj7VLvktxZY6qThYXR\n" +
                    "Rus1WErPgxDzVpNp+4gXovAYOxsZak5oTV74ynv1aQ93HSndGkKUE/qA/JECAwEA\n" +
                    "AaOBhzCBhDAdBgNVHQ4EFgQUo562SGdCEjZBvW3gubSgUouX8bMwSAYDVR0jBEEw\n" +
                    "P4AUo562SGdCEjZBvW3gubSgUouX8bOhHKQaMBgxFjAUBgNVBAMMDUpldFByb2Zp\n" +
                    "bGUgQ0GCCQDSbLGDsoN54TAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIBBjANBgkq\n" +
                    "hkiG9w0BAQsFAAOCAgEAjrPAZ4xC7sNiSSqh69s3KJD3Ti4etaxcrSnD7r9rJYpK\n" +
                    "BMviCKZRKFbLv+iaF5JK5QWuWdlgA37ol7mLeoF7aIA9b60Ag2OpgRICRG79QY7o\n" +
                    "uLviF/yRMqm6yno7NYkGLd61e5Huu+BfT459MWG9RVkG/DY0sGfkyTHJS5xrjBV6\n" +
                    "hjLG0lf3orwqOlqSNRmhvn9sMzwAP3ILLM5VJC5jNF1zAk0jrqKz64vuA8PLJZlL\n" +
                    "S9TZJIYwdesCGfnN2AETvzf3qxLcGTF038zKOHUMnjZuFW1ba/12fDK5GJ4i5y+n\n" +
                    "fDWVZVUDYOPUixEZ1cwzmf9Tx3hR8tRjMWQmHixcNC8XEkVfztID5XeHtDeQ+uPk\n" +
                    "X+jTDXbRb+77BP6n41briXhm57AwUI3TqqJFvoiFyx5JvVWG3ZqlVaeU/U9e0gxn\n" +
                    "8qyR+ZA3BGbtUSDDs8LDnE67URzK+L+q0F2BC758lSPNB2qsJeQ63bYyzf0du3wB\n" +
                    "/gb2+xJijAvscU3KgNpkxfGklvJD/oDUIqZQAnNcHe7QEf8iG2WqaMJIyXZlW3me\n" +
                    "0rn+cgvxHPt6N4EBh5GgNZR4l0eaFEV+fxVsydOQYo1RIyFMXtafFBqQl6DDxujl\n" +
                    "FeU3FZ+Bcp12t7dlM4E0/sS1XdL47CfGVj4Bp+/VbF862HmkAbd7shs7sDQkHbU=\n" +
                    "-----END CERTIFICATE-----\n",
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIIFTDCCAzSgAwIBAgIJAMCrW9HV+hjZMA0GCSqGSIb3DQEBCwUAMB0xGzAZBgNV\n" +
                    "BAMMEkxpY2Vuc2UgU2VydmVycyBDQTAgFw0xNjEwMTIxNDMwNTRaGA8yMTE2MTIy\n" +
                    "NzE0MzA1NFowHTEbMBkGA1UEAwwSTGljZW5zZSBTZXJ2ZXJzIENBMIICIjANBgkq\n" +
                    "hkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAoT7LvHj3JKK2pgc5f02z+xEiJDcvlBi6\n" +
                    "fIwrg/504UaMx3xWXAE5CEPelFty+QPRJnTNnSxqKQQmg2s/5tMJpL9lzGwXaV7a\n" +
                    "rrcsEDbzV4el5mIXUnk77Bm/QVv48s63iQqUjVmvjQt9SWG2J7+h6X3ICRvF1sQB\n" +
                    "yeat/cO7tkpz1aXXbvbAws7/3dXLTgAZTAmBXWNEZHVUTcwSg2IziYxL8HRFOH0+\n" +
                    "GMBhHqa0ySmF1UTnTV4atIXrvjpABsoUvGxw+qOO2qnwe6ENEFWFz1a7pryVOHXg\n" +
                    "P+4JyPkI1hdAhAqT2kOKbTHvlXDMUaxAPlriOVw+vaIjIVlNHpBGhqTj1aqfJpLj\n" +
                    "qfDFcuqQSI4O1W5tVPRNFrjr74nDwLDZnOF+oSy4E1/WhL85FfP3IeQAIHdswNMJ\n" +
                    "y+RdkPZCfXzSUhBKRtiM+yjpIn5RBY+8z+9yeGocoxPf7l0or3YF4GUpud202zgy\n" +
                    "Y3sJqEsZksB750M0hx+vMMC9GD5nkzm9BykJS25hZOSsRNhX9InPWYYIi6mFm8QA\n" +
                    "2Dnv8wxAwt2tDNgqa0v/N8OxHglPcK/VO9kXrUBtwCIfZigO//N3hqzfRNbTv/ZO\n" +
                    "k9lArqGtcu1hSa78U4fuu7lIHi+u5rgXbB6HMVT3g5GQ1L9xxT1xad76k2EGEi3F\n" +
                    "9B+tSrvru70CAwEAAaOBjDCBiTAdBgNVHQ4EFgQUpsRiEz+uvh6TsQqurtwXMd4J\n" +
                    "8VEwTQYDVR0jBEYwRIAUpsRiEz+uvh6TsQqurtwXMd4J8VGhIaQfMB0xGzAZBgNV\n" +
                    "BAMMEkxpY2Vuc2UgU2VydmVycyBDQYIJAMCrW9HV+hjZMAwGA1UdEwQFMAMBAf8w\n" +
                    "CwYDVR0PBAQDAgEGMA0GCSqGSIb3DQEBCwUAA4ICAQCJ9+GQWvBS3zsgPB+1PCVc\n" +
                    "oG6FY87N6nb3ZgNTHrUMNYdo7FDeol2DSB4wh/6rsP9Z4FqVlpGkckB+QHCvqU+d\n" +
                    "rYPe6QWHIb1kE8ftTnwapj/ZaBtF80NWUfYBER/9c6To5moW63O7q6cmKgaGk6zv\n" +
                    "St2IhwNdTX0Q5cib9ytE4XROeVwPUn6RdU/+AVqSOspSMc1WQxkPVGRF7HPCoGhd\n" +
                    "vqebbYhpahiMWfClEuv1I37gJaRtsoNpx3f/jleoC/vDvXjAznfO497YTf/GgSM2\n" +
                    "LCnVtpPQQ2vQbOfTjaBYO2MpibQlYpbkbjkd5ZcO5U5PGrQpPFrWcylz7eUC3c05\n" +
                    "UVeygGIthsA/0hMCioYz4UjWTgi9NQLbhVkfmVQ5lCVxTotyBzoubh3FBz+wq2Qt\n" +
                    "iElsBrCMR7UwmIu79UYzmLGt3/gBdHxaImrT9SQ8uqzP5eit54LlGbvGekVdAL5l\n" +
                    "DFwPcSB1IKauXZvi1DwFGPeemcSAndy+Uoqw5XGRqE6jBxS7XVI7/4BSMDDRBz1u\n" +
                    "a+JMGZXS8yyYT+7HdsybfsZLvkVmc9zVSDI7/MjVPdk6h0sLn+vuPC1bIi5edoNy\n" +
                    "PdiG2uPH5eDO6INcisyPpLS4yFKliaO4Jjap7yzLU9pbItoWgCAYa2NpxuxHJ0tB\n" +
                    "7tlDFnvaRnQukqSG+VqNWg==\n" +
                    "-----END CERTIFICATE-----"
    };

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long TIMESTAMP_VALIDITY_PERIOD_MS = HOUR;

    private CheckLicense() {
    }

    /**
     * @return {@code true} if licensed, {@code false} if not.
     *         {@code null} means LicensingFacade is not initialized yet.
     */
    @Nullable
    public static Boolean isLicensed() {
        LicensingFacade facade = LicensingFacade.getInstance();
        if (facade == null) {
            return null;
        }
        String cstamp = facade.getConfirmationStamp(PRODUCT_CODE);
        if (cstamp == null) {
            return false;
        }
        if (cstamp.startsWith(KEY_PREFIX)) {
            return isKeyValid(cstamp.substring(KEY_PREFIX.length()));
        }
        if (cstamp.startsWith(STAMP_PREFIX)) {
            return isLicenseServerStampValid(cstamp.substring(STAMP_PREFIX.length()));
        }
        return false;
    }

    public static void requestLicense(@NotNull String message) {
        ApplicationManager.getApplication().invokeLater(
                () -> showRegisterDialog(PRODUCT_CODE, message),
                ModalityState.nonModal()
        );
    }

    private static void showRegisterDialog(@NotNull String productCode, @Nullable String message) {
        ActionManager actionManager = ActionManager.getInstance();
        AnAction registerAction = actionManager.getAction("RegisterPlugins");
        if (registerAction == null) {
            registerAction = actionManager.getAction("Register");
        }
        if (registerAction != null) {
            ActionUtil.performActionDumbAwareWithCallbacks(
                    registerAction,
                    AnActionEvent.createEvent(
                            asDataContext(productCode, message),
                            new Presentation(),
                            "",
                            ActionUiKind.NONE,
                            null
                    )
            );
        }
    }

    @NotNull
    private static DataContext asDataContext(@NotNull String productCode, @Nullable String message) {
        SimpleDataContext.Builder builder = SimpleDataContext.builder()
                .add(REGISTER_PRODUCT_CODE, productCode);
        if (message != null) {
            builder.add(REGISTER_MESSAGE, message);
        }
        return builder.build();
    }

    private static boolean isKeyValid(@NotNull String key) {
        String[] licenseParts = key.split("-");
        if (licenseParts.length != 4) {
            return false;
        }

        String licenseId = licenseParts[0];
        String licensePartBase64 = licenseParts[1];
        String signatureBase64 = licenseParts[2];
        String certBase64 = licenseParts[3];

        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(createCertificate(
                    Base64.getMimeDecoder().decode(certBase64.getBytes(StandardCharsets.UTF_8)),
                    Collections.emptySet(),
                    false
            ));
            byte[] licenseBytes = Base64.getMimeDecoder().decode(licensePartBase64.getBytes(StandardCharsets.UTF_8));
            sig.update(licenseBytes);
            if (!sig.verify(Base64.getMimeDecoder().decode(signatureBase64.getBytes(StandardCharsets.UTF_8)))) {
                return false;
            }
            String licenseData = new String(licenseBytes, StandardCharsets.UTF_8);
            return licenseData.contains("\"licenseId\":\"" + licenseId + "\"");
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isLicenseServerStampValid(@NotNull String serverStamp) {
        try {
            String[] parts = serverStamp.split(":");
            Base64.Decoder base64 = Base64.getMimeDecoder();

            String expectedMachineId = parts[0];
            long timeStamp = Long.parseLong(parts[1]);
            String machineId = parts[2];
            String signatureType = parts[3];
            byte[] signatureBytes = base64.decode(parts[4].getBytes(StandardCharsets.UTF_8));
            byte[] certBytes = base64.decode(parts[5].getBytes(StandardCharsets.UTF_8));
            Collection<byte[]> intermediate = new ArrayList<>();
            for (int idx = 6; idx < parts.length; idx++) {
                intermediate.add(base64.decode(parts[idx].getBytes(StandardCharsets.UTF_8)));
            }

            Signature sig = Signature.getInstance(signatureType);
            sig.initVerify(createCertificate(certBytes, intermediate, true));
            sig.update((timeStamp + ":" + machineId).getBytes(StandardCharsets.UTF_8));
            if (sig.verify(signatureBytes)) {
                return expectedMachineId.equals(machineId)
                        && Math.abs(System.currentTimeMillis() - timeStamp) < TIMESTAMP_VALIDITY_PERIOD_MS;
            }
        }
        catch (Throwable ignored) {
            // invalid stamp
        }
        return false;
    }

    @NotNull
    private static X509Certificate createCertificate(
            byte[] certBytes,
            Collection<byte[]> intermediateCertsBytes,
            boolean checkValidityAtCurrentDate
    ) throws Exception {
        CertificateFactory x509factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) x509factory.generateCertificate(new ByteArrayInputStream(certBytes));

        Collection<X509Certificate> allCerts = new HashSet<>();
        allCerts.add(cert);
        for (byte[] bytes : intermediateCertsBytes) {
            allCerts.add((X509Certificate) x509factory.generateCertificate(new ByteArrayInputStream(bytes)));
        }

        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(cert);
        Set<TrustAnchor> trustAnchors = new HashSet<>();
        for (String rc : ROOT_CERTIFICATES) {
            trustAnchors.add(new TrustAnchor(
                    (X509Certificate) x509factory.generateCertificate(
                            new ByteArrayInputStream(rc.getBytes(StandardCharsets.UTF_8))
                    ),
                    null
            ));
        }

        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);
        pkixParams.setRevocationEnabled(false);
        if (!checkValidityAtCurrentDate) {
            pkixParams.setDate(cert.getNotBefore());
        }
        pkixParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(allCerts)));

        PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) CertPathBuilder.getInstance("PKIX").build(pkixParams);
        CertPath path = result.getCertPath();
        if (path != null) {
            CertPathValidator.getInstance("PKIX").validate(path, pkixParams);
            return cert;
        }
        throw new Exception("Certificate used to sign the license is not signed by JetBrains root certificate");
    }
}
