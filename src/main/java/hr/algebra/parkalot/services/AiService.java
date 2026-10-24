package hr.algebra.parkalot.services;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private final OllamaChatModel chatModel;

    public AiService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String readLicencePlate(MultipartFile image) throws IOException {
        Media media = new Media(
                MimeTypeUtils.parseMimeType(image.getContentType()),
                new ByteArrayResource(image.getBytes())
        );

        UserMessage userMessage = UserMessage.builder()
                .text("""
            Identify the vehicle's licence plate in this image.
            Return only the plate text with no spaces and only alphanumeric characters.
            All letters MUST be uppercase, if you read any lowercase letters, try again.
            If it is unreadable or absent, return UNKNOWN.
            Do not guess.
            """)
                .media(media)
                .build();

        return chatModel.call(new Prompt(userMessage))
                .getResult()
                .getOutput()
                .getText();
    }
}
