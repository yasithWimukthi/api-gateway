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

    private static final String SERVICE = "api-gateway";

    private final RestTemplate restTemplate;

    public String createOrder(String failure) {

        String requestId = UUID.randomUUID().toString();

        long start = System.currentTimeMillis();

        log.info("service={} requestId={} event=request_received",
                SERVICE, requestId);

        logSender.send(new LogEvent(
                Instant.now().toString(),
                "api-gateway",
                "INFO",
                "request_received",
                requestId,
                null,
                null
        ));


        try {

            log.info("service={} requestId={} event=forward_to_order_service",
                    SERVICE, requestId);

            logSender.send(new LogEvent(
                    Instant.now().toString(),
                    "api-gateway",
                    "INFO",
                    "forward_to_order_service",
                    requestId,
                    null,
                    null
            ));

            String url = "http://localhost:8082/orders";

            if (failure != null) {
                url += "?failure=" + failure;
            }

            restTemplate.postForObject(
                    url,
                    null,
                    String.class
            );

            log.info("service={} requestId={} event=order_success",
                    SERVICE, requestId);

            logSender.send(new LogEvent(
                    Instant.now().toString(),
                    "api-gateway",
                    "INFO",
                    "order_success",
                    requestId,
                    null,
                    null
            ));

        } catch (Exception e) {

            log.error("service={} requestId={} event=gateway_failure error={}",
                    SERVICE, requestId, e.getMessage());

            logSender.send(new LogEvent(
                    Instant.now().toString(),
                    "api-gateway",
                    "ERROR",
                    "gateway_failure",
                    requestId,
                    null,
                    e.getMessage()
            ));

            throw e;
        }

        long latency = System.currentTimeMillis() - start;

        if (latency > 1000) {
            log.warn("service={} requestId={} event=slow_response latency={}",
                    SERVICE, requestId, latency);

            logSender.send(new LogEvent(
                    Instant.now().toString(),
                    "api-gateway",
                    "WARN",
                    "slow_response",
                    requestId,
                    latency,
                    null
            ));
        }

        log.info("service={} requestId={} event=response_sent latency={}",
                SERVICE, requestId, latency);

        logSender.send(new LogEvent(
                Instant.now().toString(),
                "api-gateway",
                "INFO",
                "response_sent",
                requestId,
                latency,
                null
        ));

        return "success";
    }
}