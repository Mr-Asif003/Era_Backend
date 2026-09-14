package com.era.backend.conversation.repository;

import com.era.backend.conversation.model.Conversation;
import com.era.backend.conversation.model.ConversationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    @Query("{ 'members.userId': ?0 }")
    List<Conversation> findAllByMemberId(String userId);

    /**
     * Finds an existing DIRECT conversation that has exactly these two
     * members, so re-starting a chat with the same person reuses the
     * existing thread instead of creating a duplicate.
     */
    @Query("{ 'type': ?2, 'members.userId': { $all: [?0, ?1] }, 'members': { $size: 2 } }")
    Optional<Conversation> findDirectBetween(String userIdA, String userIdB, ConversationType type);
}
