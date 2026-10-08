package hr.algebra.parkalot.services;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiService {
    private final OllamaChatModel chatModel;

    public AiService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String ask(String message) {
        return chatModel.call(message);
    }

    public Flux<ChatResponse> stream(String message) {
        return chatModel.stream(new Prompt(message));
    }
}
