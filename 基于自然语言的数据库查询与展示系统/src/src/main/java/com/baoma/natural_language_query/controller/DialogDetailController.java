package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mongodb.DialogDetail;
import com.baoma.natural_language_query.service.DialogDetailService;
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
@RequestMapping("/dialog-detail")
public class DialogDetailController {

  @Autowired private DialogDetailService dialogDetailService;

  @GetMapping("/{dialogId}")
  public Result<DialogDetail> getByDialogId(@PathVariable String dialogId) {
    return Result.success(dialogDetailService.getByDialogId(dialogId));
  }

  @PostMapping
  public Result<DialogDetail> save(@RequestBody DialogDetail dialogDetail) {
    DialogDetail saved = dialogDetailService.save(dialogDetail);
    return Result.success(saved);
  }

  @PutMapping
  public Result<DialogDetail> update(@RequestBody DialogDetail dialogDetail) {
    DialogDetail saved = dialogDetailService.save(dialogDetail);
    return Result.success(saved);
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable String id) {
    dialogDetailService.deleteById(id);
    return Result.success();
  }
}
