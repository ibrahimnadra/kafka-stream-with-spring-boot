package com.nashtech.fraud_detection_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nashtech.fraud_detection_service.entities.Order;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class KafkaStreamsConfigTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> outputTopic;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        KafkaStreamsConfig config = new KafkaStreamsConfig();
        StreamsBuilder builder = new StreamsBuilder();
        config.kStream(builder);

        Topology topology = builder.build();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-fraud-detection");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        testDriver = new TopologyTestDriver(topology, props);
        inputTopic = testDriver.createInputTopic(AppConstants.ORDER_TOPIC_NAME,
                Serdes.String().serializer(),
                Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("suspicious-orders",
                Serdes.String().deserializer(),
                Serdes.String().deserializer());
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    void shouldFilterSuspiciousOrder_WhenAmountGreaterThan1000() throws Exception {
        // Given
        Order suspiciousOrder = new Order("ord-123", "user-456", 2000);
        String orderJson = objectMapper.writeValueAsString(suspiciousOrder);

        // When
        inputTopic.pipeInput("ord-123", orderJson);

        // Then
        assertFalse(outputTopic.isEmpty(), "Output topic should have suspicious order");
        TestRecord<String, String> output = outputTopic.readRecord();
        assertNotNull(output);
        assertEquals("ord-123", output.key());
        assertEquals(orderJson, output.value());
    }

    @Test
    void shouldNotFilterNormalOrder_WhenAmountLessThanOrEqual1000() throws Exception {
        // Given
        Order normalOrder = new Order("ord-456", "user-456", 600);
        String orderJson = objectMapper.writeValueAsString(normalOrder);

        // When
        inputTopic.pipeInput("ord-456", orderJson);

        // Then
        assertTrue(outputTopic.isEmpty(), "Output topic should be empty for normal orders");
    }

    @Test
    void shouldNotFilterBorderlineOrder_WhenAmountExactly1000() throws Exception {
        // Given
        Order borderlineOrder = new Order("ord-789", "user-456", 1000);
        String orderJson = objectMapper.writeValueAsString(borderlineOrder);

        // When
        inputTopic.pipeInput("ord-789", orderJson);

        // Then
        assertTrue(outputTopic.isEmpty(), "Output topic should be empty for normal orders");

    }

    @Test
    void shouldHandleInvalidJson_WithoutCrashing() {
        // Given
        String invalidJson = "{invalid json}";

        // When
        inputTopic.pipeInput("ord-INVALID", invalidJson);

        // Then
        assertTrue(outputTopic.isEmpty(), "Output topic should be empty for invalid JSON");
    }

    @Test
    void shouldProcessMultipleOrders_FilterOnlySuspicious() throws Exception {
        // Given
        Order normalOrder = new Order("ord-001", "user-406", 1000);
        Order suspiciousOrder1 = new Order("ord-002", "user-456", 2000);
        Order suspiciousOrder2 = new Order("ord-003", "user-776", 9000);

        // When
        inputTopic.pipeInput("ord-001", objectMapper.writeValueAsString(normalOrder));
        inputTopic.pipeInput("ord-002", objectMapper.writeValueAsString(suspiciousOrder1));
        inputTopic.pipeInput("ord-003", objectMapper.writeValueAsString(suspiciousOrder2));

        // Then
        assertFalse(outputTopic.isEmpty(), "Output topic should contain suspicious orders");
        TestRecord<String, String> output1 = outputTopic.readRecord();
        TestRecord<String, String> output2 = outputTopic.readRecord();

        assertEquals("ord-002", output1.key());
        assertEquals("ord-003", output2.key());
        assertTrue(outputTopic.isEmpty(), "No more records should be present");
    }
}