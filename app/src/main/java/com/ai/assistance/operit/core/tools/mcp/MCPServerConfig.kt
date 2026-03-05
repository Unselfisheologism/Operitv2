package com.ai.assistance.operit.core.tools.mcp

import kotlinx.serialization.Serializable

/** Transport type for MCP Server */
@Serializable
enum class MCSTransportType {
    REMOTE,    // HTTP/SSE endpoint
    STDIO      // Local process via STDIO
}

/** Configuration class for MCP Servers */
@Serializable
data class MCPServerConfig(
        val name: String,
        val endpoint: String = "",
        val description: String = "",
        val capabilities: List<String> = emptyList(),
        val extraData: Map<String, String> = emptyMap(),
        // STDIO-specific configuration
        val transportType: MCSTransportType = MCSTransportType.REMOTE,
        val command: String = "",
        val args: List<String> = emptyList()
)
