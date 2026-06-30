package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mongodb.DialogDetail;

public interface DialogDetailService {

  DialogDetail getByDialogId(String dialogId);

  DialogDetail save(DialogDetail dialogDetail);

  void deleteById(String id);
}
