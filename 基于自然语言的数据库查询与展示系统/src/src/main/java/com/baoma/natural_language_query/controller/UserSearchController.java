package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.UserSearch;
import com.baoma.natural_language_query.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-search")
public class UserSearchController {

  @Autowired private UserService userService;

  @GetMapping("/list/{userId}")
  public Result<List<UserSearch>> listByUserId(@PathVariable Long userId) {
    return Result.success(userService.listUserSearchesByUserId(userId));
  }

  @GetMapping("/list/top/{userId}/{limit}")
  public Result<List<UserSearch>> listTopSearchesByUserId(
      @PathVariable Long userId, @PathVariable int limit) {
    return Result.success(userService.listTopUserSearchesByUserId(userId, limit));
  }

  @PostMapping
  public Result<UserSearch> save(@RequestBody UserSearch userSearch) {
    UserSearch existing =
        userService.getUserSearchByUserIdAndSqlContent(
            userSearch.getUserId(), userSearch.getSqlContent());
    if (existing != null) {
      existing.setSearchCount(existing.getSearchCount() + 1);
      existing.setLastSearchTime(LocalDateTime.now());
      userService.updateUserSearchById(existing);
      return Result.success(existing);
    } else {
      if (userSearch.getSearchCount() == null) {
        userSearch.setSearchCount(1);
      }
      userSearch.setLastSearchTime(LocalDateTime.now());
      userService.saveUserSearch(userSearch);
      return Result.success(userSearch);
    }
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    userService.removeUserSearchById(id);
    return Result.success();
  }
}
