package com.microservices.apigateway.controller;

import com.microservices.apigateway.service.GatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;

    @PostMapping("/create-order")
    public ResponseEntity<String> createOrder(
            @RequestParam(required = false) String failure,
            @RequestParam(required = false, defaultValue = "100") int duration
    ) {
        return ResponseEntity.ok(gatewayService.createOrder(failure, duration));
    }
}