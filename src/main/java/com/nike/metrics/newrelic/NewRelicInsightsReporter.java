package com.nike.metrics.newrelic;

import com.codahale.metrics.*;
import com.newrelic.api.agent.NewRelic;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * A reporter for Coda Hale Metrics for New Relic Insights
 */
public final class NewRelicInsightsReporter extends ScheduledReporter {

    //private static final Logger logger = LoggerFactory.getLogger(NewRelicInsightsReporter.class);
    private final String metricNamePrefix;

    /**
     * Returns a new {@link Builder} for {@link NewRelicInsightsReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link NewRelicInsightsReporter}
     */
    public static NewRelicInsightsReporter.Builder forRegistry(MetricRegistry registry) {
        return new NewRelicInsightsReporter.Builder(registry);
    }

    /**
     * @param registry         metric registry to get metrics from
     * @param name             reporter name
     * @param filter           metric filter
     * @param rateUnit         unit for reporting rates
     * @param durationUnit     unit for reporting durations
     * @param metricNamePrefix prefix before the metric name used when naming New Relic metrics. Use "" if no prefix is
     *                         needed.
     * @see ScheduledReporter#ScheduledReporter(MetricRegistry, String, MetricFilter, TimeUnit, TimeUnit)
     */
    private NewRelicInsightsReporter(MetricRegistry registry, String name, MetricFilter filter,
                                     TimeUnit rateUnit, TimeUnit durationUnit, String metricNamePrefix) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.metricNamePrefix = metricNamePrefix;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
        SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {

        for (Map.Entry<String, Gauge> gaugeEntry : gauges.entrySet()) {
            doGauge(gaugeEntry.getKey(), gaugeEntry.getValue());
        }

        for (Map.Entry<String, Counter> counterEntry : counters.entrySet()) {
            String name = counterEntry.getKey();
            Counter counter = counterEntry.getValue();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("count", counter.getCount());
            record(name, attributes);
        }

        for (Map.Entry<String, Histogram> histogramEntry : histograms.entrySet()) {
            String name = histogramEntry.getKey();
            Snapshot snapshot = histogramEntry.getValue().getSnapshot();

            Histogram metric = histogramEntry.getValue();
            doHistogramSnapshot(name, snapshot, metric);
        }

        for (Map.Entry<String, Meter> meterEntry : meters.entrySet()) {
            String name = meterEntry.getKey();
            Meter meter = meterEntry.getValue();
            doMetered(name, meter);
        }

        for (Map.Entry<String, Timer> timerEntry : timers.entrySet()) {
            Timer timer = timerEntry.getValue();
            String name = timerEntry.getKey();
            Snapshot snapshot = timer.getSnapshot();

            doTimerMetered(timer, name);
            doTimerSnapshot(timer, name, snapshot);
        }
    }

    private void doMetered(String name, Meter meter) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("count", meter.getCount());
        attributes.put("meanRate:" + getRateUnit(), (float)convertRate(meter.getMeanRate()));
        attributes.put("1MinuteRate:" + getRateUnit(), (float)convertRate(meter.getOneMinuteRate()));
        attributes.put("5MinuteRate:" + getRateUnit(), (float) convertRate(meter.getFiveMinuteRate()));
        attributes.put("5MinuteRate:" + getRateUnit(), (float) convertRate(meter.getFiveMinuteRate()));
        attributes.put("15MinuteRate:" + getRateUnit(), (float) convertRate(meter.getFifteenMinuteRate()));

