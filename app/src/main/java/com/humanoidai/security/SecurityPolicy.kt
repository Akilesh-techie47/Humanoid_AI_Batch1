package com.humanoidai.security

/**
 * High-level security profiles that govern system-wide data handling.
 */
enum class SecurityPolicyProfile {
    /**
     * Local-only AI. No network access for skills or inference.
     */
    MAXIMUM_PRIVACY,

    /**
     * Local-first. Cloud permitted with explicit consent. (Default)
     */
    BALANCED,

    /**
     * Cloud AI allowed. Priority on capability and performance.
     */
    CLOUD_ENHANCED
}

data class PolicyConstraints(
    val cloudInferenceAllowed: Boolean,
    val thirdPartySkillsAllowed: Boolean,
    val anonymousTelemetryEnabled: Boolean,
    val locationAccessAllowed: Boolean,
    val autoPurgeTransientData: Boolean
)

fun SecurityPolicyProfile.getConstraints(): PolicyConstraints = when (this) {
    SecurityPolicyProfile.MAXIMUM_PRIVACY -> PolicyConstraints(
        cloudInferenceAllowed = false,
        thirdPartySkillsAllowed = false,
        anonymousTelemetryEnabled = false,
        locationAccessAllowed = false,
        autoPurgeTransientData = true
    )
    SecurityPolicyProfile.BALANCED -> PolicyConstraints(
        cloudInferenceAllowed = true,
        thirdPartySkillsAllowed = true,
        anonymousTelemetryEnabled = true,
        locationAccessAllowed = true,
        autoPurgeTransientData = true
    )
    SecurityPolicyProfile.CLOUD_ENHANCED -> PolicyConstraints(
        cloudInferenceAllowed = true,
        thirdPartySkillsAllowed = true,
        anonymousTelemetryEnabled = true,
        locationAccessAllowed = true,
        autoPurgeTransientData = false
    )
}
