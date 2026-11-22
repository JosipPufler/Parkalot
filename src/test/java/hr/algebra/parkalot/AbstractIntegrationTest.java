package hr.algebra.parkalot;

import hr.algebra.parkalot.services.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for full-stack integration tests: the whole Spring context, MockMvc, and the
 * dedicated test database configured in src/test/resources/application.properties (PostGIS is required).
 * Each test runs in a transaction that is rolled back afterwards.
 * <p>
 * The AI feature is never exercised against the live provider; {@link AiService} is replaced by a mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.session.jdbc.initialize-schema=always")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    /** Stub for the Ollama-backed licence plate reader. */
    @MockitoBean
    protected AiService aiService;
}
