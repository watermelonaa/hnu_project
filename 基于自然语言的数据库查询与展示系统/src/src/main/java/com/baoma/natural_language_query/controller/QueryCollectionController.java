package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.dto.QueryCollectionDTO;
import com.baoma.natural_language_query.entity.mongodb.QueryCollection;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.service.QueryCollectionService;
import com.baoma.natural_language_query.utils.PermissionUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
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
@RequestMapping("/query-collection")
public class QueryCollectionController {

  @Autowired private QueryCollectionService queryCollectionService;

  @GetMapping("/list/{userId}")
  @RequirePermission(Role.USER)
  public Result<List<QueryCollectionDTO>> listByUserId(@PathVariable Long userId, HttpServletRequest request) {
    // 权限检查：只能查看自己的收藏夹
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能查看自己的收藏夹");
    }

    List<QueryCollection> collections = queryCollectionService.listByUserId(userId);
    // 转换为DTO，映射字段名，并过滤掉无效的收藏夹（groupName 为 null 或空）
    List<QueryCollectionDTO> dtos = collections.stream()
        .filter(c -> c != null && c.getGroupName() != null && !c.getGroupName().trim().isEmpty())
        .map(this::toDTO)
        .filter(dto -> dto.getCollectionName() != null && !dto.getCollectionName().trim().isEmpty())
        .collect(Collectors.toList());
    return Result.success(dtos);
  }

  @GetMapping("/{id}")
  @RequirePermission(Role.USER)
  public Result<QueryCollectionDTO> getById(@PathVariable String id, HttpServletRequest request) {
    QueryCollection collection = queryCollectionService.getById(id);
    if (collection == null) {
      return Result.error("收藏夹不存在");
    }

    // 权限检查：只能查看自己的收藏夹
    if (!PermissionUtil.isOwner(request, collection.getUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能查看自己的收藏夹");
    }

    return Result.success(toDTO(collection));
  }

  @PostMapping
  @RequirePermission(Role.USER)
  public Result<QueryCollectionDTO> save(@RequestBody QueryCollectionDTO dto, HttpServletRequest request) {
    try {
      Long currentUserId = (Long) request.getAttribute("userId");
      if (currentUserId == null) {
        return Result.error("未登录或登录已过期");
      }
      
      // 强制设置 userId 为当前登录用户
      dto.setUserId(currentUserId);
      
      System.out.println("收到创建收藏夹请求: userId=" + dto.getUserId() 
          + ", collectionName=" + dto.getCollectionName());
      
      // 验证 collectionName 不能为空
      if (dto.getCollectionName() == null || dto.getCollectionName().trim().isEmpty()) {
        return Result.error("收藏夹名称不能为空");
      }
      
      QueryCollection collection = fromDTO(dto);
      collection.setCreateTime(LocalDateTime.now());
      
      // 确保 groupName 不为空
      if (collection.getGroupName() == null || collection.getGroupName().trim().isEmpty()) {
        collection.setGroupName("默认收藏夹");
      }
      
      QueryCollection saved = queryCollectionService.save(collection);
      System.out.println("✓ 创建收藏夹成功: id=" + saved.getId() + ", groupName=" + saved.getGroupName());
      
      return Result.success(toDTO(saved));
    } catch (Exception e) {
      System.err.println("创建收藏夹失败: " + e.getMessage());
      e.printStackTrace();
      return Result.error("创建失败: " + e.getMessage());
    }
  }

  @PutMapping
  @RequirePermission(Role.USER)
  public Result<QueryCollectionDTO> update(@RequestBody QueryCollectionDTO dto, HttpServletRequest request) {
    try {
      if (dto.getId() == null) {
        return Result.error("收藏夹ID不能为空");
      }

      QueryCollection existing = queryCollectionService.getById(dto.getId());
      if (existing == null) {
        return Result.error("收藏夹不存在");
      }

      // 权限检查：只能更新自己的收藏夹
      if (!PermissionUtil.isOwner(request, existing.getUserId()) && !PermissionUtil.isAdmin(request)) {
        return Result.error("权限不足，只能更新自己的收藏夹");
      }

      System.out.println("收到更新收藏夹请求: id=" + dto.getId() 
          + ", collectionName=" + dto.getCollectionName());
      
      QueryCollection collection = fromDTO(dto);
      collection.setUserId(existing.getUserId());
      collection.setCreateTime(existing.getCreateTime());
      
      QueryCollection saved = queryCollectionService.save(collection);
      System.out.println("✓ 更新收藏夹成功: id=" + saved.getId());
      
      return Result.success(toDTO(saved));
    } catch (Exception e) {
      System.err.println("更新收藏夹失败: " + e.getMessage());
      e.printStackTrace();
      return Result.error("更新失败: " + e.getMessage());
    }
  }

  // 转换为DTO（后端 -> 前端）
  private QueryCollectionDTO toDTO(QueryCollection collection) {
    QueryCollectionDTO dto = new QueryCollectionDTO();
    dto.setId(collection.getId());
    dto.setUserId(collection.getUserId());
    // 映射 groupName 到 collectionName，如果 groupName 为空则使用默认值
    String groupName = collection.getGroupName();
    if (groupName == null || groupName.trim().isEmpty()) {
      // 如果 groupName 为空，使用默认名称
      groupName = "默认收藏夹";
      // 同时更新数据库中的 groupName
      collection.setGroupName(groupName);
      queryCollectionService.save(collection);
    }
    dto.setCollectionName(groupName);
    dto.setDescription(collection.getDescription());
    dto.setCreateTime(collection.getCreateTime());
    return dto;
  }

  // 从DTO转换（前端 -> 后端）
  private QueryCollection fromDTO(QueryCollectionDTO dto) {
    QueryCollection collection = new QueryCollection();
    collection.setId(dto.getId());
    collection.setUserId(dto.getUserId());
    // 映射 collectionName 到 groupName，如果为空则使用默认值
    String collectionName = dto.getCollectionName();
    if (collectionName == null || collectionName.trim().isEmpty()) {
      collectionName = "默认收藏夹";
    }
    collection.setGroupName(collectionName);
    collection.setDescription(dto.getDescription());
    collection.setCreateTime(dto.getCreateTime());
    return collection;
  }

  @DeleteMapping("/{id}")
  @RequirePermission(Role.USER)
  public Result<Void> delete(@PathVariable String id, HttpServletRequest request) {
    QueryCollection existing = queryCollectionService.getById(id);
    if (existing == null) {
      return Result.error("收藏夹不存在");
    }

    // 权限检查：只能删除自己的收藏夹
    if (!PermissionUtil.isOwner(request, existing.getUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能删除自己的收藏夹");
    }

    queryCollectionService.deleteById(id);
    return Result.success();
  }
  
  /**
   * 清理无效的收藏夹
   */
  @PostMapping("/cleanup-invalid/{userId}")
  @RequirePermission(Role.USER)
  public Result<String> cleanupInvalidCollections(@PathVariable Long userId, HttpServletRequest request) {
    // 权限检查：只能清理自己的收藏夹，除非是管理员
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能清理自己的收藏夹");
    }

    try {
      List<QueryCollection> userCollections = queryCollectionService.listByUserId(userId);
      
      // 找出所有无效的收藏夹
      List<QueryCollection> invalidCollections = userCollections.stream()
          .filter(c -> c != null && (
              c.getGroupName() == null || 
              c.getGroupName().trim().isEmpty() ||
              c.getGroupName().equals("undefined") ||
              c.getGroupName().equals("null")
          ))
          .collect(Collectors.toList());
      
      // 删除无效的收藏夹
      int count = 0;
      for (QueryCollection collection : invalidCollections) {
        try {
          queryCollectionService.deleteById(collection.getId());
          count++;
          System.out.println("✓ 已删除无效收藏夹: id=" + collection.getId() + ", groupName=" + collection.getGroupName());
        } catch (Exception e) {
          System.err.println("删除收藏夹失败: id=" + collection.getId() + ", error=" + e.getMessage());
        }
      }
      
      return Result.success("成功清理 " + count + " 个无效收藏夹");
    } catch (Exception e) {
      System.err.println("清理无效收藏夹失败: " + e.getMessage());
      e.printStackTrace();
      return Result.error("清理失败: " + e.getMessage());
    }
  }
}
