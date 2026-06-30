package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.ErrorType;
import com.baoma.natural_language_query.service.ErrorTypeService;
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
@RequestMapping("/error-type")
public class ErrorTypeController {

  @Autowired private ErrorTypeService errorTypeService;

  /** 查询所有错误类型 */
  @GetMapping("/list")
  public Result<List<ErrorType>> list() {
    return Result.success(errorTypeService.list());
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  public Result<ErrorType> getById(@PathVariable Integer id) {
    return Result.success(errorTypeService.getById(id));
  }

  /** 根据错误编码查询 */
  @GetMapping("/code/{errorCode}")
  public Result<ErrorType> getByErrorCode(@PathVariable String errorCode) {
    return Result.success(errorTypeService.getByErrorCode(errorCode));
  }

  /** 添加错误类型 */
  @PostMapping
  public Result<ErrorType> save(@RequestBody ErrorType errorType) {
    errorTypeService.save(errorType);
    return Result.success(errorType);
  }

  /** 更新错误类型 */
  @PutMapping
  public Result<ErrorType> update(@RequestBody ErrorType errorType) {
    errorTypeService.updateById(errorType);
    return Result.success(errorType);
  }

  /** 删除错误类型 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Integer id) {
    errorTypeService.removeById(id);
    return Result.success();
  }
}
