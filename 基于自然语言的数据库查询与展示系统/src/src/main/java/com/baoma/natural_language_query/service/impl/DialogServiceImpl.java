package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mongodb.DialogRecord;
import com.baoma.natural_language_query.repository.DialogRecordRepository;
import com.baoma.natural_language_query.service.DialogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DialogServiceImpl implements DialogService {

  @Autowired private DialogRecordRepository dialogRecordRepository;

  @Override
  public List<DialogRecord> getUserDialogs(Long userId) {
    try {
      if (userId == null) {
        return List.of(); // 返回空列表而不是 null
      }
      List<DialogRecord> dialogs = dialogRecordRepository.findByUserIdOrderByLastTimeDesc(userId);
      return dialogs != null ? dialogs : List.of(); // 确保不返回 null
    } catch (Exception e) {
      // 记录错误但不抛出异常，返回空列表
      System.err.println("获取用户对话列表失败: " + e.getMessage());
      e.printStackTrace();
      return List.of();
    }
  }

  @Override
  public DialogRecord getDialogById(String dialogId) {
    return dialogRecordRepository.findByDialogId(dialogId);
  }

  @Override
  public DialogRecord createDialog(DialogRecord dialogRecord) {
    if (dialogRecord.getDialogId() == null || dialogRecord.getDialogId().isEmpty()) {
      dialogRecord.setDialogId("conv_" + UUID.randomUUID().toString().replace("-", ""));
    }
    if (dialogRecord.getStartTime() == null) {
      dialogRecord.setStartTime(LocalDateTime.now());
    }
    if (dialogRecord.getLastTime() == null) {
      dialogRecord.setLastTime(LocalDateTime.now());
    }
    if (dialogRecord.getTotalRounds() == null) {
      dialogRecord.setTotalRounds(0);
    }
    return dialogRecordRepository.save(dialogRecord);
  }

  @Override
  public void deleteDialog(String dialogId, Long userId) {
    DialogRecord dialog = dialogRecordRepository.findByDialogId(dialogId);
    if (dialog != null && dialog.getUserId().equals(userId)) {
      dialogRecordRepository.delete(dialog);
    }
  }

  @Override
  public DialogRecord updateDialog(DialogRecord dialogRecord) {
    DialogRecord existing = dialogRecordRepository.findByDialogId(dialogRecord.getDialogId());
    if (existing != null) {
      if (dialogRecord.getTopic() != null) {
        existing.setTopic(dialogRecord.getTopic());
      }
      if (dialogRecord.getTotalRounds() != null) {
        existing.setTotalRounds(dialogRecord.getTotalRounds());
      }
      existing.setLastTime(LocalDateTime.now());
      return dialogRecordRepository.save(existing);
    }
    return null;
  }
}
