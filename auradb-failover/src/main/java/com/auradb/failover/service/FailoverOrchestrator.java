package com.auradb.failover.service;

import com.auradb.common.model.NodeAlert;
import com.auradb.common.model.RouteInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FailoverOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FailoverOrchestrator.class);
    
    private final ReactiveRedisTemplate<String, RouteInfo> routeRedisTemplate;
    private final ReactiveStringRedisTemplate stringRedisTemplate;
    
    // Tracks degraded nodes and the timestamp they were first reported degraded
    private final ConcurrentHashMap<String, Instant> degradedNodes = new ConcurrentHashMap<>();

    public FailoverOrchestrator(ReactiveRedisTemplate<String, RouteInfo> routeRedisTemplate,
                                ReactiveStringRedisTemplate stringRedisTemplate) {
        this.routeRedisTemplate = routeRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @KafkaListener(topics = "node-alerts", groupId = "failover-orchestrator-group")
    public void handleNodeAlert(NodeAlert alert) {
        log.info("📢 ALERTS CONSUMER: Received alert for node '{}' | status: '{}'", alert.instanceId(), alert.status());
        
        if ("DEGRADED".equals(alert.status())) {
            // Put it in the tracking map if not already there, recording the start of degradation
            degradedNodes.putIfAbsent(alert.instanceId(), Instant.now());
            log.warn("⚠️ ALERTS CONSUMER: Node '{}' registered as DEGRADED. Monitoring failover threshold...", alert.instanceId());
        } else if ("HEALTHY".equals(alert.status())) {
            // Node recovered! Remove from tracking map
            if (degradedNodes.remove(alert.instanceId()) != null) {
                log.info("🏥 ALERTS CONSUMER: Node '{}' has recovered. Removed from failover tracking.", alert.instanceId());
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    public void checkFailovers() {
        Instant now = Instant.now();
        degradedNodes.forEach((instanceId, degradationStart) -> {
            long secondsDegraded = Duration.between(degradationStart, now).getSeconds();
            log.debug("🔍 SCHEDULER: Node '{}' has been degraded for {} seconds", instanceId, secondsDegraded);
            
            if (secondsDegraded >= 60) {
                triggerFailover(instanceId);
            }
        });
    }

    private void triggerFailover(String instanceId) {
        log.error("🚨 FAILOVER: Node '{}' has exceeded 60s degradation limit! Orchestrating automatic failover...", instanceId);

        // 1. Look up the tenant owner from Redis: owner:{instanceId}
        stringRedisTemplate.opsForValue().get("owner:" + instanceId)
                .flatMap(tenantId -> {
                    log.info("🔗 FAILOVER: Mapping found: Node '{}' -> Tenant '{}'", instanceId, tenantId);
                    
                    // 2. Retrieve the active route: route:{tenantId}
                    return routeRedisTemplate.opsForValue().get("route:" + tenantId)
                            .flatMap(routeInfo -> {
                                String fallback = routeInfo.fallbackEndpoint();
                                
                                if (fallback == null || fallback.isBlank()) {
                                    // 2.a Fallback to replica:{tenantId} string lookup if fallback in RouteInfo is empty
                                    return stringRedisTemplate.opsForValue().get("replica:" + tenantId)
                                            .flatMap(replicaUrl -> updateRoute(tenantId, routeInfo, replicaUrl, instanceId));
                                } else {
                                    return updateRoute(tenantId, routeInfo, fallback, instanceId);
                                }
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.error("❌ FAILOVER ERROR: No RouteInfo found for tenant '{}' under key 'route:{}'", tenantId, tenantId);
                                degradedNodes.remove(instanceId);
                                return Mono.empty();
                            }));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("❌ FAILOVER ERROR: No tenant owner found in Redis for node '{}' (key 'owner:{}')", instanceId, instanceId);
                    degradedNodes.remove(instanceId);
                    return Mono.empty();
                }))
                .subscribe(); // Subscribe to trigger execution of the reactive pipeline
    }

    private Mono<Void> updateRoute(String tenantId, RouteInfo routeInfo, String replicaUrl, String instanceId) {
        String active = routeInfo.activeEndpoint();
        
        // If route is already pointing to replicaUrl, do nothing and remove from tracking
        if (active.equals(replicaUrl)) {
            log.info("ℹ️ FAILOVER: Route for tenant '{}' is already pointing to the replica. No action needed.", tenantId);
            degradedNodes.remove(instanceId);
            return Mono.empty();
        }
        
        log.warn("🔄 FAILOVER PROGRESS: Rerouting tenant '{}' from '{}' -> replica '{}'", tenantId, active, replicaUrl);
        
        // Build new RouteInfo swapping active and fallback (the old active is now fallback)
        RouteInfo failedOverRoute = new RouteInfo(tenantId, replicaUrl, active, Instant.now());
        
        return routeRedisTemplate.opsForValue().set("route:" + tenantId, failedOverRoute)
                .doOnSuccess(success -> {
                    log.error("🎉 FAILOVER COMPLETE: Tenant '{}' successfully routed to replica '{}'", tenantId, replicaUrl);
                    degradedNodes.remove(instanceId);
                })
                .then();
    }
}
