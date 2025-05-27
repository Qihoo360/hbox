// Copyright 2017-2025 Qihoo Inc
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package net.qihoo.hbox.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.qihoo.hbox.common.*;
import net.qihoo.hbox.conf.HboxConfiguration;
import net.qihoo.hbox.util.HboxVersion;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/// Static entrypoint of OpenTelemetry like GlobalOpenTelemetry

public final class HboxOpenTelemetry {

    private static final Log LOG = LogFactory.getLog(OpenTelemetry.class);
    private static final String SCOPE_NAME = "net.qihoo.hbox";
    private static final String SCOPE_VERSION = HboxVersion.VERSION;
    private static boolean initialized = false;

    private static OpenTelemetry INSTANCE = OpenTelemetry.noop();
    private static Context CONTEXT = Context.current();

    public static void init(
            @Nullable final String[] initArgs,
            @Nullable final HboxConfiguration initConf,
            @Nullable final Class initMainClazz) {
        if (initialized) {
            LOG.warn(OpenTelemetry.class.getName() + " is already initialized!");
            return;
        }
        initialized = true;

        // init JUL
        try (InputStream configStream =
                HboxOpenTelemetry.class.getClassLoader().getResourceAsStream("logging.properties")) {
            if (configStream != null) {
                LogManager.getLogManager().readConfiguration(configStream);
                Logger.getLogger(HboxOpenTelemetry.class.getName()).fine("Loaded logging.properties from inside JAR.");
            } else {
                Logger.getLogger(HboxOpenTelemetry.class.getName())
                        .warning("logging.properties not found in classpath!");
            }
        } catch (final Exception e) {
            LOG.warn(e);
        }

        // setup resource provider before config OpenTelemetry
        HboxResourceProvider.init(initArgs, initConf, initMainClazz);

        // detect and config OpenTelemetry from javaagent or AutoConfigure sdk
        final OpenTelemetry globalOtel = GlobalOpenTelemetry.get();
        if (null != globalOtel) {
            INSTANCE = globalOtel;
        }

        // propagate the context from the outer
        // default otel.propagators is composited of W3CTraceContextPropagator and W3CBaggagePropagator
        CONTEXT = INSTANCE.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), null, ContextEnvCarriers.GETTTER);

        if (LOG.isDebugEnabled()) {
            LOG.debug("OpenTelemetry class = " + INSTANCE);
            LOG.debug("OpenTelemetry context = " + CONTEXT);
        }
    }

    public static void flush() {
        final TracerProvider trp = INSTANCE.getTracerProvider();
        if (trp instanceof SdkTracerProvider) {
            ((SdkTracerProvider) trp).forceFlush();
        }
    }

    public static Context rootContext() {
        return CONTEXT;
    }

    public static Tracer getTracer() {
        return INSTANCE.getTracer(SCOPE_NAME, SCOPE_VERSION);
    }

    // create a SpanBuilder with default tracer, then bind the root text as parent
    public static SpanBuilder spanBuilder(@Nullable final String spanName) {
        return getTracer().spanBuilder(spanName).setParent(CONTEXT);
    }

    // create and start a span
    public static Span startSpan(@Nullable final String spanName) {
        return spanBuilder(spanName).startSpan();
    }

    public static Span startSpan(@Nullable final String spanName, final UnaryOperator<SpanBuilder> builderSetter) {
        return builderSetter.apply(spanBuilder(spanName)).startSpan();
    }

    public static void injectIntoEnvMap(@Nullable final Map<String, String> carrier) {
        INSTANCE.getPropagators().getTextMapPropagator().inject(Context.current(), carrier, ContextEnvCarriers.SETTTER);
    }

    public static void injectIntoEnvList(@Nullable final List<String> carrier) {
        INSTANCE.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), carrier, ContextEnvCarriers.LIST_SETTTER);
    }
}
