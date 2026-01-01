package com.hxs.novelagent.worldgenesis.api;

import java.nio.file.Path;

import com.hxs.novelagent.worldgenesis.service.SeedDataService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.ai.openai.api-key=sk-TIEGRj7AxKWX9jJJjuAvQ5A2DEpPd9vXalo2QtMhqVBi1HFa",
                "spring.ai.openai.base-url=https://api.bltcy.ai",
                "spring.ai.openai.chat.options.model=gemini-3-flash-preview-nothinking",
                "spring.neo4j.uri=bolt://49.234.47.94:7687",
                "spring.neo4j.authentication.username=neo4j",
                "spring.neo4j.authentication.password=xk53fdab",
                "worldgenesis.default-data-file=records-15.json"
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorldGenesisGenerateIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SeedDataService seedDataService;


    @Test
    void generate_shouldReturnBible_withRealServices() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("""
                {"sampleSize":100,"output":""}
                """, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/worldgenesis/generate"),
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertThat(response.getBody(), Matchers.containsString("power_system"));
        assertThat(response.getBody(), Matchers.containsString("power_limitations"));
        assertThat(response.getBody(), Matchers.containsString("major_factions"));
        assertThat(response.getBody(), Matchers.containsString("specific_terminology"));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
