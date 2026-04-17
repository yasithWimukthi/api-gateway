package com.microservices.apigateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {

    private static final String SERVICE = "api-gateway";

    private final RestTemplate restTemplate;

    public String createOrder(String failure) {

        String requestId = UUID.randomUUID().toString();

        long start = System.currentTimeMillis();

        log.info("service={} requestId={} event=request_received",
                SERVICE, requestId);

        try {

            log.info("service={} requestId={} event=forward_to_order_service",
                    SERVICE, requestId);

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

        } catch (Exception e) {

            log.error("service={} requestId={} event=gateway_failure error={}",
                    SERVICE, requestId, e.getMessage());

            throw e;
        }

        long latency = System.currentTimeMillis() - start;

        if (latency > 1000) {
            log.warn("service={} requestId={} event=slow_response latency={}",
                    SERVICE, requestId, latency);
        }

        log.info("service={} requestId={} event=response_sent latency={}",
                SERVICE, requestId, latency);

        return "success";
    }
}