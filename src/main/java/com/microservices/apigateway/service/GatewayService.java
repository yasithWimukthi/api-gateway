package com.microservices.apigateway.service;

import com.microservices.apigateway.model.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {

    private final LogSender logSender;
    private final RestTemplate restTemplate;

    private static final String SERVICE = "api-gateway";

    public String createOrder(String failure, int duration) {

        String requestId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();

        // =========================
        // REQUEST RECEIVED
        // =========================
        sendLog("INFO", "request_received", requestId, null, null);

        boolean success = false;

        try {

            // =========================
            // FORWARD TO ORDER SERVICE
            // =========================
            sendLog("INFO", "forward_to_order_service", requestId, null, null);

            String url = "http://localhost:8082/orders";

            if (failure != null) {
                url += "?failure=" + failure + "&duration=" + duration;
            }

            long orderStart = System.currentTimeMillis();

            // 🔁 Retry logic
            int retries = 0;
            while (retries < 2) {
                try {
                    restTemplate.postForObject(url, null, String.class);
                    success = true;
                    break;

                } catch (Exception ex) {

                    retries++;

                    sendLog("WARN", "order_retry_attempt",
                            requestId,
                            null,
                            "Retry attempt " + retries
                    );

                    Thread.sleep(200);
                }
            }

            long orderLatency = System.currentTimeMillis() - orderStart;

            sendLog("INFO", "order_service_latency",
                    requestId,
                    orderLatency,
                    null
            );

            if (success) {
                sendLog("INFO", "order_success", requestId, null, null);
            } else {
                sendLog("ERROR", "order_failure",
                        requestId,
                        orderLatency,
                        "Order failed after retries"
                );
            }

        } catch (Exception e) {

            sendLog("ERROR", "gateway_failure",
                    requestId,
                    null,
                    e.getMessage()
            );

            sendLog("ERROR", "request_completed_failure",
                    requestId,
                    null,
                    e.getMessage()
            );

            throw new RuntimeException(e);
        }

        // =========================
        // TOTAL LATENCY
        // =========================
        long latency = System.currentTimeMillis() - start;

        if (latency > 1000) {
            sendLog("WARN", "slow_response",
                    requestId,
                    latency,
                    "Gateway response slow"
            );
        }

        // =========================
        // RANDOM DEGRADATION SIGNAL
        // =========================
        if (Math.random() < 0.2) {
            sendLog("WARN", "gateway_degradation",
                    requestId,
                    null,
                    "Observed slight degradation"
            );
        }

        // =========================
        // FINAL STATUS
        // =========================
        if (success) {
            sendLog("INFO", "request_completed_success",
                    requestId,
                    latency,
                    null
            );
        } else {
            sendLog("ERROR", "request_completed_failure",
                    requestId,
                    latency,
                    "Final failure at gateway"
            );
        }

        sendLog("INFO", "response_sent",
                requestId,
                latency,
                null
        );

        return success ? "success" : "failed";
    }

    // =========================
    // LOG HELPER
    // =========================
    private void sendLog(String level,
                         String event,
                         String requestId,
                         Long latency,
                         String error) {

        logSender.send(new LogEvent(
                Instant.now().toString(),
                SERVICE,
                level,
                event,
                requestId,
                latency,
                error
        ));
    }
}