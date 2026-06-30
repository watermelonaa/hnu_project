package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常测试控制器
 * 
 * <p>用于测试全局异常处理器是否正常工作
 * 注意：仅用于测试，生产环境应移除或限制访问
 * 
 * @author 神奇宝码队
 * @version 1.0
 */
@RestController
@RequestMapping("/test-exception")
public class ExceptionTestController {

  /**
   * 测试系统异常（NullPointerException）
   * 
   * 访问：GET /test-exception/null-pointer
   */
  @GetMapping("/null-pointer")
  public Result<?> testNullPointer() {
    String str = null;
    // 故意触发 NullPointerException
    int length = str.length();
    return Result.success("不会执行到这里");
  }

  /**
   * 测试数组越界异常
   * 
   * 访问：GET /test-exception/array-index
   */
  @GetMapping("/array-index")
  public Result<?> testArrayIndex() {
    int[] arr = new int[5];
    // 故意触发 ArrayIndexOutOfBoundsException
    int value = arr[10];
    return Result.success("不会执行到这里");
  }

  /**
   * 测试除零异常
   * 
   * 访问：GET /test-exception/divide-zero
   */
  @GetMapping("/divide-zero")
  public Result<?> testDivideZero() {
    // 故意触发 ArithmeticException
    int result = 10 / 0;
    return Result.success("不会执行到这里");
  }

  /**
   * 测试运行时异常
   * 
   * 访问：GET /test-exception/runtime
   */
  @GetMapping("/runtime")
  public Result<?> testRuntimeException() {
    throw new RuntimeException("这是一个测试用的运行时异常");
  }

  /**
   * 测试数据库异常（需要数据库连接）
   * 
   * 访问：GET /test-exception/database
   * 
   * 注意：这个接口会尝试执行一个错误的SQL，触发数据库异常
   */
  @GetMapping("/database")
  public Result<?> testDatabaseException() {
    // 通过执行错误的SQL来触发数据库异常
    // 实际使用时，可以通过停止数据库服务来测试
    throw new org.springframework.dao.DataAccessException("测试数据库异常：模拟数据库连接失败") {};
  }
}

