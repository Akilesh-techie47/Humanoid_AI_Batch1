# Implementation Plan - Phase 6B: Distributed Intelligence & Multi-Agent Coordination Framework

Enable multiple AI instances (Phone, Robot, Glasses) to cooperate, share context, and synchronize state under a single AI identity.

## User Review Required

> [!IMPORTANT]
> This framework allows your Humanoid AI to "jump" between devices. For example, if you are wearing AR glasses, the AI can use the glasses' camera while processing the data on your phone and notifying you via a home hub.

## Proposed Changes

### Distributed Intelligence Core [NEW]

Create a new package `com.humanoidai.distributed`.

#### [NEW] [DistributedModels.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/distributed/DistributedModels.kt)
Defines `AIInstance`, `SharedWorldState`, and `CoordinationMessage`. Tracks remote instance metadata (Battery, Load, Capabilities).

#### [NEW] [InstanceRegistry.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/distributed/InstanceRegistry.kt)
Maintains a registry of all active and trusted AI instances across the local network/cloud.

#### [NEW] [CapabilityNegotiator.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/distributed/CapabilityNegotiator.kt)
Logic to decide which instance is best suited for a specific task (e.g., "Use Robot for locomotion, Phone for reasoning").

#### [NEW] [StateSynchronizer.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/distributed/StateSynchronizer.kt)
Handles incremental synchronization of the `SharedWorldState` between instances.

#### [NEW] [CoordinationManager.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/distributed/CoordinationManager.kt)
The central orchestrator for multi-agent logic. Routes tasks and merges remote context into the local engine.

---

### Integration with Existing Systems

#### [MODIFY] [GoalManager.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/planning/GoalManager.kt)
Support delegating `PlanStep`s to remote instances via the `CoordinationManager`.

#### [MODIFY] [WorldState.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/embodiment/WorldState.kt)
Add fields for `remoteInstances` and `identityId`.

#### [MODIFY] [LayoutCustomizationManager.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/ui/layoutcustomization/domain/manager/LayoutCustomizationManager.kt)
Initialize and hold the `CoordinationManager`.

---

### Developer Tools [NEW]

#### [NEW] [CoordinationInspectorViewModel.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/distributed/ui/CoordinationInspectorViewModel.kt)
#### [NEW] [CoordinationInspectorScreen.kt](file:///D:/Projects(ASC)/app/src/main/java/com/humanoidai/distributed/ui/CoordinationInspectorScreen.kt)
A visualization tool for connected devices, task assignments, and message flow. Includes a simulator to "mock" remote agents (e.g., a Drone or Smart Glasses).

## Verification Plan

### Automated Tests
- Unit tests for `CapabilityNegotiator` to verify optimal executor selection.
- Unit tests for `StateSynchronizer` to handle state merging and conflict resolution.
- Simulation of a remote instance disconnect and later reconciliation.

### Manual Verification
- Deploy to device.
- Use the **Coordination Inspector** to add a "Mock Robot" instance.
- Trigger a goal and verify (via logs/inspector) that certain steps are assigned to the mock robot based on its reported capabilities.
- Observe synchronization of world state (e.g., "Mock Robot" detects an object, and it appears in the local world state).
