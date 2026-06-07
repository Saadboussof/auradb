package com.auradb.telemetry.controller;

import com.auradb.telemetry.agent.TelemetryAgent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/simulate")
public class CrashController {

    private final TelemetryAgent telemetryAgent;

    public CrashController(TelemetryAgent telemetryAgent) {
        this.telemetryAgent = telemetryAgent;
    }

    @PostMapping("/crash/{instanceId}")
    public ResponseEntity<String> crashNode(@PathVariable String instanceId) {
        telemetryAgent.simulateCrash(instanceId);
        return ResponseEntity.ok("Node " + instanceId + " crashed successfully.");
    }
    
    @PostMapping("/recover/{instanceId}")
    public ResponseEntity<String> recoverNode(@PathVariable String instanceId) {
        telemetryAgent.recoverNode(instanceId);
        return ResponseEntity.ok("Node " + instanceId + " recovered successfully.");
    }
}
