package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Optional<List<ChatMessage>> findAllByChatRoom_Id(Long chatRoomId);
}
