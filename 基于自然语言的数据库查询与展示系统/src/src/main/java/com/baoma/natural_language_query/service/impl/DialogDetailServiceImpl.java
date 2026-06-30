package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mongodb.DialogDetail;
import com.baoma.natural_language_query.repository.DialogDetailRepository;
import com.baoma.natural_language_query.service.DialogDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DialogDetailServiceImpl implements DialogDetailService {

  @Autowired private DialogDetailRepository dialogDetailRepository;

  @Override
  public DialogDetail getByDialogId(String dialogId) {
    return dialogDetailRepository.findByDialogId(dialogId);
  }

  @Override
  public DialogDetail save(DialogDetail dialogDetail) {
    return dialogDetailRepository.save(dialogDetail);
  }

  @Override
  public void deleteById(String id) {
    dialogDetailRepository.deleteById(id);
  }
}
