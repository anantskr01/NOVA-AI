# NOVA-AI

Cross-device personal AI platform for Android phones/tablets and desktop nodes.

## Current foundation

- Android app module (Java)
- Permission-controlled Accessibility observation/execution boundary
- Local action router
- Local memory cache
- WebSocket device gateway protocol
- MediaPipe Tasks Vision dependency ready for the NOVA hand/vision layer

## Architecture

```text
                    NOVA CORE
               AI + Agent + Memory
                        |
                  NOVA GATEWAY
                        |
          +-------------+-------------+
          |             |             |
       Android        Tablet        Desktop
          |             |             |
    Accessibility   Vision/Gesture  Device Node
    Camera/Voice    Camera/Voice    Files/Apps
```

## Integration sources

**NOVA source:** Android assistant, action engine, planner, memory, skills, voice and MediaPipe gesture pipeline from `anantskr01/NOVA`.

**Jarvis OS source/reference:** agent/runtime, gateway, device-node, screen-context, jobs, approvals and cross-device architecture from `battlesbudz/jarvis-os`.

## Engineering rule

Use the strongest implementation for each component. Avoid duplicate subsystems and keep Android permissions as the final authority for device actions.

## Status

Bootstrap + Android foundation. The next stage is importing the proven NOVA gesture pipeline and building the central agent/gateway runtime.
