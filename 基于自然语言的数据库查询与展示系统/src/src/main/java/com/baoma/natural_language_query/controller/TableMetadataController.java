package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.TableMetadata;
import com.baoma.natural_language_query.service.TableMetadataService;
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
@RequestMapping("/table-metadata")
public class TableMetadataController {

  @Autowired private TableMetadataService tableMetadataService;

  /** 查询所有表元数据 */
  @GetMapping("/list")
  public Result<List<TableMetadata>> list() {
    return Result.success(tableMetadataService.list());
  }

  /** 根据数据库连接ID查询表元数据 */
  @GetMapping("/list/{dbConnectionId}")
  public Result<List<TableMetadata>> listByDbConnection(@PathVariable Long dbConnectionId) {
    return Result.success(tableMetadataService.listByDbConnectionId(dbConnectionId));
  }

  /** 根据ID查询表元数据 */
  @GetMapping("/{id}")
  public Result<TableMetadata> getById(@PathVariable Long id) {
    return Result.success(tableMetadataService.getById(id));
  }

  /** 添加表元数据 */
  @PostMapping
  public Result<TableMetadata> save(@RequestBody TableMetadata tableMetadata) {
    tableMetadata.setCreateTime(LocalDateTime.now());
    tableMetadata.setUpdateTime(LocalDateTime.now());
    tableMetadataService.save(tableMetadata);
    return Result.success(tableMetadata);
  }

  /** 更新表元数据 */
  @PutMapping
  public Result<TableMetadata> update(@RequestBody TableMetadata tableMetadata) {
    tableMetadata.setUpdateTime(LocalDateTime.now());
    tableMetadataService.updateById(tableMetadata);
    return Result.success(tableMetadata);
  }

  /** 删除表元数据 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    tableMetadataService.removeById(id);
    return Result.success();
  }
}
