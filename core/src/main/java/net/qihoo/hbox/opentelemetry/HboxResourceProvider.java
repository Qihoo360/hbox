package net.qihoo.hbox.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.internal.ConfigUtil;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.qihoo.hbox.AM.ApplicationMaster;
import net.qihoo.hbox.api.HboxConstants;
import net.qihoo.hbox.client.Client;
import net.qihoo.hbox.conf.HboxConfiguration;
import net.qihoo.hbox.util.HboxVersion;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ContainerId;

public final class HboxResourceProvider implements ResourceProvider {

    private static final Log LOG = LogFactory.getLog(HboxResourceProvider.class);
    private static final AttributeKey<List<String>> PROCESS_COMMAND_ARGS =
            AttributeKey.stringArrayKey("process.command_args");
    private static final AttributeKey<String> PROCESS_EXE_PATH = AttributeKey.stringKey("process.executable.path");
    private static final AttributeKey<String> PROCESS_OWNER = AttributeKey.stringKey("process.owner");
    private static final AttributeKey<String> RUNTIME_NAME = AttributeKey.stringKey("process.runtime.name");
    private static final AttributeKey<String> RUNTIME_VERSION = AttributeKey.stringKey("process.runtime.version");
    private static final AttributeKey<String> RUNTIME_DESC = AttributeKey.stringKey("process.runtime.description");
    private static final AttributeKey<String> SERVICE_VERS = AttributeKey.stringKey("service.version");
    private static final AttributeKey<String> HOST_NAME = AttributeKey.stringKey("host.name");
    private static final AttributeKey<String> HOST_ARCH = AttributeKey.stringKey("host.arch");
    private static final AttributeKey<String> OS_TYPE = AttributeKey.stringKey("os.type");
    private static final AttributeKey<String> YARN_CONTAINER_ID = AttributeKey.stringKey("yarn.container.id");
    private static final AttributeKey<String> YARN_APP_ATTEMP = AttributeKey.stringKey("yarn.application.attemp");
    private static final AttributeKey<String> YARN_APP_ID = AttributeKey.stringKey("yarn.application.id");
    private static final AttributeKey<String> HBOX_GIT_BRANCH = AttributeKey.stringKey("hbox.git.branch");
    private static final AttributeKey<String> HBOX_GIT_REV = AttributeKey.stringKey("hbox.git.rev");
    private static final AttributeKey<String> HBOX_CONTAINER_ROLE = AttributeKey.stringKey("hbox.container.role");
    private static final AttributeKey<Long> HBOX_CONTAINER_INDEX = AttributeKey.longKey("hbox.container.index");
    private static final AttributeKey<String> HBOX_HOME = AttributeKey.stringKey("hbox.home");
    private static final AttributeKey<String> HBOX_CONF = AttributeKey.stringKey("hbox.conf");

    private static final String ATTRIBUTE_PROPERTY = "otel.resource.attributes";
    private static final String SERVICE_NAME_PROPERTY = "otel.service.name";

    private static String[] mainArgs = null;
    private static HboxConfiguration conf = new HboxConfiguration();
    private static Class mainClazz = null;

    public static void init(
            @Nullable final String[] initArgs,
            @Nullable final HboxConfiguration initConf,
            @Nullable final Class initMainClazz) {
        mainArgs = initArgs;
        conf = null == initConf ? new HboxConfiguration() : initConf;
        mainClazz = initMainClazz;
    }

