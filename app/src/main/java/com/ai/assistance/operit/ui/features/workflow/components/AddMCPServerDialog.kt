package com.ai.assistance.operit.ui.features.workflow.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.core.tools.mcp.MCSTransportType

/**
 * Dialog for adding a new MCP server to the workflow
 * Supports both STDIO (local) and Remote (HTTP/SSE) transport types
 */
@Composable
fun AddMCPServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (serverName: String, transportType: MCSTransportType, endpoint: String, command: String, args: String) -> Unit
) {
    var serverName by remember { mutableStateOf("") }
    var transportType by remember { mutableStateOf(MCSTransportType.REMOTE) }
    var endpoint by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var args by remember { mutableStateOf("") }
    
    var serverNameError by remember { mutableStateOf(false) }
    var endpointError by remember { mutableStateOf(false) }
    var commandError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workflow_mcp_add_server)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Server Name
                OutlinedTextField(
                    value = serverName,
                    onValueChange = {
                        serverName = it
                        serverNameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.workflow_mcp_server_name)) },
                    placeholder = { Text(stringResource(R.string.workflow_mcp_server_name_hint)) },
                    isError = serverNameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Transport Type Selection
                Text(
                    text = stringResource(R.string.workflow_mcp_transport_type),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // STDIO option
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = transportType == MCSTransportType.STDIO,
                                onClick = { transportType = MCSTransportType.STDIO },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = transportType == MCSTransportType.STDIO,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.workflow_mcp_transport_stdio))
                    }

                    // Remote option
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = transportType == MCSTransportType.REMOTE,
                                onClick = { transportType = MCSTransportType.REMOTE },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = transportType == MCSTransportType.REMOTE,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.workflow_mcp_transport_remote))
                    }
                }

                // Conditional fields based on transport type
                if (transportType == MCSTransportType.REMOTE) {
                    // Server Endpoint (for remote)
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = {
                            endpoint = it
                            endpointError = it.isBlank()
                        },
                        label = { Text(stringResource(R.string.workflow_mcp_server_endpoint)) },
                        placeholder = { Text(stringResource(R.string.workflow_mcp_server_endpoint_hint)) },
                        isError = endpointError,
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Command (for STDIO)
                    OutlinedTextField(
                        value = command,
                        onValueChange = {
                            command = it
                            commandError = it.isBlank()
                        },
                        label = { Text(stringResource(R.string.workflow_mcp_command)) },
                        placeholder = { Text(stringResource(R.string.workflow_mcp_command_hint)) },
                        isError = commandError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Args (for STDIO)
                    OutlinedTextField(
                        value = args,
                        onValueChange = { args = it },
                        label = { Text(stringResource(R.string.workflow_mcp_args)) },
                        placeholder = { Text(stringResource(R.string.workflow_mcp_args_hint)) },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    serverNameError = serverName.isBlank()
                    
                    val hasError = if (transportType == MCSTransportType.REMOTE) {
                        endpointError = endpoint.isBlank()
                        endpointError
                    } else {
                        commandError = command.isBlank()
                        commandError
                    }

                    if (!serverNameError && !hasError) {
                        onConfirm(
                            serverName.trim(), 
                            transportType, 
                            endpoint.trim(), 
                            command.trim(), 
                            args.trim()
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
}
