package pl.dudios.planner.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pl.dudios.planner.integration.fixtures.TestAuthentication;
import pl.dudios.planner.integration.fixtures.TestFixtures;
import tools.jackson.databind.ObjectMapper;

/**
 * Common base for every integration test.
 *
 * <p>One PostgreSQL container is shared by the whole JVM run and the schema comes from the real
 * Flyway migrations. Every test starts from empty tables ({@code reset-db.sql}) and builds the data
 * it needs through {@link TestFixtures}. There is no class-wide {@code @Transactional}, so tests
 * observe the same transaction boundaries as production.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/sql/reset-db.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class AbstractIntegrationTest {

  @SuppressWarnings("resource")
  protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
    .withDatabaseName("planner")
    .withUsername("test")
    .withPassword("test");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void overrideDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected TestFixtures fixtures;

  @Autowired
  protected TestAuthentication auth;

}
