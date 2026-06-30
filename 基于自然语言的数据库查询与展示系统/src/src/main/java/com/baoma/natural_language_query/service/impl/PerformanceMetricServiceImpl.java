package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.PerformanceMetric;
import com.baoma.natural_language_query.mapper.PerformanceMetricMapper;
import com.baoma.natural_language_query.service.PerformanceMetricService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PerformanceMetricServiceImpl implements PerformanceMetricService {

  @Autowired private PerformanceMetricMapper performanceMetricMapper;

  @Override
  public List<PerformanceMetric> list() {
    return performanceMetricMapper.selectList(null);
  }

  @Override
  public List<PerformanceMetric> listByMetricType(String metricType) {
    QueryWrapper<PerformanceMetric> wrapper = new QueryWrapper<>();
    wrapper.eq("metric_type", metricType);
    return performanceMetricMapper.selectList(wrapper);
  }

  @Override
  public List<PerformanceMetric> listByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    QueryWrapper<PerformanceMetric> wrapper = new QueryWrapper<>();
    wrapper.between("metric_time", startTime, endTime);
    return performanceMetricMapper.selectList(wrapper);
  }

  @Override
  public boolean save(PerformanceMetric performanceMetric) {
    return performanceMetricMapper.insert(performanceMetric) > 0;
  }
}
