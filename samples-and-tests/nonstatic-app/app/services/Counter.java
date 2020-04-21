package services;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class Counter {
  private final Map<String, AtomicLong> namedCounters = new HashMap<>();

  public void inc(String name) {
    namedCounters.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
  }

  public long get(String name) {
    return namedCounters.get(name).get();
  }
}
