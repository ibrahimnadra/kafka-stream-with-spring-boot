package com.nashtech.fraud_detection_service.config;

import com.nashtech.fraud_detection_service.entities.Order;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean
    public KStream<String, String> kStream(StreamsBuilder builder) {

        ObjectMapper objectMapper = new ObjectMapper();

        // Consuming the "orders" topic with String key and String value
        KStream<String, String> stream = builder.stream(AppConstants.ORDER_TOPIC_NAME, Consumed.with(Serdes.String(), Serdes.String()));


        // Processing the stream
        stream.peek((key, value) -> System.out.println("Received record - Key: " + key + ", Value: " + value))
                .filter((key, value) -> {
                    try {
                        // Parse the value to Order object and check if the amount is greater than 1000
                        Order order = objectMapper.readValue(value, Order.class);
                        boolean isSuspicious = order.amount() > 1000;
                        System.out.println("Order " + key + " is " + (isSuspicious ? "suspicious" : "not suspicious"));
                        return isSuspicious;
                    } catch (Exception e) {
                        System.out.println("Error parsing JSON for key: " + key + ", error: " + e.getMessage());
                        return false;
                    }
                })
                .peek((key, value) -> System.out.println("Sending suspicious record - Key: " + key + ", Value: " + value))
                .to("suspicious-orders", Produced.with(Serdes.String(), Serdes.String()));

        return stream;
    }
}