package com.baoma.natural_language_query.exception;

/**
 * 业务异常类
 *
 * <p>用于处理业务逻辑中的异常情况，与系统异常区分开。 业务异常通常由用户操作不当或业务规则不满足引起， 需要返回明确的错误信息给前端。
 *
 * <p>使用场景：
 *
 * <ul>
 *   <li>用户输入验证失败
 *   <li>业务规则校验不通过
 *   <li>资源不存在或无权访问
 *   <li>操作冲突（如重复提交）
 * </ul>
 */
public class BusinessException extends RuntimeException {

  /**
   * 业务错误码
   *
   * <ul>
   *   <li>400: 默认业务错误码（客户端请求错误）
   *   <li>401: 未授权
   *   <li>403: 禁止访问
   *   <li>404: 资源不存在
   * </ul>
   */
  private int code;

  /**
   * 构造函数（使用默认错误码400）
   *
   * @param message 错误信息
   */
  public BusinessException(String message) {
    super(message);
    this.code = 400; // 默认业务错误码
  }

  /**
   * 构造函数（自定义错误码）
   *
   * @param code 错误码
   * @param message 错误信息
   */
  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * 获取错误码
   *
   * @return 错误码
   */
  public int getCode() {
    return code;
  }

  /**
   * 设置错误码
   *
   * @param code 错误码
   */
  public void setCode(int code) {
    this.code = code;
  }
}
