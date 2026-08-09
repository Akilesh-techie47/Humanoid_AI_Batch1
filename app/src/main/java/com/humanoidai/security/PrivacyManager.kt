package com.humanoidai.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enforces data-handling policies.
 */
class PrivacyManager(private val auditLogger: AuditLogger) {

    private val _currentPolicy = MutableStateFlow(SecurityPolicyProfile.BALANCED)
    val currentPolicy: StateFlow<SecurityPolicyProfile> = _currentPolicy.asStateFlow()

    fun setPolicy(profile: SecurityPolicyProfile) {
        _currentPolicy.value = profile
        auditLogger.log(SecurityCategory.PRIVACY, "PolicyChanged", details = profile.name)
    }

    fun canUseCloudAI(): Boolean {
        return _currentPolicy.value.getConstraints().cloudInferenceAllowed
    }

    fun isLocalOnly(): Boolean {
        return _currentPolicy.value == SecurityPolicyProfile.MAXIMUM_PRIVACY
    }

    fun canExecuteThirdPartySkills(): Boolean {
        return _currentPolicy.value.getConstraints().thirdPartySkillsAllowed
    }
}
