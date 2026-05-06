package acc.br.GenAi.controller;

import acc.br.GenAi.model.ChatHistory;
import acc.br.GenAi.repository.ChatHistoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final ChatHistoryRepository repository;

    public ChatController(ChatClient.Builder builder, ChatHistoryRepository repository) {
        this.chatClient = builder.build();
        this.repository = repository;
    }

    @GetMapping("/")
    public ChatResponse chat() {
        String promptText = "Qual a capital do japão?";
        ChatResponse response = chatClient.prompt()
                .user(promptText)
                .call()
                .chatResponse();

        var result = response != null ? response.getResult() : null;
        var output = result != null ? result.getOutput() : null;
        String text = output != null ? output.getText() : "";
        repository.save(new ChatHistory(promptText, text));
        return response;
    }

    @GetMapping("/q")
    public String chat2() {
        String promptText = "Qual a capital do japão?";
        String content = chatClient.prompt()
                .user(promptText)
                .call()
                .content();

        repository.save(new ChatHistory(promptText, content));
        return content;
    }

}
