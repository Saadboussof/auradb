package com.auradb.telemetry.agent;

import com.auradb.common.model.DbMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryAgent {

    private static final Logger log = LoggerFactory.getLogger(TelemetryAgent.class);
    private static final String TOPIC = "db-metrics";

    private final KafkaTemplate<String, DbMetric> kafkaTemplate;
    
    // Tracks which nodes are currently "crashed" for simulation purposes
    private final ConcurrentHashMap<String, Boolean> crashedNodes = new ConcurrentHashMap<>();

    public TelemetryAgent(KafkaTemplate<String, DbMetric> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Runs every 10 seconds to simulate a database constantly emitting telemetry data
    @Scheduled(fixedRate = 10000)
    public void generateMetrics() {
        String instanceId = "db-inst-001"; // Simulating a single instance for Phase 4
        
        DbMetric metric;
        if (crashedNodes.getOrDefault(instanceId, false)) {
            metric = DbMetric.createCrashed(instanceId);
            log.warn("🚨 AGENT: Node {} is CRASHED! Emitting critical metric (CPU: {}%)", instanceId, metric.cpuUsage());
        } else {
            metric = DbMetric.createHealthy(instanceId);
            log.info("💓 AGENT: Node {} is HEALTHY. Emitting standard metric (CPU: {}%)", instanceId, metric.cpuUsage());
        }

        kafkaTemplate.send(TOPIC, instanceId, metric);
    }

    public void simulateCrash(String instanceId) {
        crashedNodes.put(instanceId, true);
        log.warn("🚨 SIMULATION TRIGGERED: Node {} has been forced into a crashed state!", instanceId);
    }
    
    public void recoverNode(String instanceId) {
        crashedNodes.put(instanceId, false);
        log.info("🏥 SIMULATION RECOVERED: Node {} has been recovered.", instanceId);
    }
}
