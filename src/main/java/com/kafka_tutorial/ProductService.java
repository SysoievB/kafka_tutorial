package com.kafka_tutorial;

import com.shared_core_library.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public String createProduct(CreateProductDto createProductDto) {
        return Optional.of(UUID.randomUUID().toString())
                .map(uuid -> new ProductCreatedEvent(
                        uuid,
                        createProductDto.title(),
                        createProductDto.price(),
                        createProductDto.quantity()
                ))
                .map(productCreatedEvent -> {
                    try {
                        log.info("*************Before creating product event: {}*************", productCreatedEvent.toString());

                        ProducerRecord<String, ProductCreatedEvent> record = new ProducerRecord<>(
                                "product-created-event-topic",
                                productCreatedEvent.getProductId(),
                                productCreatedEvent
                        );
                        record.headers().add("messageId", UUID.randomUUID().toString().getBytes());

                        SendResult<String, ProductCreatedEvent> result = kafkaTemplate.send(record).get();

                        log.info("*************Partition: {}*************", result.getRecordMetadata().partition());
                        log.info("*************Topic: {}*************", result.getRecordMetadata().topic());
                        log.info("*************Offset: {}*************", result.getRecordMetadata().offset());
                        log.info("*************Returning product ID*************");
                        return productCreatedEvent.getProductId();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("************* Error while sending create product event: {} *************", e.getMessage());

                        throw new RuntimeException("Kafka send failed: " + e.getMessage(), e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("Failed to create product"));
    }
}
