package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.dto.QueryRequestDTO;
import com.baoma.natural_language_query.dto.RecommendationRequestDTO;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.service.QueryService;
import com.baoma.natural_language_query.vo.QueryResponseVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自然语言查询控制器
 */
@RestController
@RequestMapping("/query")
public class QueryController {

  /** 查询服务 */
  @Autowired private QueryService queryService;

  /**
   * 执行自然语言查询
   */
  @PostMapping("/execute")
  @RequirePermission(Role.USER)
  public Result<QueryResponseVO> executeQuery(
      @RequestBody QueryRequestDTO request,
      HttpServletRequest servletRequest) {
    try {
      Long userId = (Long) servletRequest.getAttribute("userId");
      if (userId == null) {
        return Result.error("未登录或登录已过期");
      }
      
      System.out.println("收到查询请求: userId=" + userId + ", dbConnectionId=" + request.getDbConnectionId() + ", model=" + request.getModel());
      QueryResponseVO response = queryService.executeQuery(request, userId);

      List<String> recommendations = response.getFollowupQuestions();
      if (recommendations != null) {
        System.out.println("智能推荐生成成功，共 " + recommendations.size() + " 条");
      }

      System.out.println("查询服务返回响应: id=" + response.getId() + ", dbConnectionId=" + response.getDbConnectionId());
      return Result.success(response);
    } catch (Exception e) {
      System.err.println("查询执行异常: " + e.getMessage());
      e.printStackTrace();
      return Result.error(e.getMessage());
    }
  }

  /**
   * 生成智能推荐查询
   */
  @PostMapping("/recommendations")
  @RequirePermission(Role.USER)
  public Result<List<String>> getRecommendations(@RequestBody RecommendationRequestDTO request) {
    try {
      System.out.println("收到推荐请求: conversationId=" + request.getConversationId() + 
          ", dbConnectionId=" + request.getDbConnectionId() + 
          ", llmConfigId=" + request.getLlmConfigId());
      List<String> recommendations = queryService.getRecommendations(request);
      if (recommendations != null) {
        System.out.println("推荐生成成功，共 " + recommendations.size() + " 条");
      }
      return Result.success(recommendations);
    } catch (Exception e) {
      System.err.println("生成推荐异常: " + e.getMessage());
      e.printStackTrace();
      return Result.error(e.getMessage());
    }
  }
}
