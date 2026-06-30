package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mongodb.DialogRecord;
import java.util.List;

public interface DialogService {
  List<DialogRecord> getUserDialogs(Long userId);

  DialogRecord getDialogById(String dialogId);

  DialogRecord createDialog(DialogRecord dialogRecord);

  void deleteDialog(String dialogId, Long userId);

  DialogRecord updateDialog(DialogRecord dialogRecord);
}
