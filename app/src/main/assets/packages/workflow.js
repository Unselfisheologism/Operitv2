/* METADATA
{
  name: "workflow"
  description: '''
工作流管理工具，提供工作流的创建、查询、更新、删除和触发执行能力。

本工具的核心目标：让 AI 能够“自主创建/修改工作流”，并正确理解：
- 节点类型（trigger/execute/condition/logic/extract）
- 触发节点类型（manual/schedule/tasker/intent）及其配置
- 参数引用（ParameterValue：静态值 vs 引用其他节点输出）
- 分支连线 condition 的语义（true/false/regex）

重要：create_workflow/update_workflow 的 nodes/connections 参数类型是 string（JSON 数组字符串）。
本示例封装允许你直接传对象数组，封装层会自动 JSON.stringify。
'''

  enabledByDefault: true

  tools: [
    {
      name: "usage_advice"
      description: '''
工作流工具使用建议（给 AI）：

- 推荐流程：
  1) 先 get_all_workflows 找到候选 workflow_id。
  2) 再 get_workflow 获取 nodes/connections 全量结构。
-  3) 如果你要“整体替换” nodes/connections：构造完整的新数组后用 update_workflow 一次性覆盖。
-  4) 如果你只想“增量修改” nodes/connections：优先使用 patch_workflow（node_patches/connection_patches）。

- 节点与连线的 ID：
  - 节点 id 可省略（服务端会生成），但如果你要创建 connections，强烈建议你在 nodes 里显式写好 id。
  - connections 里 source/target 可以用：
    - sourceNodeId/targetNodeId（推荐）
    - 或 source/target/from/to
    - 或 sourceIndex/targetIndex（按 nodes 数组下标）
    - 或 sourceNodeName/targetNodeName（不推荐：同名会歧义）

- 分支连线 condition（核心）：
  - 对 ConditionNode / LogicNode：
    - condition 为空：默认代表 true 分支（相当于 "true"）
    - condition = "false"：false 分支
    - condition = 其它字符串：当作 Regex 匹配源节点输出字符串
  - 对非 Condition/Logic 节点：
    - condition 为空：表示“源节点执行成功就走”（顺序依赖）

- 参数引用（ParameterValue）：
  - 静态值：直接写字符串/数字/布尔值即可（会被当作 StaticValue）
  - 引用某节点输出：写对象 { nodeId: "<node-id>" }
    - 兼容字段：nodeId / ref / refNodeId

- 触发节点类型（TriggerNode.triggerType）：
  - manual：手动触发（UI 点“触发工作流”）
  - schedule：定时触发（由 WorkManager 调度）
  - tasker：Tasker 事件触发
  - intent：系统广播 Intent 触发

  触发配置 TriggerNode.triggerConfig（注意：值全是 string）：
  - schedule：
    - schedule_type: interval | specific_time | cron
    - interval_ms: "900000"  (15分钟)
    - specific_time: "2026-01-04 10:30"  (格式依实现)
    - cron_expression: "15 * * * *"  (简化 cron)
    - repeat: "true"/"false"
    - enabled: "true"/"false"
  - tasker：
    - command: "start_meeting"  (当 Tasker params 中包含该字符串则触发)
  - intent：
    - action: "com.example.MY_ACTION"  (当收到该 action 的 Intent 则触发)
'''
      parameters: []
    }

    {
      name: "get_all_workflows"
      description: "获取所有工作流列表（只含概要信息：ID/名称/启用/统计等）。"
      parameters: []
    }

    {
      name: "get_workflow"
      description: "获取指定工作流完整详情（nodes + connections）。"
      parameters: [
        { name: "workflow_id", description: "工作流 ID", type: "string", required: true }
      ]
    }

    {
      name: "create_workflow"
      description: '''
创建工作流。

参数说明：
- nodes: JSON 数组字符串（推荐传对象数组，让封装自动 stringify）
- connections: JSON 数组字符串（同上）

节点类型（node.type）：
- trigger / execute / condition / logic / extract

Execute 节点：
- actionType: 工具名（如 "visit_web" / "list_files" / "get_system_setting" ...）
- actionConfig: 工具参数对象，支持 ParameterValue（静态值/节点引用）

Condition 节点：
- left/right: ParameterValue
- operator: EQ/NE/GT/GTE/LT/LTE/CONTAINS/NOT_CONTAINS/IN/NOT_IN

Logic 节点：
- operator: AND/OR

Extract 节点：
- source: ParameterValue
- mode: REGEX/JSON
- expression: REGEX 表达式 或 JSONPath
- group/defaultValue 可选
'''
      parameters: [
        { name: "name", description: "工作流名称", type: "string", required: true }
        { name: "description", description: "工作流描述（可选）", type: "string", required: false }
        { name: "nodes", description: "可选，节点 JSON 数组字符串（或直接传节点数组，由封装 stringify）", type: "string", required: false }
        { name: "connections", description: "可选，连线 JSON 数组字符串（或直接传连线数组，由封装 stringify）", type: "string", required: false }
        { name: "enabled", description: "可选，是否启用（默认 true）", type: "boolean", required: false }
      ]
    }

    {
      name: "update_workflow"
      description: '''
更新工作流。

注意：update_workflow 的 nodes / connections 是“整体覆盖”。
- 若你只改其中一部分，推荐使用 patch_workflow。
- 或者：get_workflow 取回旧结构 -> 本地构造新数组（保留未改部分）-> update_workflow 一次性传回。
'''
      parameters: [
        { name: "workflow_id", description: "工作流 ID", type: "string", required: true }
        { name: "name", description: "可选，新名称", type: "string", required: false }
        { name: "description", description: "可选，新描述", type: "string", required: false }
        { name: "nodes", description: "可选，节点 JSON 数组字符串（整体覆盖）", type: "string", required: false }
        { name: "connections", description: "可选，连线 JSON 数组字符串（整体覆盖）", type: "string", required: false }
        { name: "enabled", description: "可选，是否启用", type: "boolean", required: false }
      ]
    }

    {
      name: "patch_workflow"
      description: '''
差异更新工作流（增量 patch）。

使用 node_patches / connection_patches 传入 JSON 数组字符串：
- op: add | update | remove
- id: 可选
- node / connection: 对象

说明：
- add：必须提供 node/connection
- update：必须提供 id 或 node.id/connection.id
- remove：必须提供 id
'''
      parameters: [
        { name: "workflow_id", description: "工作流 ID", type: "string", required: true }
        { name: "name", description: "可选，新名称", type: "string", required: false }
        { name: "description", description: "可选，新描述", type: "string", required: false }
        { name: "enabled", description: "可选，是否启用", type: "boolean", required: false }
        { name: "node_patches", description: "可选，节点 patch JSON 数组字符串", type: "string", required: false }
        { name: "connection_patches", description: "可选，连线 patch JSON 数组字符串", type: "string", required: false }
      ]
    }

    {
      name: "delete_workflow"
      description: "删除指定工作流。"
      parameters: [
        { name: "workflow_id", description: "工作流 ID", type: "string", required: true }
      ]
    }

    {
      name: "trigger_workflow"
      description: "触发指定工作流执行（相当于 UI 手动触发）。"
      parameters: [
        { name: "workflow_id", description: "工作流 ID", type: "string", required: true }
      ]
    }
  ]
}
*/
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
/// <reference path="./types/index.d.ts" />
const WorkflowIntegration = (function () {
    async function usage_advice(_params) {
        return {
            success: true,
            message: "请阅读 workflow 工具 METADATA 中的 usage_advice 说明：包含节点/连线 schema、分支 condition 规则、触发类型与配置示例。建议在脚本里直接使用 Tools.Workflow.getAll/get/create/update/patch/delete/trigger 等封装方法。"
        };
    }
    /**
     * 获取所有工作流
     * @returns 工作流列表
     */
    async function get_all_workflows() {
        const data = await Tools.Workflow.getAll();
        return {
            success: true,
            message: "成功获取工作流列表",
            data
        };
    }
    /**
     * 创建新工作流
     * @param params 工作流参数
     * @returns 创建结果
     */
    async function create_workflow(params) {
        const data = await Tools.Workflow.create(params.name, params.description || "", params.nodes || null, params.connections || null, params.enabled);
        return {
            success: true,
            message: "成功创建工作流",
            data
        };
    }
    /**
     * 获取工作流详情
     * @param params 获取参数
     * @returns 工作流详情
     */
    async function get_workflow(params) {
        const data = await Tools.Workflow.get(params.workflow_id);
        return {
            success: true,
            message: "成功获取工作流详情",
            data
        };
    }
    /**
     * 更新工作流
     * @param params 更新参数
     * @returns 更新结果
     */
    async function update_workflow(params) {
        const { workflow_id } = params, updates = __rest(params, ["workflow_id"]);
        const data = await Tools.Workflow.update(workflow_id, updates);
        return {
            success: true,
            message: "成功更新工作流",
            data
        };
    }
    /**
     * 差异更新工作流（增量 patch）
     * @param params patch 参数
     * @returns 更新结果
     */
    async function patch_workflow(params) {
        const { workflow_id } = params, patch = __rest(params, ["workflow_id"]);
        const data = await Tools.Workflow.patch(workflow_id, patch);
        return {
            success: true,
            message: "成功差异更新工作流",
            data
        };
    }
    /**
     * 删除工作流
     * @param params 删除参数
     * @returns 删除结果
     */
    async function delete_workflow(params) {
        const data = await Tools.Workflow['delete'](params.workflow_id);
        return {
            success: true,
            message: "成功删除工作流",
            data
        };
    }
    /**
     * 触发工作流执行
     * @param params 触发参数
     * @returns 执行结果
     */
    async function trigger_workflow(params) {
        const data = await Tools.Workflow.trigger(params.workflow_id);
        return {
            success: true,
            message: "成功触发工作流",
            data
        };
    }
    /**
     * 包装工具执行，处理错误和结果
     * @param func 要执行的函数
     * @param params 函数参数
     */
    async function wrapToolExecution(func, params) {
        try {
            const result = await func(params);
            complete(result);
        }
        catch (error) {
            console.error(`Tool ${func.name} failed unexpectedly`, error);
            complete({ success: false, message: String(error && error.message ? error.message : error) });
        }
    }
    return {
        usage_advice: (params) => wrapToolExecution(usage_advice, params),
        get_all_workflows: () => wrapToolExecution(get_all_workflows, {}),
        create_workflow: (params) => wrapToolExecution(create_workflow, params),
        get_workflow: (params) => wrapToolExecution(get_workflow, params),
        update_workflow: (params) => wrapToolExecution(update_workflow, params),
        patch_workflow: (params) => wrapToolExecution(patch_workflow, params),
        delete_workflow: (params) => wrapToolExecution(delete_workflow, params),
        trigger_workflow: (params) => wrapToolExecution(trigger_workflow, params)
    };
})();
exports.usage_advice = WorkflowIntegration.usage_advice;
exports.get_all_workflows = WorkflowIntegration.get_all_workflows;
exports.create_workflow = WorkflowIntegration.create_workflow;
exports.get_workflow = WorkflowIntegration.get_workflow;
exports.update_workflow = WorkflowIntegration.update_workflow;
exports.patch_workflow = WorkflowIntegration.patch_workflow;
exports.delete_workflow = WorkflowIntegration.delete_workflow;
exports.trigger_workflow = WorkflowIntegration.trigger_workflow;