        record(name, attributes);
    }

    private void doTimerMetered(Timer timer, String name) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("count", timer.getCount());
        attributes.put("meanRate:" + getRateUnit(), (float)convertRate(timer.getMeanRate()));
        attributes.put("1MinuteRate:" + getRateUnit(), (float)convertRate(timer.getOneMinuteRate()));
        attributes.put("5MinuteRate:" + getRateUnit(), (float)convertRate(timer.getFiveMinuteRate()));
        attributes.put("15MinuteRate:" + getRateUnit(), (float)convertRate(timer.getFifteenMinuteRate()));

        record(name, attributes);
    }

    private void doHistogramSnapshot(String name, Snapshot snapshot, Histogram metric) {

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("min", (float)convertDuration(snapshot.getMin()));
        attributes.put("max", (float) convertDuration(snapshot.getMax()));
        attributes.put("mean", (float)convertDuration(snapshot.getMean()));
        attributes.put("stdDev", (float)convertDuration(snapshot.getStdDev()));
        attributes.put("median", (float)convertDuration(snapshot.getMedian()));
        attributes.put("75th", (float)convertDuration(snapshot.get75thPercentile()));
        attributes.put("95th", (float)convertDuration(snapshot.get95thPercentile()));
        attributes.put("98th", (float)convertDuration(snapshot.get98thPercentile()));
        attributes.put("99th", (float)convertDuration(snapshot.get99thPercentile()));
        attributes.put("99.9th", (float)convertDuration(snapshot.get999thPercentile()));

        record(name, attributes);
    }

    private void doTimerSnapshot(Timer timer, String name, Snapshot snapshot) {
        String nameSuffix = ":" + getDurationUnit();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("min" + nameSuffix, (float)convertDuration(snapshot.getMin()));
        attributes.put("max" + nameSuffix, (float) convertDuration(snapshot.getMax()));
        attributes.put("mean" + nameSuffix, (float)convertDuration(snapshot.getMean()));
        attributes.put("stdDev" + nameSuffix, (float)convertDuration(snapshot.getStdDev()));
        attributes.put("median" + nameSuffix, (float)convertDuration(snapshot.getMedian()));
        attributes.put("75th" + nameSuffix, (float)convertDuration(snapshot.get75thPercentile()));
        attributes.put("95th" + nameSuffix, (float)convertDuration(snapshot.get95thPercentile()));
        attributes.put("98th" + nameSuffix, (float)convertDuration(snapshot.get98thPercentile()));
        attributes.put("99th" + nameSuffix, (float)convertDuration(snapshot.get99thPercentile()));
        attributes.put("99.9th" + nameSuffix, (float)convertDuration(snapshot.get999thPercentile()));

        record(name, attributes);
    }

    private void doGauge(String name, Gauge gauge) {
        Object gaugeValue = gauge.getValue();

        if (gaugeValue instanceof Number) {
            float n = ((Number) gaugeValue).floatValue();
            if (!Float.isNaN(n) && !Float.isInfinite(n)) {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("value", n);
                record(name, attributes);
            }
        }
    }

    /**
     * get in a map, write it to NR Insights
     * @param name
     * @param eventAttributes
     */
    private void record(String name, Map<String, Object> eventAttributes) {
        String fullMetricName = metricNamePrefix + name;
        //TODO it would be better to use a regex here...
        NewRelic.getAgent().getInsights().recordCustomEvent(
                replaceCharachtersInsightsDoesNotLike(fullMetricName), eventAttributes);
    }

    /**
     * New Relic Insights doesn't like a bunch of charachters, so we're taking them out here
     * See documentation on it here:
     * https://docs.newrelic.com/docs/insights/new-relic-insights/adding-querying-data/inserting-custom-events-new-relic-apm-agents#limits
     * @param metricName whatever was passed in
     * @return a clean metric name that Insights likes
     */
    public String replaceCharachtersInsightsDoesNotLike(String metricName) {
        String newName = metricName.replace(".",":")
                .replace("-","_")
                .replace("$",":")
                .replace("/",":")
                .replace("[","")
                .replace("]","")
                .replace("{","")
                .replace("}","")
                .replace("*","");
        return newName;
    }

    public static final class Builder {
        private MetricRegistry registry;
        private String name;
        private MetricFilter filter;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private String metricNamePrefix;

        public Builder(MetricRegistry registry) {
            this.registry = registry;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.metricNamePrefix = "";
            this.name = "New Relic Insights Reporter";
            this.filter = MetricFilter.ALL;
        }

        /**
         * @param name reporter name
         * @return this
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param filter metric filter
         * @return this
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * @param rateUnit unit for reporting rates
         * @return this
         */
        public Builder rateUnit(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * @param durationUnit unit for reporting durations
         * @return this
         */
        public Builder durationUnit(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * @param metricNamePrefix prefix before the metric name used when naming New Relic metrics. Use "" if no prefix
         *                         is needed.
         * @return this
         */
        public Builder metricNamePrefix(String metricNamePrefix) {
            this.metricNamePrefix = metricNamePrefix;
            return this;
        }

        public NewRelicInsightsReporter build() {
            return new NewRelicInsightsReporter(registry, name, filter, rateUnit, durationUnit,
                metricNamePrefix);
        }
    }
}
