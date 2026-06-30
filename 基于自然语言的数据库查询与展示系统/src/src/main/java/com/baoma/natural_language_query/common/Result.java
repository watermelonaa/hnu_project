package com.baoma.natural_language_query.common;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应结果封装类
 *
 * <p>用于封装所有Controller接口的返回结果，提供统一的响应格式。 包含状态码、消息和数据三部分，便于前端统一处理。
 */
@Setter
@Getter
public class Result<T> {

  /**
   * 响应状态码
   *
   * <ul>
   *   <li>200: 成功
   *   <li>400: 客户端错误（业务异常）
   *   <li>500: 服务器错误
   * </ul>
   *
   * -- GETTER -- 获取响应状态码
   *
   * <p>-- SETTER -- 设置响应状态码
   *
   * @return 状态码
   * @param code 状态码
   */
  private Integer code;

  /**
   * 响应消息 -- GETTER -- 获取响应消息
   *
   * <p>-- SETTER -- 设置响应消息
   *
   * @return 消息内容
   * @param message 消息内容
   */
  private String message;

  /**
   * 响应数据 -- GETTER -- 获取响应数据
   *
   * <p>-- SETTER -- 设置响应数据
   *
   * @return 数据对象
   * @param data 数据对象
   */
  private T data;

  /**
   * 创建成功响应（无数据）
   *
   * @param <T> 数据类型
   * @return 成功响应对象
   */
  public static <T> Result<T> success() {
    return success(null);
  }

  /**
   * 创建成功响应（带数据）
   *
   * @param <T> 数据类型
   * @param data 响应数据
   * @return 成功响应对象
   */
  public static <T> Result<T> success(T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setMessage("success");
    result.setData(data);
    return result;
  }

  /**
   * 创建错误响应（默认500状态码）
   *
   * @param <T> 数据类型
   * @param message 错误消息
   * @return 错误响应对象
   */
  public static <T> Result<T> error(String message) {
    Result<T> result = new Result<>();
    result.setCode(500);
    result.setMessage(message);
    return result;
  }

  /**
   * 创建错误响应（自定义状态码）
   *
   * @param <T> 数据类型
   * @param code 错误状态码
   * @param message 错误消息
   * @return 错误响应对象
   */
  public static <T> Result<T> error(Integer code, String message) {
    Result<T> result = new Result<>();
    result.setCode(code);
    result.setMessage(message);
    return result;
  }
}
