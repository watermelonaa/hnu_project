package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.PerformanceMetric;
import com.baoma.natural_language_query.service.PerformanceMetricService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/performance-metric")
public class PerformanceMetricController {

  @Autowired private PerformanceMetricService performanceMetricService;

  @GetMapping("/list")
  public Result<List<PerformanceMetric>> list() {
    return Result.success(performanceMetricService.list());
  }

  @GetMapping("/list/{metricType}")
  public Result<List<PerformanceMetric>> listByMetricType(@PathVariable String metricType) {
    return Result.success(performanceMetricService.listByMetricType(metricType));
  }

  @GetMapping("/range")
  public Result<List<PerformanceMetric>> listByTimeRange(
      @RequestParam String startTime, @RequestParam String endTime) {
    LocalDateTime start = LocalDateTime.parse(startTime);
    LocalDateTime end = LocalDateTime.parse(endTime);
    return Result.success(performanceMetricService.listByTimeRange(start, end));
  }

  @PostMapping
  public Result<PerformanceMetric> save(@RequestBody PerformanceMetric performanceMetric) {
    performanceMetricService.save(performanceMetric);
    return Result.success(performanceMetric);
  }
}
