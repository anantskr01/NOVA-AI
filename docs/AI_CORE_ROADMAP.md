# NOVA AI Core Roadmap

## Current
- Android runtime coordinator
- Device WebSocket transport with reconnect
- Versioned node protocol
- Local deterministic agent planner
- Accessibility boundary for device actions

## Next
1. Replace placeholder planning with a provider-neutral LLM interface.
2. Add structured tool registry with explicit capability and permission checks.
3. Add persistent conversation/task context to `NovaMemory`.
4. Add central gateway/server so Android nodes do not need to host the AI brain.
5. Add desktop node using the same protocol.
6. Add screen-context and vision adapters.
7. Integrate the full NOVA MediaPipe gesture pipeline.
8. Add voice input/output and event-driven background jobs.
9. Add approval policy for sensitive actions.
10. Add integration tests for protocol, gateway reconnect, planning and action authorization.

## Design rule
The LLM proposes. The planner validates. The tool layer authorizes. The device node executes. Every remote action must remain capability- and permission-bound.
