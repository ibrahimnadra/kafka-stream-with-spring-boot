package com.nashtech.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nashtech.order_service.config.AppConstants;
import com.nashtech.order_service.entities.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private KafkaTemplate<String, Order> kafkaTemplate;

    @InjectMocks
    private OrderController orderController;

    @Test
    void createOrder_ShouldReturnSuccess() throws Exception {
        // Given
        Order order = new Order("ord-123", "user-256", 300);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order posted successfully"));

        // Verify Kafka interaction
        verify(kafkaTemplate, times(1)).send(AppConstants.ORDER_TOPIC_NAME, order.orderId(), order);
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenInvalidJson() throws Exception {
        // Given
        String invalidJson = "{\"orderId\": }"; // Invalid JSON

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        // Verify Kafka is not called
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(Order.class));
    }
}