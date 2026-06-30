package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.PerformanceMetric;
import java.time.LocalDateTime;
import java.util.List;

public interface PerformanceMetricService {

  List<PerformanceMetric> list();

  List<PerformanceMetric> listByMetricType(String metricType);

  List<PerformanceMetric> listByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

  boolean save(PerformanceMetric performanceMetric);
}
