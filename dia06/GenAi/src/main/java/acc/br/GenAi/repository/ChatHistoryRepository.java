package acc.br.GenAi.repository;

import acc.br.GenAi.model.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
}
