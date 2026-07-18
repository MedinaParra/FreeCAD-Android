package com.medinaparra.freecadandroid.nativebridge

data class MacroExecutionResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long
)
