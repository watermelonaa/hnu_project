package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.ColumnMetadata;
import com.baoma.natural_language_query.service.ColumnMetadataService;
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
@RequestMapping("/column-metadata")
public class ColumnMetadataController {

  @Autowired private ColumnMetadataService columnMetadataService;

  /** 查询所有字段元数据 */
  @GetMapping("/list")
  public Result<List<ColumnMetadata>> list() {
    return Result.success(columnMetadataService.list());
  }

  /** 根据表ID查询字段元数据 */
  @GetMapping("/list/{tableId}")
  public Result<List<ColumnMetadata>> listByTable(@PathVariable Long tableId) {
    return Result.success(columnMetadataService.listByTableId(tableId));
  }

  /** 根据ID查询字段元数据 */
  @GetMapping("/{id}")
  public Result<ColumnMetadata> getById(@PathVariable Long id) {
    return Result.success(columnMetadataService.getById(id));
  }

  /** 添加字段元数据 */
  @PostMapping
  public Result<ColumnMetadata> save(@RequestBody ColumnMetadata columnMetadata) {
    columnMetadata.setCreateTime(LocalDateTime.now());
    if (columnMetadata.getIsPrimary() == null) {
      columnMetadata.setIsPrimary(0);
    }
    columnMetadataService.save(columnMetadata);
    return Result.success(columnMetadata);
  }

  /** 更新字段元数据 */
  @PutMapping
  public Result<ColumnMetadata> update(@RequestBody ColumnMetadata columnMetadata) {
    columnMetadataService.updateById(columnMetadata);
    return Result.success(columnMetadata);
  }

  /** 删除字段元数据 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    columnMetadataService.removeById(id);
    return Result.success();
  }
}
