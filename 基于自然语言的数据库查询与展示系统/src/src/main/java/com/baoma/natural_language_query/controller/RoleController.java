package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.Role;
import com.baoma.natural_language_query.service.RoleService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/role")
public class RoleController {

  @Autowired private RoleService roleService;

  @PostMapping
  public Result<Role> add(@RequestBody Role role) {
    roleService.save(role);
    return Result.success(role);
  }

  @GetMapping("/list")
  public Result<List<Role>> list() {
    return Result.success(roleService.list());
  }
}
