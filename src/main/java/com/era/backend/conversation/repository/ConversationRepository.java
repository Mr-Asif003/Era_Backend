package com.era.backend.conversation.repository;

import com.era.backend.conversation.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    @Query("{ 'members.userId': ?0 }")
    List<Conversation> findAllByMemberId(String userId);
}
