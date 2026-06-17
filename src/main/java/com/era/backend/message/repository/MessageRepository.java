package com.era.backend.message.repository;

import com.era.backend.message.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(String conversationId, Pageable pageable);
}
