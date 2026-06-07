package com.auradb.telemetry.processor;

import com.auradb.common.model.DbMetric;
import com.auradb.common.model.NodeAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TelemetryProcessor {

    private static final Logger log = LoggerFactory.getLogger(TelemetryProcessor.class);
    private static final String METRICS_TOPIC = "db-metrics";
    private static final String ALERTS_TOPIC = "node-alerts";

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, NodeAlert> kafkaTemplate;

    public TelemetryProcessor(StringRedisTemplate redisTemplate, KafkaTemplate<String, NodeAlert> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = METRICS_TOPIC, groupId = "telemetry-processor-group")
    public void processMetric(DbMetric metric) {
        log.debug("PROCESSOR: Received metric from {}", metric.instanceId());

        boolean isDegraded = metric.cpuUsage() > 90.0 || metric.memoryUsage() > 95.0 || metric.latencyMs() > 500;
        String currentStatus = isDegraded ? "DEGRADED" : "HEALTHY";
        
        String redisKey = "health:" + metric.instanceId();
        
        // Retrieve previous state to detect state changes
        String previousStatus = redisTemplate.opsForValue().get(redisKey);
        
        // Always update the latest status in Redis so other services (like Gateway) can check it
        redisTemplate.opsForValue().set(redisKey, currentStatus);

        // If the node JUST crashed, emit a critical alert to Kafka
        if ("DEGRADED".equals(currentStatus) && !"DEGRADED".equals(previousStatus)) {
            log.error("💥 PROCESSOR: DETECTED CRASH on {}! CPU is {}%. Emitting Alert to '{}' topic...", 
                    metric.instanceId(), metric.cpuUsage(), ALERTS_TOPIC);
                    
            NodeAlert alert = NodeAlert.create(metric.instanceId(), currentStatus);
            kafkaTemplate.send(ALERTS_TOPIC, metric.instanceId(), alert);
        } else if ("HEALTHY".equals(currentStatus) && "DEGRADED".equals(previousStatus)) {
            log.info("✅ PROCESSOR: Node {} has recovered! Emitting Alert to '{}' topic...", 
                    metric.instanceId(), ALERTS_TOPIC);
                    
            NodeAlert alert = NodeAlert.create(metric.instanceId(), currentStatus);
            kafkaTemplate.send(ALERTS_TOPIC, metric.instanceId(), alert);
        }
    }
}
