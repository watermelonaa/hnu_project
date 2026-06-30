package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.LlmConfig;
import com.baoma.natural_language_query.entity.mysql.OperationLog;
import com.baoma.natural_language_query.service.LlmConfigService;
import com.baoma.natural_language_query.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/llm-config")
public class LLMConfigController {

  @Autowired private LlmConfigService llmConfigService;

  @Autowired private OperationLogService operationLogService;

  /** 查询所有大模型配置 */
  @GetMapping("/list")
  public Result<List<LlmConfig>> list() {
    return Result.success(llmConfigService.list());
  }

  /** 查询所有可用的大模型配置 */
  @GetMapping("/list/available")
  public Result<List<LlmConfig>> listAvailable() {
    return Result.success(llmConfigService.listAvailable());
  }

  /** 根据ID查询大模型配置 */
  @GetMapping("/{id}")
  public Result<LlmConfig> getById(@PathVariable Long id) {
    return Result.success(llmConfigService.getById(id));
  }

  /** 添加大模型配置 */
  @PostMapping
  public Result<LlmConfig> save(@RequestBody LlmConfig llmConfig, HttpServletRequest request) {
    // 验证必填字段
    if (llmConfig.getName() == null || llmConfig.getName().trim().isEmpty()) {
      return Result.error("模型名称不能为空");
    }
    if (llmConfig.getVersion() == null || llmConfig.getVersion().trim().isEmpty()) {
      return Result.error("模型版本不能为空");
    }
    if (llmConfig.getApiKey() == null || llmConfig.getApiKey().trim().isEmpty()) {
      return Result.error("API密钥不能为空");
    }
    if (llmConfig.getApiUrl() == null || llmConfig.getApiUrl().trim().isEmpty()) {
      return Result.error("API地址不能为空");
    }

    // 设置默认值
    llmConfig.setCreateTime(LocalDateTime.now());
    llmConfig.setUpdateTime(LocalDateTime.now());
    if (llmConfig.getIsDisabled() == null) {
      llmConfig.setIsDisabled(0);
    }
    if (llmConfig.getStatusId() == null) {
      llmConfig.setStatusId(1); // 默认状态ID为1
    }
    if (llmConfig.getTimeout() == null) {
      llmConfig.setTimeout(60000); // 默认60秒
    }
    if (llmConfig.getCreateUserId() == null) {
      // 从请求属性中获取当前用户ID（由JWT拦截器设置）
      Long userId = (Long) request.getAttribute("userId");
      llmConfig.setCreateUserId(userId != null ? userId : 1L);
    }

    try {
      llmConfigService.save(llmConfig);

      // 记录操作日志
      OperationLog log = new OperationLog();
      log.setUserId((Long) request.getAttribute("userId"));
      log.setUsername((String) request.getAttribute("username"));
      log.setOperation("创建");
      log.setModule("大模型配置");
      log.setRelatedLlm(llmConfig.getName());
      log.setIpAddress(getClientIp(request));
      log.setOperateTime(LocalDateTime.now());
      log.setResult(1);
      operationLogService.save(log);

      return Result.success(llmConfig);
    } catch (Exception e) {
      // 记录失败日志
      OperationLog log = new OperationLog();
      log.setUserId((Long) request.getAttribute("userId"));
      log.setUsername((String) request.getAttribute("username"));
      log.setOperation("创建");
      log.setModule("大模型配置");
      log.setRelatedLlm(llmConfig.getName());
      log.setIpAddress(getClientIp(request));
      log.setOperateTime(LocalDateTime.now());
      log.setResult(0);
      log.setErrorMsg(e.getMessage());
      operationLogService.save(log);

      return Result.error("添加失败：" + e.getMessage());
    }
  }

