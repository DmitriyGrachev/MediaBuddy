package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    Optional<List<ChatRoom>> getChatRoomsByUser1(User user1);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.user1 = :user OR cr.user2 = :user")
    Optional<List<ChatRoom>> findChatRoomsByUser(@Param("user") User user);
    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.user1.id = :user1Id AND cr.user2.id = :user2Id) OR (cr.user1.id = :user2Id AND cr.user2.id = :user1Id)")
    Optional<ChatRoom> findChatRoomByUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    ChatRoom findChatRoomById(Long id);
}
