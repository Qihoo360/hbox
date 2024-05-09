package net.qihoo.hbox.util;

import java.net.URL;
import java.util.Enumeration;
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
        try {
            for (final Enumeration<URL> urls = HboxVersion.class.getClassLoader().getResources("META-INF/MANIFEST.MF"); urls.hasMoreElements();) {
                final URL url = urls.nextElement();
                if (null == url) {
                    continue;
                }
                try {
                    final Manifest manifest = new Manifest(url.openStream());
                    final String title = manifest.getMainAttributes().getValue("Implementation-Title");
                    if ("HBox Yarn Application".equals(title)) {
                        return manifest.getMainAttributes();
                    }
                } catch (final Throwable e) {
                    LOG.warn("parser manifest error:", e);
                }
            }
        } catch (final Throwable e) {
            LOG.warn("failed to search /META-INF/MANIFEST.MF", e);
        }
        LOG.warn("/META-INF/MANIFEST.MF does not exist!");
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
