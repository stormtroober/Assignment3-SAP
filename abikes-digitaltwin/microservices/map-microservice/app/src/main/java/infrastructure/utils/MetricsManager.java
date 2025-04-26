package infrastructure.utils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MetricsManager {
  private static final MetricsManager instance = new MetricsManager();
  private final PrometheusMeterRegistry registry;

  private MetricsManager() {
    this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  public static MetricsManager getInstance() {
    return instance;
  }

  public MeterRegistry getRegistry() {
    return registry;
  }

  public String getMetrics() {
    return registry.scrape();
  }

  public Timer.Sample startTimer() {
    return Timer.start(registry);
  }

  public void recordTimer(Timer.Sample sample, String methodName) {
    sample.stop(
        Timer.builder("map_service_method_duration")
            .description("Time taken for map service method execution")
            .tag("method", methodName)
            .register(registry));
  }

  public void incrementMethodCounter(String methodName) {
    registry.counter("map_service_method_calls", "method", methodName).increment();
  }

  public void recordError(Timer.Sample sample, String methodName, Throwable error) {
    sample.stop(
        Timer.builder("map_service_method_duration")
            .description("Time taken for map service method execution")
            .tag("method", methodName)
            .tag("error", error.getClass().getSimpleName())
            .register(registry));
    registry
        .counter(
            "map_service_method_errors",
            "method",
            methodName,
            "error",
            error.getClass().getSimpleName())
        .increment();
  }
}