    @Override
    public Resource createResource(final ConfigProperties config) {
        final AttributesBuilder attrBuilder = Attributes.builder();

        // command_args resource
        if (null != mainArgs) {
            final List<String> commandArgs = new ArrayList<>(2 + mainArgs.length);
            int extraArgsCount = HboxConfiguration.DEFAULT_HBOX_INTERNAL_EXTRA_ARGS_COUNT;

            if (Client.class.equals(mainClazz)) {
                final String sdkResourceValue = System.getProperty(
                        ConfigUtil.normalizePropertyKey(ATTRIBUTE_PROPERTY),
                        System.getenv(ContextEnvCarriers.normalizeKeyAsEnvName(ATTRIBUTE_PROPERTY)));
                final String command = Arrays.stream((null == sdkResourceValue ? "" : sdkResourceValue).split(","))
                        .filter(s -> s.startsWith("process.command=") && s.split("=").length == 2)
                        .map(s -> s.split("=")[1])
                        .reduce("hbox-submit", (first, second) -> second);

                try {
                    extraArgsCount = Math.min(
                            mainArgs.length,
                            null != conf
                                    ? conf.getInt(HboxConfiguration.HBOX_INTERNAL_EXTRA_ARGS_COUNT, extraArgsCount)
                                    : extraArgsCount);
                } catch (final NumberFormatException e) {
                    LOG.error("Not an integer for conf " + HboxConfiguration.HBOX_INTERNAL_EXTRA_ARGS_COUNT, e);
                }

                commandArgs.add(command);
            } else {
                final String javaHome = System.getProperty("java.home");
                if (null == javaHome || "".equals(javaHome)) {
                    commandArgs.add("java");
                } else {
                    final String javaPath = Paths.get(javaHome, "bin", "java").toString();
                    attrBuilder.put(PROCESS_EXE_PATH, javaPath);
                    commandArgs.add(javaPath);
                }
                commandArgs.add(null != mainClazz ? mainClazz.getName() : "unknownClass");
            }

            for (int i = extraArgsCount; i < mainArgs.length; ++i) {
                commandArgs.add(mainArgs[i]);
            }

            attrBuilder.put(PROCESS_COMMAND_ARGS, commandArgs);
        }

        // owner resource
        attrBuilder.put(PROCESS_OWNER, System.getProperty("user.name"));

        // runtime resource
        attrBuilder.put(RUNTIME_NAME, System.getProperty("java.runtime.name"));
        attrBuilder.put(RUNTIME_VERSION, System.getProperty("java.runtime.version"));
        attrBuilder.put(
                RUNTIME_DESC,
                System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.name") + " "
                        + System.getProperty("java.vm.version"));

        // service resource
        attrBuilder.put(SERVICE_VERS, HboxVersion.VERSION);

        // host and os resource
        try {
            attrBuilder.put(HOST_NAME, InetAddress.getLocalHost().getHostName());
        } catch (final UnknownHostException e) {
            LOG.warn("ignore host name resource", e);
        }
        attrBuilder.put(HOST_ARCH, normalizeArch(System.getProperty("os.arch")));
        attrBuilder.put(OS_TYPE, normalizeOs(System.getProperty("os.name")));

        // hbox version resource
        attrBuilder.put(HBOX_GIT_BRANCH, HboxVersion.GIT_BRANCH);
        attrBuilder.put(HBOX_GIT_REV, HboxVersion.GIT_REV);

        // hbox runtime resource
        final String hboxRole = System.getenv(HboxConstants.Environment.HBOX_TF_ROLE.toString());
        final String hboxIndex = System.getenv(HboxConstants.Environment.HBOX_TF_INDEX.toString());
        final String hboxHome = System.getenv("HBOX_HOME");
        final String hboxConf = System.getenv("HBOX_CONF_DIR");
        if (null != hboxRole && !hboxRole.isEmpty()) {
            attrBuilder.put(HBOX_CONTAINER_ROLE, hboxRole);
        }
        if (null != hboxIndex && !hboxIndex.isEmpty()) {
            try {
                attrBuilder.put(HBOX_CONTAINER_INDEX, Long.parseLong(hboxIndex));
            } catch (final NumberFormatException ignore) {
            }
        }
        if (null != hboxHome && !hboxHome.isEmpty()) {
            attrBuilder.put(HBOX_HOME, hboxHome);
        }
        if (null != hboxConf && !hboxConf.isEmpty()) {
            attrBuilder.put(HBOX_CONF, hboxConf);
        }

        // yarn resource
        final String containerIdString = System.getenv(ApplicationConstants.Environment.CONTAINER_ID.name());
        if (null != containerIdString && !containerIdString.isEmpty()) {
            final ContainerId containerId = ContainerId.fromString(containerIdString);
            attrBuilder.put(YARN_CONTAINER_ID, containerIdString);
            if (null != containerId) {
                final ApplicationAttemptId appAttemptId = containerId.getApplicationAttemptId();
                attrBuilder.put(YARN_APP_ATTEMP, appAttemptId.toString());
                if (null != appAttemptId) {
                    final ApplicationId appId = appAttemptId.getApplicationId();
                    if (null != appId) {
                        attrBuilder.put(YARN_APP_ID, appId.toString());
                    }
                    if (ApplicationMaster.class.equals(mainClazz)) {
                        attrBuilder.put(HBOX_CONTAINER_ROLE, "application-master");
                        attrBuilder.put(HBOX_CONTAINER_INDEX, (long) appAttemptId.getAttemptId());
                    }
                }
            }
        }

        return Resource.create(attrBuilder.build());
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }

