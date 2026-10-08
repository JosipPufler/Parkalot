package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.services.AiService;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/rest/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<String> ask(@RequestBody String message) {
        return ResponseEntity.ok(aiService.ask(message));
    }

    @PostMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> stream(@RequestBody String message) {
        return aiService.stream(message)
                .map(ChatResponse::getResult)
                .map(result -> result.getOutput().getText());
    }
}
