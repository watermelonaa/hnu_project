package com.baoma.natural_language_query.repository;

import com.baoma.natural_language_query.entity.mongodb.DialogRecord;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DialogRecordRepository extends MongoRepository<DialogRecord, String> {
  List<DialogRecord> findByUserIdOrderByLastTimeDesc(Long userId);

  DialogRecord findByDialogId(String dialogId);
}
