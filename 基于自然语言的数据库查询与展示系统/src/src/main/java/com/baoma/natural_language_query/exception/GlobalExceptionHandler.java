package com.baoma.natural_language_query.exception;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.ErrorLog;
import com.baoma.natural_language_query.entity.mysql.OperationLog;
import com.baoma.natural_language_query.service.ErrorLogService;
import com.baoma.natural_language_query.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器
 *
 * <p>统一处理系统异常，包括：
 *
 * <ul>
 *   <li>业务异常（BusinessException）- 返回400状态码
 *   <li>系统异常（Exception）- 返回500状态码并记录日志
 *   <li>数据库异常（DataAccessException）- 记录详细错误信息
 *   <li>网络异常 - 记录连接错误
 * </ul>
 *
 * <p>所有异常都会记录到操作日志中，便于排查问题。
 *
 * @author 神奇宝码队
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Autowired private OperationLogService operationLogService;
  
  @Autowired private ErrorLogService errorLogService;

  /**
   * 处理业务异常
   *
   * @param e 业务异常
   * @param request HTTP请求
   * @return 错误响应
   */
  @ExceptionHandler(BusinessException.class)
  public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
    logger.warn("业务异常: {} - {}", request.getRequestURI(), e.getMessage());
    recordErrorLog(request, "业务异常", "业务异常处理", e.getMessage(), e);
    return Result.error(e.getCode(), e.getMessage());
  }

  /**
   * 处理数据库访问异常
   *
   * @param e 数据库异常
   * @param request HTTP请求
   * @return 错误响应
   */
  @ExceptionHandler(DataAccessException.class)
  public Result<?> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
    logger.error("数据库访问异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
    String errorMsg = "数据库操作失败: " + e.getMessage();
    recordErrorLog(request, "数据库异常", "数据库异常处理", errorMsg, e);
    return Result.error(500, "数据库操作失败，请稍后重试");
  }

  /**
   * 处理404异常（资源未找到）
   *
   * @param e 404异常
   * @param request HTTP请求
   * @return 错误响应
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  public Result<?> handleNotFoundException(NoHandlerFoundException e, HttpServletRequest request) {
    logger.warn("资源未找到: {} - {}", request.getRequestURI(), e.getMessage());
    return Result.error(404, "请求的资源不存在: " + request.getRequestURI());
  }

  /**
   * 处理网络连接异常
   *
   * @param e 网络异常
   * @param request HTTP请求
   * @return 错误响应
   */
  @ExceptionHandler({
    java.net.ConnectException.class,
    java.net.SocketTimeoutException.class,
    java.net.UnknownHostException.class,
    org.springframework.web.client.ResourceAccessException.class
  })
  public Result<?> handleNetworkException(Exception e, HttpServletRequest request) {
    logger.error("网络异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
    String errorMsg = "网络连接失败: " + e.getClass().getSimpleName() + " - " + e.getMessage();
    recordErrorLog(request, "网络异常", "网络异常处理", errorMsg, e);
    return Result.error(500, "网络连接失败，请检查网络设置或稍后重试");
  }

  /**
   * 处理所有其他异常（系统崩溃、未预期的异常等）
   *
   * @param e 异常
   * @param request HTTP请求
   * @return 错误响应
   */
  @ExceptionHandler(Exception.class)
  public Result<?> handleException(Exception e, HttpServletRequest request) {
    logger.error(
        "系统异常: {} - {}",
        request.getRequestURI(),
        e.getMessage(),
        e); // 记录完整堆栈信息
    
    // 排除某些不需要记录的异常
    if (e instanceof NoHandlerFoundException) {
      // 404异常已经在上面单独处理，这里不应该到达
      return Result.error(404, "请求的资源不存在: " + request.getRequestURI());
    }
    
    String errorMsg =
        "系统错误: "
            + e.getClass().getSimpleName()
            + " - "
            + (e.getMessage() != null ? e.getMessage() : "无错误消息")
            + "\n堆栈: "
            + Arrays.toString(e.getStackTrace()).substring(0, Math.min(500, Arrays.toString(e.getStackTrace()).length()));
    
    logger.info("准备记录异常日志: 模块=系统异常, 操作=系统异常处理, URI={}", request.getRequestURI());
    recordErrorLog(request, "系统异常", "系统异常处理", errorMsg, e);
    logger.info("异常日志记录完成");
    
    return Result.error(500, "系统内部错误，请联系管理员");
  }

  /**
   * 记录错误日志到操作日志表
   *
   * @param request HTTP请求
   * @param module 模块名称（如：系统异常、数据库异常、网络异常、业务异常）
   * @param operation 操作类型（如：系统异常处理、数据库异常处理等）
   * @param errorMsg 错误消息
   * @param exception 异常对象
   */
  private void recordErrorLog(
      HttpServletRequest request, String module, String operation, String errorMsg, Exception exception) {
    try {
      logger.debug("开始记录错误日志: module={}, operation={}, URI={}", module, operation, request.getRequestURI());
      
      OperationLog log = new OperationLog();
      log.setModule(module);
      log.setOperation(operation);
      log.setOperateTime(LocalDateTime.now());
      log.setResult(0); // 失败

      // 从请求中获取用户信息
      Long userId = (Long) request.getAttribute("userId");
      String username = (String) request.getAttribute("username");
      
      // 设置userId：如果为null，使用系统管理员ID（1）作为默认值
      // 注意：不能使用0，因为外键约束要求user_id必须存在于users表中
      if (userId != null) {
        log.setUserId(userId);
        logger.debug("从请求中获取到userId: {}", userId);
      } else {
        // 使用系统管理员ID（1）作为默认值，确保外键约束通过
        log.setUserId(1L);
        logger.debug("请求中没有userId，将使用系统管理员（userId=1）");
      }
      
      // 设置username：如果为null或空，使用"系统"
      if (username != null && !username.isEmpty()) {
        log.setUsername(username);
      } else {
        log.setUsername("系统");
      }

      // 构建详细的错误信息
      StringBuilder details = new StringBuilder();
      details.append("请求路径: ").append(request.getRequestURI()).append("\n");
      details.append("请求方法: ").append(request.getMethod()).append("\n");
      details.append("错误类型: ").append(exception.getClass().getName()).append("\n");
      details.append("错误消息: ").append(errorMsg).append("\n");
      
      // 添加堆栈信息（限制长度）
      String stackTrace = Arrays.toString(exception.getStackTrace());
      if (stackTrace.length() > 1000) {
        stackTrace = stackTrace.substring(0, 1000) + "...";
      }
      details.append("堆栈信息: ").append(stackTrace);

      log.setErrorMsg(details.toString());

      // 获取客户端IP
      String ipAddress = getClientIpAddress(request);
      log.setIpAddress(ipAddress != null ? ipAddress : "unknown");

      // 确保userId不为null（MyBatis Plus可能要求）
      if (log.getUserId() == null) {
        log.setUserId(0L);
      }
      
      logger.debug("准备保存日志到数据库: module={}, operation={}, userId={}, username={}", 
          log.getModule(), log.getOperation(), log.getUserId(), log.getUsername());
      
      operationLogService.save(log);
      logger.info("异常日志已成功保存到数据库: id={}, module={}, operation={}, URI={}", 
          log.getId(), module, operation, request.getRequestURI());
      
      // 同时保存到ErrorLog表
      recordErrorLogToErrorTable(request, module, exception);
    } catch (Exception e) {
      // 如果记录日志失败，只记录到系统日志，避免循环异常
      logger.error("记录错误日志失败: module={}, operation={}, URI={}", module, operation, request.getRequestURI(), e);
      logger.error("记录失败原因: {}", e.getMessage(), e);
    }
  }
  
  /**
   * 记录异常到ErrorLog表
   *
   * @param request HTTP请求
   * @param module 模块名称（如：系统异常、数据库异常、网络异常、业务异常）
   * @param exception 异常对象
   */
  private void recordErrorLogToErrorTable(
      HttpServletRequest request, String module, Exception exception) {
    try {
      ErrorLog errorLog = new ErrorLog();
      
      // 根据异常类型和模块名称映射errorTypeId
      // 1=模型调用超时, 2=数据库连接错误, 3=用户认证失败, 0=其他
      Integer errorTypeId = mapExceptionToErrorTypeId(module, exception);
      errorLog.setErrorTypeId(errorTypeId);
      
      // 每个异常记录为1次
      errorLog.setErrorCount(1);
      
      // 错误率设为null（单个异常不计算错误率）
      errorLog.setErrorRate(null);
      
      // 使用当前日期作为period（格式：yyyy-MM-dd）
      errorLog.setPeriod(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      
      // 统计时间设为当前时间
      errorLog.setStatTime(LocalDateTime.now());
      
      errorLogService.save(errorLog);
      logger.info("异常日志已成功保存到ErrorLog表: id={}, errorTypeId={}, module={}, URI={}", 
          errorLog.getId(), errorTypeId, module, request.getRequestURI());
    } catch (Exception e) {
      // 如果记录ErrorLog失败，只记录到系统日志，避免循环异常
      logger.error("记录ErrorLog失败: module={}, URI={}", module, request.getRequestURI(), e);
    }
  }
  
  /**
   * 根据异常类型和模块名称映射errorTypeId
   * 
   * @param module 模块名称
   * @param exception 异常对象
   * @return errorTypeId: 1=模型调用超时, 2=数据库连接错误, 3=SQL语法错误, 4=权限不足, 5=用户认证失败, 0=系统其他错误
   */
  private Integer mapExceptionToErrorTypeId(String module, Exception exception) {
    // 根据模块名称判断
    if (module != null) {
      if (module.contains("数据库") || module.contains("DataAccess")) {
        return 2; // 数据库连接错误
      }
      if (module.contains("网络") || module.contains("Network") || 
          exception instanceof java.net.ConnectException ||
          exception instanceof java.net.SocketTimeoutException ||
          exception instanceof java.net.UnknownHostException) {
        // 网络异常可能是模型调用超时
        if (exception instanceof java.net.SocketTimeoutException) {
          return 1; // 模型调用超时
        }
        return 0; // 其他网络异常
      }
      if (module.contains("认证") || module.contains("Auth") || 
          exception.getMessage() != null && 
          (exception.getMessage().contains("认证") || 
           exception.getMessage().contains("未登录") ||
           exception.getMessage().contains("登录已过期") ||
           exception.getMessage().contains("未授权") ||
           exception.getMessage().contains("Unauthorized"))) {
        return 5; // 用户认证失败
      }
      if (module.contains("权限") || (exception.getMessage() != null && exception.getMessage().contains("权限不足"))) {
        return 4; // 权限不足
      }
    }
    
    // 根据异常类型判断
    if (exception instanceof DataAccessException) {
      return 2; // 数据库连接错误
    }
    if (exception instanceof java.net.SocketTimeoutException) {
      return 1; // 模型调用超时
    }
    if (exception instanceof org.springframework.security.core.AuthenticationException ||
        exception.getMessage() != null && 
        (exception.getMessage().contains("认证") || 
         exception.getMessage().contains("未授权") ||
         exception.getMessage().contains("未登录"))) {
      return 5; // 用户认证失败
    }
    
    // 默认返回其他错误类型 (ID=0)
    return 0;
  }

  /**
   * 获取客户端IP地址
   *
   * @param request HTTP请求
   * @return IP地址
   */
  private String getClientIpAddress(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    // 如果是多级代理，取第一个IP
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }
}

