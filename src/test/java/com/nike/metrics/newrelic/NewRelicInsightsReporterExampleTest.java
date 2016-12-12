package com.nike.metrics.newrelic;

import com.codahale.metrics.*;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo class for testing setup with tests along the way
 */
public class NewRelicInsightsReporterExampleTest {

    @Test
    public void testSetup() {
        MetricRegistry registry = new MetricRegistry();
        final AtomicInteger gaugeInteger = new AtomicInteger();
        registry.register(name("gauge"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return gaugeInteger.get();
            }
        });
        final Counter counter = registry.counter(name("counter"));
        final Histogram histogram = registry.histogram(name("histogram"));
        final Meter meter = registry.meter(name("meter"));
        final Timer timer = registry.timer(name("timer"));

        NewRelicInsightsReporter reporter = NewRelicInsightsReporter.forRegistry(registry)
                .name("New Relic Insights Reporter")
                .filter(MetricFilter.ALL)
                .rateUnit(TimeUnit.SECONDS)
                .durationUnit(TimeUnit.MILLISECONDS)
                .metricNamePrefix("homeslice:")
                .build();

        //reporter gets created and extends the dropwizard stuff
        Assert.assertNotNull(reporter);
        Assert.assertTrue(reporter instanceof ScheduledReporter);

        reporter.start(60, TimeUnit.SECONDS);

        ScheduledExecutorService svc = Executors.newScheduledThreadPool(1);

        final Random random = new Random();

        svc.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                gaugeInteger.incrementAndGet();
                Assert.assertEquals(gaugeInteger.get(), 1);
                counter.inc();
                Assert.assertEquals(counter.getCount(), 1);
                histogram.update(random.nextInt(10));
                meter.mark();
                Assert.assertEquals(meter.getCount(), 1);
                timer.update(random.nextInt(10), TimeUnit.MILLISECONDS);
            }
        }, 0, 1, TimeUnit.SECONDS);

        String metricName1 = "awesome-metric";
        String metricName2 = "$awesomeMetric";
        String metricName3 = "awesome_metric";
        String metricName4 = "[awesome].[metric]";
        String metricName5 = "awesome.metric";

        Assert.assertEquals("Underscores are replaced", "awesome_metric", reporter.replaceCharachtersInsightsDoesNotLike(metricName1));
    }


    @Mock
    MetricRegistry mockRegistry;

    @Test
    public void testMoreStuff() {
        //TODO add mockito magic to test stuff out foreals
        //TODO mock out all the dependancies
        //TODO verify they all call the right stuff

        NewRelicInsightsReporter reporter = NewRelicInsightsReporter.forRegistry(mockRegistry)
                .name("New Relic Insights Reporter")
                .filter(MetricFilter.ALL)
                .rateUnit(TimeUnit.SECONDS)
                .durationUnit(TimeUnit.MILLISECONDS)
                .metricNamePrefix("homeslice:")
                .build();

        //reporter gets created and extends the dropwizard stuff
        Assert.assertNotNull(reporter);
        Assert.assertTrue(reporter instanceof ScheduledReporter);

        reporter.start(60, TimeUnit.SECONDS);

        ScheduledExecutorService svc = Executors.newScheduledThreadPool(1);

        final Random random = new Random();

        svc.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
//                gaugeInteger.incrementAndGet();
//                counter.inc();
//                histogram.update(random.nextInt(10));
//                meter.mark();
//                timer.update(random.nextInt(10), TimeUnit.MILLISECONDS);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static String name(String s) {
        return MetricRegistry.name(NewRelicInsightsReporterExampleTest.class, s);
    }
}
