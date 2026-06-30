package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.DbType;
import com.baoma.natural_language_query.service.DbTypeService;
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
@RequestMapping("/db-type")
public class DbTypeController {

  @Autowired private DbTypeService dbTypeService;

  /** 查询所有数据库类型 */
  @GetMapping("/list")
  public Result<List<DbType>> list() {
    return Result.success(dbTypeService.list());
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  public Result<DbType> getById(@PathVariable Integer id) {
    return Result.success(dbTypeService.getById(id));
  }

  /** 根据类型编码查询 */
  @GetMapping("/code/{typeCode}")
  public Result<DbType> getByTypeCode(@PathVariable String typeCode) {
    return Result.success(dbTypeService.getByTypeCode(typeCode));
  }

  /** 添加数据库类型 */
  @PostMapping
  public Result<DbType> save(@RequestBody DbType dbType) {
    dbTypeService.save(dbType);
    return Result.success(dbType);
  }

  /** 更新数据库类型 */
  @PutMapping
  public Result<DbType> update(@RequestBody DbType dbType) {
    dbTypeService.updateById(dbType);
    return Result.success(dbType);
  }

  /** 删除数据库类型 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Integer id) {
    dbTypeService.removeById(id);
    return Result.success();
  }
}
