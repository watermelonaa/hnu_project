package com.baoma.natural_language_query.repository;

import com.baoma.natural_language_query.entity.mongodb.DialogDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DialogDetailRepository extends MongoRepository<DialogDetail, String> {

  DialogDetail findByDialogId(String dialogId);
}
