# MCP 插件加载与导入指南（Operit）

本文说明 Operit 当前的 MCP（Model Context Protocol）加载方式、文件应如何放置、运行原理，以及用户手动添加/自动导入 MCP 的正确姿势。

## 1. MCP 在 Operit 里是什么

在 Operit 中，MCP server 被当作“插件/服务”管理：

- MCP server 对外提供 tools/resources（目前主要用 tools）。
- Operit 通过一个 NodeJS “桥接器（Bridge）”来统一管理 MCP 服务的注册、启动、停止、查询工具列表、调用工具。
- Operit 内部把每个 MCP 工具注册为 `pluginId:toolName` 的形式，供聊天侧的工具调用系统使用。

你可以把它理解成：

- **配置层**：描述有哪些 MCP server（怎么启动/怎么连接/启用禁用）。
- **Bridge 层**：真正负责把这些 server 作为进程/远程连接跑起来，并提供统一 TCP API。
- **工具层**：把 MCP tools 映射成应用内部的 Tool，让模型可以调用。

## 2. 文件应该怎么放（Android 端目录结构）

### 2.1 默认目录（最关键）

`MCPLocalServer` 把 MCP 的配置和插件（仓库 ZIP 解压结果）统一放在：

- **Download/Operit/mcp_plugins/**

其中最重要的两个文件是：

- **`mcp_config.json`**：MCP 配置（兼容官方 MCP 配置结构，核心字段是 `mcpServers`）
- **`server_status.json`**：运行状态 + 工具缓存（加速下次启动）

目录示例：

```text
Download/
  Operit/
    mcp_plugins/
      mcp_config.json
      server_status.json
      playwright/                  # 一个“物理安装”的插件目录（可选）
      my_local_mcp/                # 另一个插件目录（可选）
```

### 2.2 插件目录到底放什么

取决于这个 MCP server 的 `command` 类型：

- **不需要物理安装**：`command` 是 `npx` / `uvx` / `uv`（以及远程 `remote`）
  - 这种情况下，Android 端可以没有对应插件目录。
  - 你只需要把配置写进 `mcp_config.json`，然后启用并启动即可。

- **需要物理安装**：其它 `command`（例如 `python` / `node` / 运行某个脚本等）
  - 需要在 `Download/Operit/mcp_plugins/<pluginId>/...` 下有项目文件。
  - Operit 会在“部署（deploy）”阶段把这些文件复制到终端/容器环境（Linux）下的 `~/mcp_plugins/<pluginShortName>`，然后在该目录启动服务。

> 物理安装识别：代码里会检查插件目录是否存在且包含一些典型文件（例如 `README.md`、`package.json`、`main.py`、`index.js` 等），目录为空会被认为“未安装”。

### 2.3 如果默认目录不可写会怎样

`MCPRepository` 会优先使用 `Download/Operit/mcp_plugins/`，如果该目录不可写，会退回到应用私有外部目录（日志里会打印 `使用应用私有目录: ...`）。

实际以应用 UI 提示/打开的配置文件路径为准（见下文“验证与排查”）。

## 3. 加载与运行原理（代码层）

### 3.1 配置从哪里加载

- `MCPLocalServer` 在初始化时读取 `mcp_config.json` 和 `server_status.json`。
- `mcp_config.json` 的结构核心是：
  - `mcpServers`: `{ [serverId: string]: { command, args, env, autoApprove, disabled } }`
  - `pluginMetadata`: `{ [serverId: string]: { name, description, type, endpoint, ... } }`

重要行为：

- 如果你只写了 `mcpServers`，没有写 `pluginMetadata`，`MCPLocalServer` 会 **自动补齐缺失的元数据**（便于 UI 展示与后续管理）。

### 3.2 启动时如何“加载 MCP 插件”

应用启动时（以及启动页插件加载流程中），会走：

- `PluginLoadingState.initializeMCPServer()`
  - 读取“已安装 + 已启用”的插件列表
  - 调用 `MCPStarter.startAllDeployedPlugins()` 批量注册/启动/验证

`MCPStarter` 的核心职责：

- 初始化并启动 Bridge（必要时会把 bridge 文件部署到终端环境）
- 对每个插件：
  - **本地插件**：必要时自动部署（把 Android 端目录复制到 Linux 的 `~/mcp_plugins/...`）
  - 调用 Bridge 的 `register` 注册服务
  - 根据需要 `spawn` 启动服务
  - `ping` 验证服务是否可用
  - 拉取 tools 列表并缓存（写入 `server_status.json`）

### 3.3 工具是怎么注册与调用的

- 验证成功后，`MCPRepository.registerToolsForLoadedPlugins()` 会将工具注册到 `AIToolHandler`。
- 每个工具的名字会被加前缀：
  - **`<pluginId>:<toolName>`**
- 执行时由 `MCPToolExecutor` 解析出 `pluginId` 和 `toolName`，并通过 `MCPManager -> MCPBridgeClient` 调用 Bridge。

注意：

- `MCPToolExecutor` 在真正调用前会检查服务是否处于 active（运行/已激活）。
- 若未激活，它会提示需要先用 `use_package` 激活对应包名（包名就是 `pluginId`）。
  - 实际上你也可以在 MCP 管理界面手动启动对应插件（效果等价）。

## 4. 用户手动添加 MCP：怎么做（推荐流程）

这里给两条路线：

- **只加配置（最简单，适合 npx/uvx/uv）**
- **加本地项目文件 + 配置（适合 python/node 项目）**

### 4.1 方式 A：只添加配置（npx/uvx/uv 类型，推荐）

适用场景：例如 Playwright MCP（`npx @playwright/mcp@latest`）这类“一条命令就能跑”的 server。

步骤：

1. 打开 MCP 配置界面
2. 打开“导入/连接”对话框
3. 选择“配置导入”
4. 粘贴 JSON（需要包含 `mcpServers`）并点击“合并配置”
5. 返回列表后，刷新/重启插件加载，确保该服务已启用并启动

示例配置（你可以按需改 serverId 和 args）：

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@playwright/mcp@latest"],
      "env": {},
      "autoApprove": []
    }
  }
}
```

要点：

- `mcpServers` 的 key（这里是 `playwright`）在 Operit 里会成为 **`pluginId`**。
- 建议只用：字母/数字/下划线，并尽量小写（与 UI 的插件名约束一致）。

### 4.2 方式 B：本地项目型 MCP（需要放文件）

适用场景：你自己写的 MCP server、或某个 GitHub 仓库的 MCP 项目，需要 `python -m xxx` / `node dist/index.js` 等方式启动。

推荐做法：

- 直接用 UI 的“从仓库导入”或“从 ZIP 导入”，让 Operit 帮你把仓库下载/解压到正确目录。

如果你坚持手动放文件：

1. 在手机文件管理器里创建目录：
   - `Download/Operit/mcp_plugins/<pluginId>/`
2. 把项目文件放进去（至少要有一些可识别文件，例如 `package.json` / `main.py` 等，目录不要空）
3. 用“方式 A”把对应的 `mcpServers` 配置合并进 `mcp_config.json`
4. 在 MCP 管理界面执行“部署/启动”（或重启后等待自动部署/启动）

重点解释：

- **Android 端目录只是“来源”**。
- 真正运行时，Operit 会把它复制到 **Linux/终端环境**：`~/mcp_plugins/<pluginShortName>`。
- 启动服务时 Bridge 会设置 `cwd=~/mcp_plugins/<pluginShortName>`。

因此你的 `args` 最好使用相对路径或与该工作目录匹配的路径。

### 4.3 方式 C：连接远程 MCP 服务（remote）

适用场景：你在电脑/服务器上已经跑好了 MCP server，手机只需要连接。

步骤（UI）：

1. MCP 配置界面 -> 导入/连接
2. 选择“连接远程服务”
3. 填写：
   - Host 地址（例如 `http://127.0.0.1:8752`）
   - Connection Type（`httpStream` 或 `sse`）
   - 可选 Bearer Token
4. 保存后启用并启动

说明：

- 远程服务仍然会通过 Bridge 统一管理与调用。
- UI 里创建远程服务时，`pluginId` 通常等于你填写的插件名（会自动小写），尽量不要后续再改显示名以免混淆。

## 5. 自动导入 MCP：怎么做

### 5.1 从 MCP 市场自动安装

市场安装（`MCPMarketViewModel.installMCPFromIssue`）有两种分支：

- **配置合并安装（免物理安装）**
  - 如果市场给的 `installConfig` 里所有 server 的 `command` 都是 `npx/uvx/uv`，应用会直接调用 `mergeConfigFromJson()` 合并进本地配置。

- **普通安装（需要下载仓库）**
  - 否则，会走下载仓库 ZIP -> 解压到 `Download/Operit/mcp_plugins/<pluginId>/...` -> 保存 marketConfig -> 后续由部署流程生成/保存配置并部署到 Linux 环境。

### 5.2 MCP 配置界面的“导入/连接”

`MCPConfigScreen` 里提供了四种入口：

- 从仓库导入（下载 GitHub ZIP 并解压到插件目录）
- 从 ZIP 导入（把本地 ZIP 解压到插件目录）
- 连接远程服务
- 配置导入（粘贴 JSON，调用 `mergeConfigFromJson()` 合并）

如果你的目标是“自己手动加 MCP”，但又不想直接改文件，**最推荐用“配置导入”**：

- 它能自动处理 JSON 解析、自动补齐元数据、并刷新列表。

## 6. 验证与排查

- **确认配置文件路径**：
  - MCP 配置界面里有“打开配置文件”的入口（会尝试用系统打开器打开），打不开会 Toast 显示路径。

- **手动刷新**：
  - 配置改完但 UI 没变化时，执行“刷新插件列表/重新进入页面/重启应用”。
  - 刷新会触发 `reloadConfigurations()`，并重新从 `mcpServers` 自动识别服务器。

- **Bridge 相关问题**（本地插件/远程插件都会依赖 Bridge）：
  - Bridge 监听端口通常是 `8752`（本地直连），不通会降级尝试 `8751`（SSH 转发）。
  - Bridge 启动依赖终端环境中的 NodeJS（以及当前代码里仍会检查 pnpm）。
  - 启动页的插件加载会提示：Node 缺失、Bridge 启动失败等。

- **工具调用报“未激活”**：
  - 说明该 MCP 服务还没处于 active。
  - 解决方式：在 MCP 管理界面启动该插件，或在对话中先调用 `use_package` 指定包名（即 `pluginId`）。

---

## 代码定位（给开发者）

- 配置与状态：`MCPLocalServer`（`mcp_config.json` / `server_status.json`）
- 插件安装/导入：`MCPRepository`（仓库 ZIP/本地 ZIP）
- 启动/注册/验证/缓存：`MCPStarter`（Bridge + deploy + ping + cache tools）
- Bridge：`MCPBridge`（TCP 命令：register/spawn/listtools/toolcall/...）
- 工具执行：`MCPToolExecutor`（`pluginId:toolName` -> Bridge toolcall）
