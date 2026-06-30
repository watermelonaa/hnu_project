package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.Priority;
import com.baoma.natural_language_query.service.PriorityService;
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
@RequestMapping("/priority")
public class PriorityController {

  @Autowired private PriorityService priorityService;

  /** 查询所有优先级（按排序） */
  @GetMapping("/list")
  public Result<List<Priority>> list() {
    return Result.success(priorityService.listOrderBySort());
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  public Result<Priority> getById(@PathVariable Integer id) {
    return Result.success(priorityService.getById(id));
  }

  /** 根据优先级编码查询 */
  @GetMapping("/code/{priorityCode}")
  public Result<Priority> getByPriorityCode(@PathVariable String priorityCode) {
    return Result.success(priorityService.getByPriorityCode(priorityCode));
  }

  /** 添加优先级 */
  @PostMapping
  public Result<Priority> save(@RequestBody Priority priority) {
    priorityService.save(priority);
    return Result.success(priority);
  }

  /** 更新优先级 */
  @PutMapping
  public Result<Priority> update(@RequestBody Priority priority) {
    priorityService.updateById(priority);
    return Result.success(priority);
  }

  /** 删除优先级 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Integer id) {
    priorityService.removeById(id);
    return Result.success();
  }
}
