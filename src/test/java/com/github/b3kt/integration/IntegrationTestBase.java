package com.github.b3kt.integration;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

/**
 * Base for the tests that boot the application against a real database.
 *
 * <p>The database comes from {@link PostgresTestResource}, which starts the container and hands
 * Quarkus the JDBC url. This class deliberately does not declare a {@code @Container} of its own:
 * a second container would be started for every subclass and then never connected to, since the
 * datasource url always comes from the test resource.
 *
 * <p>The {@code quarkus} tag keeps these off the parallel unit-test fork - see the surefire
 * executions in pom.xml.
 */
@QuarkusTest
@Tag("quarkus")
@QuarkusTestResource(PostgresTestResource.class)
public abstract class IntegrationTestBase {

    @BeforeEach
    void setUp() {
        // Common test setup
    }
}
