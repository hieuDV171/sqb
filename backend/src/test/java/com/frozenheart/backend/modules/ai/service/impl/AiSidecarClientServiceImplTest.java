package com.frozenheart.backend.modules.ai.service.impl;

import com.frozenheart.backend.core.config.AiSidecarConfig;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;
import com.frozenheart.backend.modules.ai.dto.BtpropSidecarResponse;
import com.frozenheart.backend.modules.ai.service.AiSidecarClientService;
import com.frozenheart.backend.modules.ai.service.BtpropHallucinationCheckerService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.ai.dto.BtpropSidecarRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiSidecarClientServiceImplTest {

    @Test
    void exchangesBtpropAuditUsingTheApplicationJacksonConfiguration() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(AiSidecarConfig.class)
                .withPropertyValues("aisidecar.url=http://sidecar.test")
                .run(context -> verifyBtpropAudit(context.getBean(RestClient.class)));
    }

    @Test
    void incompleteAuditPreservesEvidenceAndRequiresManualReview() {
        var sidecar = mock(AiSidecarClientService.class);
        var warning = new BtpropSidecarResponse.ViolationDto(
                "AMBIGUITY", "Điều kiện", "Thiếu điều kiện làm đổi đáp án", "MEDIUM");
        when(sidecar.auditQuestion(any()))
                .thenReturn(BtpropSidecarResponse.builder()
                        .status("incomplete").beliefTreeDepth(1).issues(List.of("Kiểm tra chưa hoàn tất"))
                        .violations(List.of(warning)).build());
        var checker = new BtpropHallucinationCheckerService(sidecar);
        var suggestion = AiRefineResponse.SuggestedQuestionDto.builder()
                .content("Câu hỏi kiểm thử").build();

        var audit = checker.audit(null, suggestion);

        assertEquals(1, audit.beliefTreeDepth());
        assertEquals(List.of("UNVERIFIED", "AMBIGUITY"), audit.violations().stream()
                .map(AiRefineResponse.ViolationDto::type).toList());
    }

    private void verifyBtpropAudit(RestClient restClient) {
        var builder = restClient.mutate();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sidecar.test/api/v1/ai/btprop/audit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.fast_mode").doesNotExist())
                .andExpect(jsonPath("$.options[0].is_correct").value(true))
                .andExpect(jsonPath("$.options[1].is_correct").value(false))
                .andExpect(jsonPath("$.options[0].media_id").value(19))
                .andExpect(jsonPath("$.options[0].isCorrect").doesNotExist())
                .andRespond(withSuccess("""
                        {
                          "is_hallucinated": true,
                          "confidence_score": 0.85,
                          "risk_score": 75.0,
                          "risk_label": "cao",
                          "belief_tree_depth": 2,
                          "violations": [{
                            "type": "WRONG_ANSWER",
                            "node_statement": "Phương án B",
                            "detail": "Cần kiểm tra lại",
                            "severity": "HIGH"
                          }],
                          "issues": [],
                          "likely_suitable_options": ["A"],
                          "status": "complete",
                          "mode": "live_openai"
                        }
                        """, MediaType.APPLICATION_JSON));

        var client = new AiSidecarClientServiceImpl(builder.build(), new MetricService(new SimpleMeterRegistry()));
        var request = BtpropSidecarRequest.builder()
                .content("Câu hỏi kiểm thử")
                .options(List.of(
                        BtpropSidecarRequest.OptionDto.builder().key("A").text("Đúng").isCorrect(true).mediaId(19L).build(),
                        BtpropSidecarRequest.OptionDto.builder().key("B").text("Sai").isCorrect(false).build()))
                .build();

        var audit = client.auditQuestion(request);

        assertNotNull(audit);
        assertTrue(audit.isHallucinated());
        assertEquals(0.85, audit.confidenceScore());
        assertEquals(2, audit.beliefTreeDepth());
        assertEquals("Phương án B", audit.violations().getFirst().nodeStatement());
        assertEquals(List.of("A"), audit.likelySuitableOptions());
        server.verify();
    }
}
