# NOVA AI Architecture

## Integration strategy

NOVA-AI is a selective integration of the existing NOVA Android project and the Jarvis OS architecture.

### Keep from NOVA
- Android Accessibility execution boundary
- Camera/MediaPipe perception and continuous gesture-control pipeline
- NOVA assistant, action, skill and voice components when compatible
- Android-first UX/HUD

### Adopt from Jarvis OS
- Central agent/runtime separation
- Device gateway/node protocol
- Persistent device connections and reconnect strategy
- Screen-context abstraction
- Background jobs and approval/policy boundaries
- Cross-device identity and shared-memory direction

## Runtime layers

1. **NOVA Core** — reasoning, planning, memory and personality.
2. **Gateway** — routes events/actions between NOVA Core and device nodes.
3. **Device Node** — Android/desktop implementation of observation and execution.
4. **Perception** — screen, camera, gesture and voice inputs.
5. **Action boundary** — all device actions pass through explicit permission-controlled APIs.

## Android first

The first production target is an Android tablet/phone node. The node must remain useful offline for local actions while using the gateway for shared intelligence and cross-device tasks.

## Safety and reliability

Remote commands are treated as untrusted input. The Android node validates operation type and parameters before execution. Destructive or sensitive capabilities should require an explicit approval policy before they are enabled.
