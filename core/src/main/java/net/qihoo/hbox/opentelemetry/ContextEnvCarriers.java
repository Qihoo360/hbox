package net.qihoo.hbox.opentelemetry;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.qihoo.hbox.common.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/// The getters and setters to implement environment context propagation carriers.
// spec:
// https://github.com/open-telemetry/opentelemetry-specification/blob/v1.44.0/specification/context/env-carriers.md
// environment name and value spec:
// * W3C Trace Context
// * W3C Baggage
// * when extracting and injecting, the read and written environment names are in UPPER case.

public class ContextEnvCarriers {

    private static final Log LOG = LogFactory.getLog(ContextEnvCarriers.class);

    public static String normalizeKeyAsEnvName(final String key) {
        return key.toUpperCase(Locale.ROOT).replace(".", "_");
    }

    public static final EnvGetter GETTTER = new EnvGetter();
    public static final EnvSetter SETTTER = new EnvSetter();
    public static final EnvListSetter LIST_SETTTER = new EnvListSetter();

    public static class EnvGetter implements TextMapGetter<Map<String, String>> {
        @Override
        public Iterable<String> keys(@Nullable Map<String, String> carrier) {
            if (null == carrier) {
                carrier = System.getenv();
            }
            return null == carrier ? Collections.emptyList() : carrier.keySet();
        }

        @Nullable @Override
        public String get(@Nullable Map<String, String> carrier, final String key) {
            if (null == carrier) {
                carrier = System.getenv();
            }
            return null == carrier ? null : carrier.get(normalizeKeyAsEnvName(key));
        }
    }

    public static class EnvSetter implements TextMapSetter<Map<String, String>> {
        @Override
        public void set(@Nullable final Map<String, String> carrier, final String key, final String value) {
            if (null == carrier) {
                return;
            }
            carrier.put(normalizeKeyAsEnvName(key), value);
        }
    }

    public static class EnvListSetter implements TextMapSetter<List<String>> {
        @Override
        public void set(@Nullable final List<String> carrier, final String key, final String value) {
            if (null == carrier) {
                return;
            }
            carrier.add(String.format("%s=%s", normalizeKeyAsEnvName(key), value));
        }
    }
}
