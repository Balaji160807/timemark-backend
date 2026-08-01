package com.timemark.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.timemark.entity.AttendanceStatus;
import com.timemark.entity.Employee;
import com.timemark.entity.LeaveStatus;
import com.timemark.repository.AttendanceRepository;
import com.timemark.repository.EmployeeRepository;
import com.timemark.repository.LeaveRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Note: this calls the real OpenAI REST API (https://api.openai.com/v1/chat/completions).
 * It's written to OpenAI's documented, stable contract, but hasn't been exercised against
 * a live key in the environment this was built in (no outbound network access to
 * api.openai.com there). Test it against your own OPENAI_API_KEY before relying on it.
 */
@Slf4j
@Service
public class AiInsightsService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.model}")
    private String model;

    public AiInsightsService(EmployeeRepository employeeRepository,
                              AttendanceRepository attendanceRepository,
                              LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public String teamInsight() {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI insights aren't configured yet. Set the OPENAI_API_KEY environment " +
                    "variable to enable natural-language summaries of attendance and leave patterns.";
        }

        String dataSummary = buildDataSummary();
        String prompt = "You're an HR assistant. Based on this attendance/leave data for today, " +
                "write a short (3-4 sentence) plain-English summary highlighting anything HR should " +
                "notice — patterns of lateness, absences, or pending leave requests. Be concise and " +
                "specific, using the numbers given. Data:\n\n" + dataSummary;

        try {
            return callOpenAi(prompt);
        } catch (Exception e) {
            log.warn("AI insight request failed: {}", e.getMessage());
            return "Couldn't reach the AI insights service right now (" + e.getMessage() + "). " +
                    "Try again shortly, or check that OPENAI_API_KEY is valid.";
        }
    }

    private String buildDataSummary() {
        LocalDate today = LocalDate.now();
        List<Employee> employees = employeeRepository.findAll();
        long present = attendanceRepository.findByDate(today).stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long late = attendanceRepository.findByDate(today).stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long checkedInTotal = attendanceRepository.findByDate(today).size();
        long pendingLeave = leaveRequestRepository.findByStatus(LeaveStatus.PENDING).size();

        return String.format(
                "Total employees: %d. Checked in today: %d (present: %d, late: %d). " +
                        "Not yet checked in: %d. Pending leave requests: %d.",
                employees.size(), checkedInTotal, present, late,
                employees.size() - checkedInTotal, pendingLeave
        );
    }

    private String callOpenAi(String prompt) throws Exception {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);

        ArrayNode messages = mapper.createArrayNode();
        messages.add(message);

        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.set("messages", messages);
        body.put("temperature", 0.4);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("OpenAI API returned status " + response.statusCode());
        }

        JsonNode json = mapper.readTree(response.body());
        return json.path("choices").path(0).path("message").path("content").asText("(empty response)").trim();
    }
}