  /** 更新大模型配置 */
  @PutMapping
  public Result<LlmConfig> update(@RequestBody LlmConfig llmConfig, HttpServletRequest request) {
    llmConfig.setUpdateTime(LocalDateTime.now());

    try {
      LlmConfig existingConfig = llmConfigService.getById(llmConfig.getId());
      String llmName = existingConfig != null ? existingConfig.getName() : "未知模型";

      llmConfigService.updateById(llmConfig);

      // 记录操作日志
      OperationLog log = new OperationLog();
      log.setUserId((Long) request.getAttribute("userId"));
      log.setUsername((String) request.getAttribute("username"));
      log.setOperation("更新");
      log.setModule("大模型配置");
      log.setRelatedLlm(llmName);
      log.setIpAddress(getClientIp(request));
      log.setOperateTime(LocalDateTime.now());
      log.setResult(1);
      operationLogService.save(log);

      return Result.success(llmConfig);
    } catch (Exception e) {
      // 记录失败日志
      OperationLog log = new OperationLog();
      log.setUserId((Long) request.getAttribute("userId"));
      log.setUsername((String) request.getAttribute("username"));
      log.setOperation("更新");
      log.setModule("大模型配置");
      log.setRelatedLlm(llmConfig.getName());
      log.setIpAddress(getClientIp(request));
      log.setOperateTime(LocalDateTime.now());
      log.setResult(0);
      log.setErrorMsg(e.getMessage());
      operationLogService.save(log);

      return Result.error("更新失败：" + e.getMessage());
    }
  }

  /** 删除大模型配置 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    try {
      LlmConfig config = llmConfigService.getById(id);
      String llmName = config != null ? config.getName() : "未知模型";

      llmConfigService.removeById(id);

      // 记录操作日志
      OperationLog log = new OperationLog();
      log.setUserId((Long) request.getAttribute("userId"));
      log.setUsername((String) request.getAttribute("username"));
      log.setOperation("删除");
      log.setModule("大模型配置");
      log.setRelatedLlm(llmName);
      log.setIpAddress(getClientIp(request));
      log.setOperateTime(LocalDateTime.now());
      log.setResult(1);
      operationLogService.save(log);

      return Result.success();
    } catch (Exception e) {
      // 记录失败日志
      OperationLog log = new OperationLog();
      log.setUserId((Long) request.getAttribute("userId"));
      log.setUsername((String) request.getAttribute("username"));
      log.setOperation("删除");
      log.setModule("大模型配置");
      log.setRelatedLlm("未知模型");
      log.setIpAddress(getClientIp(request));
      log.setOperateTime(LocalDateTime.now());
      log.setResult(0);
      log.setErrorMsg(e.getMessage());
      operationLogService.save(log);

      return Result.error("删除失败：" + e.getMessage());
    }
  }

  /** 禁用/启用大模型配置 */
  @PutMapping("/{id}/toggle")
  public Result<Void> toggle(@PathVariable Long id, HttpServletRequest request) {
    try {
      LlmConfig config = llmConfigService.getById(id);
      if (config != null) {
        String operation = config.getIsDisabled() == 0 ? "禁用" : "启用";
        config.setIsDisabled(config.getIsDisabled() == 0 ? 1 : 0);
        config.setUpdateTime(LocalDateTime.now());
        llmConfigService.updateById(config);

        // 记录操作日志
        OperationLog log = new OperationLog();
        log.setUserId((Long) request.getAttribute("userId"));
        log.setUsername((String) request.getAttribute("username"));
        log.setOperation(operation);
        log.setModule("大模型配置");
        log.setRelatedLlm(config.getName());
        log.setIpAddress(getClientIp(request));
        log.setOperateTime(LocalDateTime.now());
        log.setResult(1);
        operationLogService.save(log);
      }
      return Result.success();
    } catch (Exception e) {
      // 记录失败日志
      OperationLog log = new OperationLog();
      log.setUserId((Long) request.getAttribute("userId"));
      log.setUsername((String) request.getAttribute("username"));
      log.setOperation("切换状态");
      log.setModule("大模型配置");
      log.setRelatedLlm("未知模型");
      log.setIpAddress(getClientIp(request));
      log.setOperateTime(LocalDateTime.now());
      log.setResult(0);
      log.setErrorMsg(e.getMessage());
      operationLogService.save(log);

      return Result.error("操作失败：" + e.getMessage());
    }
  }

  /** 获取客户端IP地址 */
  private String getClientIp(HttpServletRequest request) {
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
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip != null ? ip : "unknown";
  }
}

