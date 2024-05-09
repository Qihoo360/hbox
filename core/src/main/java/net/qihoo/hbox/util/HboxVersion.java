package net.qihoo.hbox.util;

import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class HboxVersion {
    private static Log LOG = LogFactory.getLog(HboxVersion.class);
    public static String VERSION;
    public static String GIT_BRANCH;
    public static String GIT_REV;

    private static Attributes getJarAttributes() {
        final URL manifestPath = HboxVersion.class.getResource("/META-INF/MANIFEST.MF");
        if (manifestPath == null){
            LOG.warn("/META-INF/MANIFEST.MF does not exist!");
        } else {
            try {
                final Manifest manifest = new Manifest(manifestPath.openStream());
                return manifest.getMainAttributes();
            } catch (final Throwable e) {
                LOG.warn("parser manifest error:", e);
            }
        }
        return null;
    }

    static {
        final Attributes attributes = getJarAttributes();
        if (null != attributes) {
            final String implVerManifest = attributes.getValue("Implementation-Version");
            final String branchManifest = attributes.getValue("X-Git-Branch");
            final String revManifest = attributes.getValue("X-Git-Revision");
            VERSION = implVerManifest == null || implVerManifest.isEmpty() ? "unknown" : implVerManifest;
            GIT_BRANCH = branchManifest == null ? "" : branchManifest;
            GIT_REV = revManifest == null ? "" : revManifest;
        } else {
            VERSION = "unknown";
            GIT_BRANCH = "";
            GIT_REV = "";
        }
    }
}
