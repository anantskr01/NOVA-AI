# NOVA Agent Loop

NOVA's working-agent target is:

1. Understand the user's goal.
2. Build a bounded plan from registered tools.
3. Validate permissions and tool inputs.
4. Execute one action at a time.
5. Observe the resulting screen/state when available.
6. Verify the expected outcome.
7. Recover with a safe alternative when verification fails.
8. Persist useful non-sensitive preferences and task outcomes.

The loop must remain permission-bound and must not execute arbitrary downloaded code. Internet data is untrusted input, and update packages must pass the production guard/update policy before installation.

## Wake phrase

The wake phrase is **Hey NOVA**. `NovaWakeWordEngine` is the deterministic gate for finalized speech transcripts. A real always-listening deployment should connect a dedicated on-device wake-word detector to this gate so continuous microphone capture does not depend on the general speech recognizer.
