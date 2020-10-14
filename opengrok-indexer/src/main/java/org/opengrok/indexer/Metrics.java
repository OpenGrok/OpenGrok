/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 */
package org.opengrok.indexer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;
import io.micrometer.statsd.StatsdFlavor;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.index.Indexer;
import org.opengrok.indexer.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Encapsulates logic of meter registry setup and handling.
 * Generally, the web application publishes metrics to Prometheus and the Indexer to StatsD.
 */
public final class Metrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrics.class);

    private static final StatsdConfig statsdConfig = new StatsdConfig() {
        @Override
        public String get(String k) {
            return null;
        }

        @Override
        public StatsdFlavor flavor() {
            String flavor = RuntimeEnvironment.getInstance().getStatsdConfig().getFlavor().toLowerCase();
            switch (flavor) {
                case "etsy":
                    return StatsdFlavor.ETSY;
                case "datadog":
                    return StatsdFlavor.DATADOG;
                case "sysdig":
                    return StatsdFlavor.SYSDIG;
                case "telegraf":
                    return StatsdFlavor.TELEGRAF;
            }

            return null;
        }

        @Override
        public int port() {
            return RuntimeEnvironment.getInstance().getStatsdConfig().getPort();
        }

        @Override
        public String host() {
            return RuntimeEnvironment.getInstance().getStatsdConfig().getHost();
        }

        @Override
        public boolean buffered() {
            return true;
        }
    };

    private static PrometheusMeterRegistry prometheusRegistry;
    private static StatsdMeterRegistry statsdRegistry;

    static {
        MeterRegistry registry;

        if (RuntimeEnvironment.getInstance().getStatsdConfig().isEnabled()) {
            LOGGER.log(Level.INFO, "configuring StatsdRegistry");

            statsdRegistry = new StatsdMeterRegistry(statsdConfig, Clock.SYSTEM);

            // Add tag for per-project reindex.
            List<String> subFiles = RuntimeEnvironment.getInstance().getSubFiles();
            if (!subFiles.isEmpty()) {
                List<String> sList = subFiles.stream().
                        map(s -> s.startsWith(Indexer.PATH_SEPARATOR_STRING) ? s.substring(1) : s).
                        collect(Collectors.toList());
                statsdRegistry.config().commonTags(Collections.singleton(Tag.of("projects",
                        String.join(",", sList))));
            }

            registry = statsdRegistry;
        } else {
            LOGGER.log(Level.INFO, "configuring PrometheusRegistry");
            prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            registry = prometheusRegistry;
        }

        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
    }

    private Metrics() {
    }

    public static PrometheusMeterRegistry getPrometheusRegistry() {
        return prometheusRegistry;
    }

    private static StatsdMeterRegistry getStatsdRegistry() {
        return statsdRegistry;
    }

    /**
     * Get registry based on running context.
     * @return MeterRegistry instance
     */
    public static MeterRegistry getRegistry() {
        if (RuntimeEnvironment.getInstance().isIndexer()) {
            return Metrics.getStatsdRegistry();
        } else {
            return Metrics.getPrometheusRegistry();
        }
    }
}