    private static String normalize(@Nullable final String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    // normalize os.arch property for opentelemetry spec
    // ref:
    // https://opentelemetry.io/docs/specs/semconv/resource/host/
    // https://github.com/trustin/os-maven-plugin/blob/os-maven-plugin-1.7.1/src/main/java/kr/motd/maven/os/Detector.java#L192
    private static String normalizeArch(@Nullable String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "amd64";
        }
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86";
        }
        if (value.matches("^(ia64w?|itanium64)$")) {
            return "ia64";
        }
        if ("ia64n".equals(value)) {
            return "ia32";
        }
        if (value.matches("^(sparc|sparc32)$")) {
            return "sparc32";
        }
        if (value.matches("^(sparcv9|sparc64)$")) {
            return "sparc64";
        }
        if (value.matches("^(arm|arm32)$")) {
            return "arm32";
        }
        if ("aarch64".equals(value)) {
            return "arm64";
        }
        if (value.matches("^(mips|mipsel|mips32|mips32el)$")) {
            return "mips32";
        }
        if (value.matches("^(mips64|mips64el)$")) {
            return "mips64";
        }
        if (value.matches("^(ppc|ppcle|ppc32|ppc32le)$")) {
            return "ppc32";
        }
        if (value.matches("^(ppc64|ppc64le)$")) {
            return "ppc64";
        }
        if ("s390".equals(value)) {
            return "s390";
        }
        if ("s390x".equals(value)) {
            return "s390x";
        }
        if (value.matches("^(riscv|riscv32)$")) {
            return "riscv32";
        }
        if ("riscv64".equals(value)) {
            return "riscv64";
        }
        if ("e2k".equals(value)) {
            return "e2k";
        }
        if ("loongarch64".equals(value)) {
            return "loongarch64";
        }
        return "unknown-arch-" + value;
    }

    // normalize os.name property for opentelemetry spec
    // ref:
    // https://opentelemetry.io/docs/specs/semconv/resource/host/
    // https://github.com/trustin/os-maven-plugin/blob/os-maven-plugin-1.7.1/src/main/java/kr/motd/maven/os/Detector.java#L192
    private static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("linux")) {
            return "linux";
        }
        if (value.startsWith("mac") || value.startsWith("osx") || value.startsWith("darwin")) {
            return "darwin";
        }
        if (value.startsWith("windows")) {
            return "windows";
        }
        if (value.startsWith("aix")) {
            return "aix";
        }
        if (value.startsWith("hpux")) {
            return "hpux";
        }
        if (value.startsWith("os400")) {
            // Avoid the names such as os4000
            if (value.length() <= 5 || !Character.isDigit(value.charAt(5))) {
                return "os400";
            }
        }
        if (value.startsWith("freebsd")) {
            return "freebsd";
        }
        if (value.startsWith("openbsd")) {
            return "openbsd";
        }
        if (value.startsWith("netbsd")) {
            return "netbsd";
        }
        if (value.startsWith("dragonfly")) {
            return "flybsd";
        }
        if (value.startsWith("solaris") || value.startsWith("sunos")) {
            return "solaris";
        }
        if (value.startsWith("zos")) {
            return "z_os";
        }

        return "unknown-os";
    }

    public static String currentServiceNamespace() {
        final String sdkResourceValue = System.getProperty(
                ConfigUtil.normalizePropertyKey(ATTRIBUTE_PROPERTY),
                System.getenv(ContextEnvCarriers.normalizeKeyAsEnvName(ATTRIBUTE_PROPERTY)));
        return Arrays.stream((null == sdkResourceValue ? "" : sdkResourceValue).split(","))
                .filter(s -> s.startsWith("service.namespace=") && s.split("=").length == 2)
                .map(s -> s.split("=")[1])
                .reduce("net.qihoo", (first, second) -> second);
    }

    public static String serviceNamePropertyForContainer() {
        return String.format("-D%s=hbox", ConfigUtil.normalizePropertyKey(SERVICE_NAME_PROPERTY));
    }

    public static String resourcePropertyForContainer() {
        return String.format(
                "-D%s=service.namespace=%s,process.pid=$$,process.parent_pid=$PPID,process.executable.name=java,${OTEL_RESOURCE_ATTRIBUTES:+,$OTEL_RESOURCE_ATTRIBUTES}",
                ConfigUtil.normalizePropertyKey(ATTRIBUTE_PROPERTY), currentServiceNamespace());
    }
}
