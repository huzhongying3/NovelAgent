package com.hxs.novelagent.novelgraph.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.ai.openai.api-key=sk-TIEGRj7AxKWX9jJJjuAvQ5A2DEpPd9vXalo2QtMhqVBi1HFa",
                "spring.ai.openai.base-url=https://api.bltcy.ai",
                "spring.ai.openai.chat.options.model=gemini-3-flash-preview-nothinking",
                "spring.neo4j.uri=bolt://49.234.47.94:7687",
                "spring.neo4j.authentication.username=neo4j",
                "spring.neo4j.authentication.password=xk53fdab",
                "spring.datasource.url=jdbc:mysql://sh-cynosdbmysql-grp-gkf6f1w0.sql.tencentcdb.com:23479/noveagent-service?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true",
                "spring.datasource.username=root",
                "spring.datasource.password=xk53fdab@",
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"
        }
)
class NovelGraphControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void process_shouldRunPipelineWithRealServices() throws Exception {
        Path tempFile = Files.createTempFile("novelgraph-it", ".txt");
        Files.writeString(tempFile, "第一章 清晨，周威信护送鸳鸯刀进京，遇到太岳四侠拦路。");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/novelgraph/process"),
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {
                }
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Object processed = response.getBody().get("processedChunks");
        assertNotNull(processed);
        assertTrue(Integer.parseInt(processed.toString()) >= 0);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
