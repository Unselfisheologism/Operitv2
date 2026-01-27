# Operit App 中文硬编码字符串完整清单

本文档列出了整个app中所有需要国际化的中文硬编码字符串（已排除注释和AppLogger调用）。

**搜索范围**: `D:\Code\prog\assistance\app\src\main\java\com\ai\assistance\operit`

**排除项**:
1. 注释（// 和 /* */）
2. AppLogger调用
3. 已在strings.xml中定义的字符串引用

---

## 📊 国际化进度跟踪 (更新于 2025-01-27 深夜)

### ✅ 本次会话完成的新增文件

#### Text() 和 UI 字符串模式 (已完成)
- ✅ PackageDetailsDialog.kt (1个字符串 - "删除")
- ✅ AutomationPackageDetailsDialog.kt (3个字符串 - 标题、消息、关闭)
- ✅ AutomationFunctionExecutionDialog.kt (3个字符串 - 标题、消息、关闭)
- ✅ DialogComponents.kt (6个字符串 - 命令结果对话框)
- ✅ UIDebuggerComponents.kt (6个contentDescription字符串 - 之前已处理Text)

#### Toast 消息 (已完成)
- ✅ DialogComponents.kt (3个Toast消息 - 复制相关)
- ✅ SkillMarketViewModel.kt (5个Toast和错误消息 - 登录/退出/移除)

#### Snackbar 消息 (已完成)
- ✅ SkillManagerScreen.kt (9个Snackbar和其他UI字符串)

#### contentDescription 字符串 (部分完成)
- ✅ FolderNavigator.kt (4个contentDescription - 文件夹导航)
- ✅ MCPServerDetailsHeader.kt (1个contentDescription - 关闭)
- ✅ MCPEnvironmentVariablesDialog.kt (2个contentDescription - 删除/添加)
- ✅ UIDebuggerComponents.kt (6个contentDescription - UI调试器)
- ✅ ActivityMonitorPanel.kt (1个contentDescription - 关闭)
- ✅ ThemeSettingsScreen.kt (2个contentDescription - 已完成)
- ✅ FileDiffDisplay.kt (1个contentDescription - 已完成)
- ✅ FFmpegToolboxScreen.kt (1个contentDescription - 已完成)
- ✅ CharacterCardAssignDialog.kt (1个contentDescription - 已完成)
- ✅ FileListPane.kt (1个contentDescription - 已完成)

**本次会话统计**:
- 已处理文件数: 16个
- 已处理字符串数: ~54个
- 新增字符串资源: 54个中文 + 54个英文
- 重复项清理: ✅ 已清理4个重复项
- 资源检查: ✅ 无重复，完整匹配 (4455个字符串键)
- Lambda修复: ✅ 修复SkillManagerScreen.kt中5处stringResource()调用错误

### ✅ 已完成模块（累计）

#### Chat模块 (部分完成)
- ✅ DialogComponents.kt (7个字符串) - 实际路径：demo/components
- ✅ CustomXmlRenderer.kt (2个字符串 - 展开/收起)
- ✅ BubbleUserMessageComposable.kt (2个字符串)
- ✅ UserMessageComposable.kt (2个字符串)
- ✅ ChatHistorySelector.kt (4个字符串)
- ✅ FullscreenInputDialog.kt (1个字符串)
- ✅ ChatSettingsBar.kt (2个字符串)
- ✅ ChatScreenContent.kt (2个字符串)
- ✅ MemoryFolderSelectionDialog.kt (1个字符串)
- ✅ FileDiffDisplay.kt (1个contentDescription - 本次会话新增)

**小计**: 10个文件，24个字符串

#### Floating窗口模块 (100% 完成)
- ✅ FloatingChatWindowScreen.kt (11个字符串)
- ✅ FloatingFullscreenModeViewModel.kt (8个字符串)
- ✅ FloatingFullscreenScreen.kt (3个字符串)
- ✅ FloatingChatWindowInputControls.kt (3个字符串)
- ✅ SpeechInteractionManager.kt (8个字符串)
- ✅ SiriBall.kt (1个字符串)
- ✅ FloatingResultDisplay.kt (1个字符串)
- ✅ BottomControlBar.kt (18个字符串 - 之前已完成)
- ✅ EditPanel.kt (4个字符串 - 之前已完成)

**小计**: 7个文件，57个字符串

#### Memory模块 (100% 完成)
- ✅ MemoryViewModel.kt (51个字符串)
- ✅ MemoryScreen.kt (1个字符串)
- ✅ MemoryDialogs.kt (20个字符串)
- ✅ FolderNavigator.kt (4个contentDescription - 本次会话新增)
- ✅ EditMemorySheet.kt (9个字符串)
- ✅ ToolTestDialog.kt (3个字符串)
- ✅ DocumentViewDialog.kt (2个字符串 - 本次会话新增)
- ✅ GraphVisualizer.kt (0个UI字符串，仅日志)
- ✅ MemoryAppBar.kt (1个字符串)

**小计**: 9个文件，91个字符串

#### Common组件模块 (100% 完成)
- ✅ UIAutomationProgressOverlay.kt (4个字符串)
- ✅ EnhancedCodeBlock.kt (8个字符串)
- ✅ MarkdownImageRenderer.kt (3个字符串)
- ✅ CanvasMarkdownNodeRenderer.kt (1个字符串)

**小计**: 4个文件，16个字符串

#### Packages/Skills模块 (部分完成)
- ✅ MCPConfigScreen.kt (11个字符串)
- ✅ PackageManagerScreen.kt (8个字符串)
- ✅ PackageDetailsDialog.kt (1个字符串 - 本次会话新增)
- ✅ MCPMarketScreen.kt (4个字符串)
- ✅ ScriptExecutionDialog.kt (3个字符串)
- ✅ MCPEnvironmentVariablesDialog.kt (2个contentDescription - 本次会话新增)
- ✅ MCPDeployProgressDialog.kt (10个字符串)
- ✅ MCPDeployConfirmDialog.kt (6个字符串)
- ✅ MCPCommandsEditDialog.kt (7个字符串)
- ✅ MCPInstallProgressDialog.kt (11个字符串)
- ✅ MCPServerDetailsHeader.kt (1个contentDescription - 本次会话新增)
- ✅ AutomationPackageDetailsDialog.kt (3个字符串 - 本次会话新增)
- ✅ AutomationFunctionExecutionDialog.kt (3个字符串 - 本次会话新增)
- ✅ SkillMarketViewModel.kt (5个Toast/错误 - 本次会话新增)
- ✅ SkillManagerScreen.kt (9个Snackbar/UI - 本次会话新增)

**小计**: 15个文件，83个字符串

#### Settings模块 (部分完成)
- ✅ ColorPickerDialog.kt (18个字符串)
- ✅ ChatHistorySettingsScreen.kt (2个字符串)
- ✅ TagMarketScreen.kt (2个字符串)
- ✅ CustomHeadersSettingsScreen.kt (2个字符串)
- ✅ ContextSummarySettingsScreen.kt (1个字符串)
- ✅ GlobalDisplaySettingsScreen.kt (4个字符串)
- ✅ ThemeSettingsScreen.kt (2个contentDescription - 本次会话新增)
- ✅ CharacterCardAssignDialog.kt (1个contentDescription - 本次会话新增)

**小计**: 8个文件，32个字符串

#### Demo模块 (部分完成)
- ✅ AccessibilityWizardCard.kt (1个字符串)
- ✅ DialogComponents.kt (7个字符串 - 6个对话框UI + 3个Toast)

**小计**: 2个文件，8个字符串

#### Toolbox模块 (部分完成)
- ✅ ActivityMonitorPanel.kt (1个contentDescription - 本次会话新增)
- ✅ AutoGlmToolScreen.kt (1个字符串)
- ✅ HtmlPackagerScreen.kt (6个字符串)
- ✅ AutoGlmOneClickToolScreen.kt (4个字符串)
- ✅ UIDebuggerComponents.kt (6个contentDescription - 本次会话新增)
- ✅ StreamMarkdownDemo.kt (7个字符串)
- ✅ FFmpegToolboxScreen.kt (1个contentDescription - 本次会话新增)
- ✅ FileListPane.kt (1个contentDescription - 本次会话新增)

**小计**: 8个文件，27个字符串

### 📈 总体进度 (更新于 2025-01-27 深夜)

#### 累计完成统计
- **已完成文件数**: 63个
- **已完成字符串数**: ~336个
- **新增字符串资源**: 336个中文 + 336个英文
- **构建状态**: ✅ 编译通过
- **资源检查**: ✅ 无重复，完整匹配 (4455个字符串键)
- **重复项清理**: ✅ 已清理10个重复项（之前6个 + 本次4个）
- **导入修复**: ✅ 已修复6个文件的缺失导入

#### 本次会话统计 (2025-01-27 深夜)
- **新增完成文件**: 16个
- **新增完成字符串**: 54个
- **新增字符串资源**: 54个中文 + 54个英文
- **重复项清理**: 4个 (pkg_close×2, character_card_avatar×2)
- **导入修复**: 1个文件 (MCPCommandsEditDialog.kt添加background导入)
- **Lambda修复**: 5处 (SkillManagerScreen.kt)

#### 本次会话详细清单:
1. ✅ PackageDetailsDialog.kt (1个字符串) - 删除按钮
2. ✅ AutomationPackageDetailsDialog.kt (3个字符串) - 标题、消息、关闭
3. ✅ AutomationFunctionExecutionDialog.kt (3个字符串) - 标题、消息、关闭
4. ✅ DialogComponents.kt (9个字符串 - 6个对话框UI + 3个Toast)
5. ✅ SkillMarketViewModel.kt (5个) - Toast和错误消息
6. ✅ SkillManagerScreen.kt (9个) - Snackbar和其他UI
7. ✅ FolderNavigator.kt (4个) - contentDescription
8. ✅ MCPServerDetailsHeader.kt (1个) - contentDescription
9. ✅ MCPEnvironmentVariablesDialog.kt (2个) - contentDescription
10. ✅ UIDebuggerComponents.kt (6个) - contentDescription
11. ✅ ActivityMonitorPanel.kt (1个) - contentDescription
12. ✅ ThemeSettingsScreen.kt (2个) - contentDescription
13. ✅ FileDiffDisplay.kt (1个) - contentDescription
14. ✅ FFmpegToolboxScreen.kt (1个) - contentDescription
15. ✅ CharacterCardAssignDialog.kt (1个) - contentDescription
16. ✅ FileListPane.kt (1个) - contentDescription

### 🔄 待处理模块详细清单

#### ~~Memory模块~~ (已完成 ✓)

#### ~~Common组件~~ (已完成 ✓)

#### Packages/Skills模块 (部分完成)
**高优先级文件**:
- [x] packages/screens/MCPConfigScreen.kt (11个字符串) - ✅ 已完成
- [x] packages/screens/PackageManagerScreen.kt (8个字符串) - ✅ 已完成
- [x] packages/dialogs/PackageDetailsDialog.kt (1个字符串) - ✅ 已完成
- [ ] packages/screens/mcp/viewmodel/MCPMarketViewModel.kt (118个字符串)
- [x] packages/screens/skill/viewmodel/SkillMarketViewModel.kt (5个字符串) - ✅ 已完成
- [ ] packages/utils/SkillIssueParser.kt (18个字符串)
- [ ] packages/utils/MCPPluginParser.kt (17个字符串)
- [ ] packages/screens/MCPPluginDetailScreen.kt (16个字符串)
- [x] packages/dialogs/ScriptExecutionDialog.kt (3个字符串) - ✅ 部分完成
- [ ] packages/screens/SkillDetailScreen.kt (13个字符串)

**中优先级文件**:
- [x] packages/components/MCPInstallProgressDialog.kt (11个字符串) - ✅ 已完成
- [x] packages/dialogs/PackageDetailsDialog.kt (1个字符串) - ✅ 已完成
- [x] packages/screens/MCPMarketScreen.kt (4个字符串) - ✅ 已完成
- [x] packages/screens/SkillManagerScreen.kt (9个字符串) - ✅ 已完成

#### Chat模块 (1200+个字符串，89个文件)
**高优先级文件**:
- [ ] chat/webview/WorkspaceUtils.kt (304个字符串)
- [ ] chat/components/part/CustomXmlRenderer.kt (181个字符串)
- [ ] chat/webview/workspace/editor/language/KotlinSupport.kt (109个字符串)
- [ ] chat/webview/workspace/editor/language/HtmlSupport.kt (101个字符串)
- [ ] chat/webview/workspace/editor/language/JavaScriptSupport.kt (85个字符串)
- [ ] chat/viewmodel/ChatViewModel.kt (79个字符串)
- [ ] chat/components/ExportDialogs.kt (68个字符串)
- [ ] chat/webview/LocalWebServer.kt (64个字符串)
- [ ] chat/components/style/bubble/BubbleUserMessageComposable.kt (64个字符串)
- [ ] chat/components/style/cursor/UserMessageComposable.kt (63个字符串)

**中优先级文件**:
- [ ] chat/components/part/DialogComponents.kt
- [ ] chat/components/part/FileDiffDisplay.kt
- [ ] chat/screens/AIChatScreen.kt
- [ ] chat/util/MessageImageGenerator.kt
- [ ] chat/webview/workspace/WorkspaceConfig.kt

#### Workflow模块 (274个字符串，6个文件)
**高优先级文件**:
- [ ] workflow/viewmodel/WorkflowViewModel.kt (113个字符串)
- [ ] workflow/screens/WorkflowDetailScreen.kt (87个字符串)
- [ ] workflow/components/ScheduleConfigDialog.kt (42个字符串)
- [ ] workflow/components/GridWorkflowCanvas.kt (16个字符串)
- [ ] workflow/components/ConnectionMenu.kt (11个字符串)
- [ ] workflow/screens/WorkflowListScreen.kt (5个字符串)

#### Settings模块 (460+个字符串，40个文件)
**高优先级文件**:
- [ ] settings/screens/PersonaCardGenerationScreen.kt (89个字符串)
- [ ] settings/sections/ModelApiSettingsSection.kt (79个字符串)
- [ ] settings/screens/ThemeSettingsScreen.kt (68个字符串)
- [ ] settings/screens/TagMarketScreen.kt (50个字符串)
- [ ] settings/screens/ModelPromptsSettingsScreen.kt (46个字符串)
- [ ] settings/components/ColorPickerDialog.kt (45个字符串) ✅ 已完成
- [ ] settings/screens/ChatBackupSettingsScreen.kt (27个字符串)
- [ ] settings/screens/CustomHeadersSettingsScreen.kt (24个字符串)

**中优先级文件**:
- [ ] settings/screens/ContextSummarySettingsScreen.kt
- [ ] settings/screens/FunctionalConfigScreen.kt
- [ ] settings/screens/GlobalDisplaySettingsScreen.kt
- [ ] settings/screens/MnnModelDownloadScreen.kt
- [ ] settings/screens/ModelConfigScreen.kt

#### Toolbox模块 (670+个字符串，42个文件)
**高优先级文件**:
- [ ] toolbox/screens/apppermissions/AppPermissionsScreen.kt (230个字符串)
- [ ] toolbox/screens/tooltester/ToolTesterScreen.kt (120个字符串)
- [ ] toolbox/screens/filemanager/utils/FileUtils.kt (60个字符串)
- [ ] toolbox/screens/filemanager/components/FileContextMenu.kt (56个字符串)
- [ ] toolbox/screens/filemanager/viewmodel/FileManagerViewModel.kt (38个字符串)
- [ ] toolbox/screens/autoglm/AutoGlmViewModel.kt (38个字符串)
- [ ] toolbox/screens/uidebugger/UIDebuggerViewModel.kt (34个字符串)
- [ ] toolbox/screens/autoglm/AutoGlmOneClickToolScreen.kt (33个字符串)

**中优先级文件**:
- [ ] toolbox/screens/ffmpegtoolbox/FFmpegToolboxScreen.kt (26个字符串)
- [ ] toolbox/screens/htmlpackager/HtmlPackagerScreen.kt (23个字符串)
- [ ] toolbox/screens/logcat/LogcatViewModel.kt (19个字符串)
- [ ] toolbox/screens/filemanager/FileManagerScreen.kt

#### Demo模块 (125个字符串，7个文件)
**中优先级文件**:
- [ ] demo/state/DemoStateManager.kt (53个字符串)
- [ ] demo/screens/ShizukuDemoScreen.kt (48个字符串)
- [ ] demo/viewmodel/ShizukuDemoViewModel.kt (8个字符串)
- [ ] demo/components/PermissionLevelCard.kt (8个字符串)
- [ ] demo/wizards/AccessibilityWizardCard.kt (4个字符串)

#### Token模块 (92个字符串，8个文件)
**中优先级文件**:
- [ ] token/preferences/UrlConfigManager.kt (27个字符串)
- [ ] token/webview/WebViewConfig.kt (24个字符串)
- [ ] token/webview/DeepseekJsInterface.kt (12个字符串)
- [ ] token/TokenConfigWebViewScreen.kt (8个字符串)
- [ ] token/webview/JsScripts.kt (7个字符串)
- [ ] token/network/DeepseekApiConstants.kt (7个字符串)

#### Common模块 (100+个字符串)
**中优先级文件**:
- [ ] common/displays/UIAutomationProgressOverlay.kt (4个字符串: "Phone Agent", "恢复代理", "接管", "取消")
- [ ] common/displays/VirtualDisplayOverlay.kt (日志类，可选择性处理)
- [ ] common/markdown/EnhancedCodeBlock.kt (8个字符串: "显示代码", "渲染Mermaid", "预览HTML", "关闭自动换行", "开启自动换行", "全屏预览", "复制代码", "已复制", "退出全屏")
- [ ] common/markdown/MarkdownImageRenderer.kt (3个字符串: "加载失败", "关闭", "重置缩放")
- [ ] common/markdown/CanvasMarkdownNodeRenderer.kt (1个字符串: "渲染失败")

### 🔧 问题修复记录 (2025-01-26)

#### XML特殊字符问题
- **问题**: ColorPickerDialog中的特殊Unicode字符 (✓ ⚠) 导致XML解析失败
- **解决**: 移除特殊字符，使用纯文本替代
  - `colorpicker_high_contrast`: "高对比度 ✓" → "高对比度"
  - `colorpicker_low_contrast`: "低对比度 ⚠" → "低对比度"

#### 单引号转义问题
- **问题**: 英文"Don't"中的单引号导致aapt编译错误
- **解决**: 转义单引号为 `\'`
  - `floating_didnt_hear_clearly`: "Didn't hear clearly" → "Didn\'t hear clearly"

#### Lambda中LocalContext使用错误
- **问题**: SiriBall.kt:76 - 在lambda回调中使用`LocalContext.current`
- **错误**: @Composable invocations can only happen from the context of a @Composable function
- **解决**: Lambda回调使用外层@Composable函数中已获取的context，移除lambda内部的`LocalContext.current`

#### R类导入缺失
- **问题**: FloatingFullscreenModeViewModel.kt - "Unresolved reference: string"
- **原因**: 缺少 `import com.ai.assistance.operit.R`
- **解决**: 添加R类的导入语句

### 📝 最佳实践总结

#### 1. 字符串资源使用规则
| 上下文类型 | 使用方法 | 示例 |
|---------|---------|------|
| @Composable函数 | `stringResource(R.string.xxx)` | `Text(text = stringResource(R.string.hello))` |
| 非Composable代码 | `context.getString(R.string.xxx)` | `toast(context.getString(R.string.hello))` |
| Lambda回调 | `context.getString(R.string.xxx)` | `onClick = { context.getString(R.string.ok) }` |
| ViewModel类 | `context.getString(R.string.xxx)` | 需要传入context参数 |

#### 2. XML字符串注意事项
- ❌ 避免特殊Unicode字符 (✓ ⚠ → 等)，使用纯文本或数字
- ❌ 避免未转义的单引号，使用 `\'` 替代 `'`
- ✅ 使用位置参数 `%1$s`, `%2$d` 而非 `%s`, `%d`
- ✅ 添加 `formatted="false"` 属性用于包含HTML标签的字符串

#### 3. 导入清单
处理国际化时需要添加的导入：
```kotlin
import androidx.compose.ui.res.stringResource  // for @Composable
import com.ai.assistance.operit.R                // R类引用
import androidx.compose.ui.platform.LocalContext  // 获取context
```

#### 4. 常见错误及解决方案
| 错误 | 原因 | 解决方案 |
|-----|------|---------|
| @Composable invocations error | 在lambda中使用stringResource() | 使用context.getString() |
| Unresolved reference: R | 未导入R类 | 添加 `import com.ai.assistance.operit.R` |
| Invalid unicode escape sequence | XML中特殊字符未转义 | 移除或转义特殊字符 |
| Multiple substitutions | 非位置参数格式 | 使用 `%1$s` 替代 `%s` |

---

## 1. Chat模块 (ui/features/chat)

### components/ChatHistorySelector.kt
- **Line 1062**: "聊天记录设置"
- **Line 1069**: "显示模式"
- **Line 1273**: "设置"
- **Line 1285**: "返回"

### components/ChatScreenContent.kt
- **Line 662**: "开始导出..."
- **Line 700**: "开始导出..."

### components/ChatSettingsBar.kt
- **Line 1015**: "禁止使用autoglm作为对话主模型。对话模型和ui控制模型是分离的，请选择任意一个别的聪明的大模型。如有疑问，请仔细阅读文档学习软件的模型配置机制。"
- **Line 1167**: "${modelList.size}个模型"

### components/ExportDialogs.kt
- **Line 698**: "复制网页文件到APK ${webContentDir.absolutePath} -> ${webAssetsDir.absolutePath}"
- **Line 710**: "签名使用密钥库: ${keyStoreFile.absolutePath}, 大小: ${keyStoreFile.length()}"
- **Line 854**: "复制网页文件到Windows应用: ${webContentDir.absolutePath} -> ${webContentTarget.absolutePath}"
- **Line 982**: "复制文件失败: ${file.absolutePath} -> ${destFile.absolutePath}"

### components/FullscreenInputDialog.kt
- **Line 56**: "全屏输入"

### components/MemoryFolderSelectionDialog.kt
- **Line 341**: "未分类"
- **Line 374**: "未分类"
- **Line 376**: "未分类"
- **Line 377**: "未分类"

### components/part/CustomXmlRenderer.kt
- **Line 220**: "收起" / "展开"
- **Line 368**: "收起" / "展开"
- **Line 412**: "未知工具"
- **Line 479**: "未知工具"

### components/part/DetailsTagRenderer.kt
- **Line 60**: "收起" / "展开"

### components/part/DialogComponents.kt
- **Line 142**: "关闭"

### components/part/FileDiffDisplay.kt
- **Line 73**: "工具执行结果"

### components/part/ThinkToolsXmlNodeGrouper.kt
- **Line 229**: "收起" / "展开"

### components/style/bubble/BubbleUserMessageComposable.kt
- **Line 454**: "用户正在回复你之前的这条消息："
- **Line 482**: "工作区状态"

### components/style/cursor/UserMessageComposable.kt
- **Line 382**: "用户正在回复你之前的这条消息："
- **Line 410**: "工作区状态"

### screens/AIChatScreen.kt
- **Line 790**: "正在导出工作区: ${workDir.absolutePath}, 聊天ID: $currentChatId"

### util/MessageImageGenerator.kt
- **Line 93**: "消息列表不能为空"
- **Line 311**: "图片捕获失败: ${e.message}"

### webview/workspace/WorkspaceConfig.kt
- **Line 35**: "切换到浏览器预览"
- **Line 36**: "浏览器预览"

### webview/workspace/editor/PerformEdit.kt
- **Line 27**: "EditText不能为空"

---

## 2. Floating窗口模块 (ui/floating)

### ui/ball/FloatingResultDisplay.kt
- **Line 29**: "思考"

### ui/ball/SiriBall.kt
- **Line 75**: "用户无输入"

### ui/fullscreen/components/BottomControlBar.kt
- **Line 183**: "屏幕识别"
- **Line 194**: "通知"
- **Line 205**: "位置"
- **Line 216**: "圆圈选择" / "圈选识别"
- **Line 314**: "当前正在执行任务"
- **Line 331**: "暂停对话"
- **Line 350**: "编辑"
- **Line 494**: "取消获取" / "结束解码"
- **Line 524**: "切换多轮模式"
- **Line 545**: "缩小悬浮窗"
- **Line 578**: "编辑"
- **Line 590**: "取消"
- **Line 807**: "获取录音"
- **Line 815**: "编辑录音"
- **Line 823**: "锁住说话"

### ui/fullscreen/components/EditPanel.kt
- **Line 78**: "编辑用户消息"
- **Line 93**: "正在后台编辑用户消息..."
- **Line 133**: "取消"
- **Line 149**: "发送"

### ui/fullscreen/screen/FloatingFullscreenScreen.kt
- **Line 250**: "切换多轮模式"
- **Line 260**: "缩小悬浮窗"
- **Line 275**: "关闭悬浮窗"

### ui/fullscreen/viewmodel/FloatingFullscreenModeViewModel.kt
- **Line 33**: "键盘上划手动开始说话"
- **Line 70**: "思考中..."
- **Line 120**: "思考中..."
- **Line 297**: "无法获取输入法状态"
- **Line 299**: "键盘上划手动开始说话"
- **Line 388**: "编辑用户消息"
- **Line 394**: "键盘上划手动开始说话"
- **Line 402**: "思考中..."
- **Line 420**: "思考中..."

### ui/pet/AvatarEmotionManager.kt
- **Line 21-24**: 各种情绪标签（开心、害羞、惊讶、思考、太可爱、困惑、期待、愤怒、生气、害怕、难过、压力、疲惫、绝望、沉默、委屈、傲娇、凝视、深沉思考）

### ui/window/components/FloatingChatWindowInputControls.kt
- **Line 201**: "附加更多"
- **Line 246**: "取消" / "发送"

### ui/window/screen/FloatingChatWindowScreen.kt
- **Line 254**: "全屏"
- **Line 292**: "常用应用"
- **Line 373**: "最小化"
- **Line 408**: "关闭"
- **Line 619**: "关闭"
- **Line 680**: "正在处理流式响应..."
- **Line 742**: "取消" / "发送"
- **Line 1007**: "当前使用工具: ${state.toolName}"
- **Line 1008**: "正在处理工具调用: ${state.toolName}"
- **Line 1011**: "错误: ${state.message}"
- **Line 1012**: "回复中..."

### voice/SpeechInteractionManager.kt
- **Line 121**: "无法获取音频"
- **Line 135**: "正在识别..."
- **Line 174-205**: 各种识别状态（识别完成、识别失败、识别中、思考中、用户无输入）

---

## 3. Settings模块 (ui/features/settings)

### components/AvatarPicker.kt
- **Line 80**: "重置"

### components/CharacterCardAssignDialog.kt
- **Line 160**: "角色卡头像"
- **Line 166**: "角"

### components/ColorPickerDialog.kt
- **Line 209-214**: 各种颜色选择提示（选择主色、次色、状态栏颜色等）
- **Line 264**: "示例文本"
- **Line 273**: "高对比度 ✓" / "低对比度 ⚠"
- **Line 297**: "手动输入颜色"
- **Line 340**: "粘贴"
- **Line 346**: "应用"
- **Line 388**: "应用 RGB"
- **Line 436**: "应用 HSV"
- **Line 490**: "最近使用"
- **Line 529**: "推荐颜色"
- **Line 573**: "确定"
- **Line 577**: "取消"

### screens/ChatHistorySettingsScreen.kt
- **Line 66**: "内部存储" / "外部存储"
- **Line 130**: "内部存储"
- **Line 148**: "外部存储"
- **Line 1198**: "内部存储"

### screens/ContextSummarySettingsScreen.kt
- **Line 76**: "$name 必须是大于 0 的有效数字"
- **Line 85**: "$name 必须是大于 0 的有效整数"
- **Line 94**: "$name 必须是大于等于 0 的有效整数"
- **Line 100-104**: 各种设置项名称（最大文件大小、分片大小等）
- **Line 144**: "上下文窗口与对话总结现在在模型配置中管理。此处仅调节文件读取与结果截断相关的系统参数。"
- **Line 173**: "行"
- **Line 192**: "次"
- **Line 201**: "次"
- **Line 233**: "重置所有设置"
- **Line 269**: "验证失败"

### screens/CustomHeadersSettingsScreen.kt
- **Line 25-45**: 各种预设选项描述
- **Line 115**: "加载预设"
- **Line 192**: "请求头已保存"

### screens/FunctionalConfigScreen.kt
- **Line 309**: "禁止使用autoglm作为对话主模型。对话模型和ui控制模型是分离的，请选择任意一个别的聪明的大模型。如有疑问，请仔细阅读文档学习软件的模型配置机制。"
- **Line 603**: "${modelList.size}个模型"

### screens/GlobalDisplaySettingsScreen.kt
- **Line 233**: "自动化显示与行为"
- **Line 274**: "虚拟屏幕码率"
- **Line 345**: "自动化状态指示样式"
- **Line 366**: "全屏彩虹边框"
- **Line 380**: "顶部提示条"
- **Line 394**: "自动化截图设置"
- **Line 402**: "图片格式"
- **Line 419**: "PNG（无损，默认）"
- **Line 430**: "JPG（有损，体积更小）"
- **Line 442**: "画质（仅对 JPG 生效）"
- **Line 478**: "分辨率缩放（发送前缩小截图）"

### screens/MnnModelDownloadScreen.kt
- **Line 69**: "加载失败"
- **Line 126**: "加载失败"

### screens/ModelConfigScreen.kt
- **Line 721**: "请输入有效的上下文长度"
- **Line 725**: "请输入有效的最大上下文长度"
- **Line 747**: "保存失败"
- **Line 780**: "保存失败"
- **Line 791**: "请输入0-1之间的总结触发阈值"
- **Line 795**: "请输入有效的消息数量阈值"
- **Line 824**: "保存失败"
- **Line 867**: "收起" / "展开"
- **Line 954**: "收起" / "展开"

---

## 4. Packages/Skills模块 (ui/features/packages)

### components/MCPInstallProgressDialog.kt
- **Line 42-201**: 各种安装/卸载进度提示

### components/dialogs/content/MCPServerConfigContent.kt
- **Line 63**: "插件安装路径:"
- **Line 79**: "JSON 配置"
- **Line 118**: "此配置将在服务器启动时生效"
- **Line 141**: "保存配置"
- **Line 154**: "此插件尚未安装，无法配置"

### components/dialogs/content/MCPServerDetailsContent.kt
- **Line 64**: "暂无描述"

### components/dialogs/header/MCPServerDetailsHeader.kt
- **Line 172**: "关闭"

### components/dialogs/tabs/MCPServerDetailsTabs.kt
- **Line 84**: "插件详情"
- **Line 130**: "配置设置"

### dialogs/AutomationFunctionExecutionDialog.kt
- **Line 27**: "Kotlin UI 自动化功能执行（route/config）已移除。"
- **Line 55**: "自动化功能"
- **Line 72**: "关闭"

### dialogs/AutomationPackageDetailsDialog.kt
- **Line 25**: "Kotlin UI 自动化配置功能已移除。"
- **Line 29**: "提示"

### dialogs/PackageDetailsDialog.kt
- **Line 82-83**: "确认删除"对话框文本
- **Line 101**: "删除"
- **Line 106**: "取消"
- **Line 173**: "工具列表"
- **Line 185**: "暂无可用工具"
- **Line 213**: "默认"
- **Line 272**: "暂无可用工具"
- **Line 310**: "删除"
- **Line 315**: "关闭"
- **Line 399**: "运行"

### dialogs/ScriptExecutionDialog.kt
- **Line 205**: "错误: ${result.error}"
- **Line 224**: "取消"
- **Line 243**: "缺少参数: ${missingParams.join...}"
- **Line 269**: "执行流错误: ${e.message}"
- **Line 295**: "执行错误: ${e.message}"
- **Line 313**: "执行中"
- **Line 321**: "执行"

### screens/MCPConfigScreen.kt
- **Line 575-827**: 各种配置相关提示文本

### screens/MCPMarketScreen.kt
- **Line 285**: "搜索插件名称、描述、作者..."
- **Line 286**: "搜索"
- **Line 290**: "清空搜索"
- **Line 324**: "刷新"

### screens/PackageManagerScreen.kt
- **Line 262**: "管理环境变量"
- **Line 698**: "配置环境变量"
- **Line 702**: "当前已导入的工具包没有声明需要的环境变量。"
- **Line 822**: "默认: ${envVar.defaultValue}"
- **Line 842**: "输入值（必需）" / "输入值（可选）"
- **Line 856**: "保存"
- **Line 861**: "取消"

### screens/SkillManagerScreen.kt
- **Line 167**: "已刷新"
- **Line 194**: "未找到任何Skill。请将Skill文件夹放入: ${skillRepository.getSkillsDirectoryPath()}，并确保其中包含 SKILL.md。"
- **Line 416**: "仅支持 .zip 文件"
- **Line 427**: "无法读取文件"
- **Line 442**: "导入失败: ${e.message}"
- **Line 493**: "已删除: $skillName"
- **Line 495**: "删除失败: $skillName"
- **Line 502**: "删除"
- **Line 512**: "关闭"

---

## 5. 其他UI功能模块

### about/screens/AboutScreen.kt
- **Line 426**: "未知"

### demo/components/DialogComponents.kt
- **Line 80**: "确定"
- **Line 95**: "命令结果"
- **Line 99**: "已复制到剪贴板"
- **Line 104**: "复制失败: ${e.message}"
- **Line 110**: "复制"
- **Line 156**: "选择一个示例命令:"

### demo/components/PermissionStatusItem.kt
- **Line 49**: "已授权" / "未授权"

### demo/screens/ShizukuDemoScreen.kt
- **Line 280**: "缓存Shizuku版本状态 - 已安装: $installed, 内置: $bundled, 需要更新: $needsUpdate"
- **Line 449**: "APK提取成功: ${apkFile.absolutePath}, 大小: ${apkFile.length()} 字节"
- **Line 598**: "APK提取成功: ${apkFile.absolutePath}, 大小: ${apkFile.length()} 字节"

### demo/state/DemoStateManager.kt
- **Line 483-523**: 各种示例命令名称

### demo/viewmodel/ShizukuDemoViewModel.kt
- **Line 95**: "Root状态更新: 设备已Root=$isDeviceRooted, 应用有Root权限=$hasRootAccess"

### demo/wizards/AccessibilityWizardCard.kt
- **Line 322**: "我已明确并知晓无障碍权限导致的其他应用封号、限制账号功能风险，并后果自己承担"
- **Line 367**: "检测到新版本"
- **Line 373**: "已安装: ${installedVersion ?: "} -> 内置: $bundledVersion"
- **Line 383**: "立即更新"

### event/screens/EventCampaignScreen.kt
- **Line 30-123**: 整个活动宣传页面文本（包含大量中文）

### memory/screens/FolderNavigator.kt
- **Line 160**: "文件夹"
- **Line 173**: "关闭侧边栏"
- **Line 203**: "刷新文件夹列表"
- **Line 216**: "新建文件夹"
- **Line 224**: "全部"
- **Line 431**: "折叠" / "展开"
- **Line 446**: "文件夹"
- **Line 511-657**: 文件夹操作相关对话框文本

### memory/screens/MemoryAppBar.kt
- **Line 75**: "搜索记忆"

### common/displays/UIAutomationProgressOverlay.kt
- **Line 382**: "恢复代理" / "接管"
- **Line 390**: "取消"
- **Line 403**: "执行"
- **Line 404**: "完成"

### common/displays/VirtualDisplayOverlay.kt
- **Line 604**: "OverlayCard: Shower 虚拟屏已关闭/断开，取消自动化任务 agentId=$agentId"
- **Line 987**: "OverlayCard: Shower 虚拟屏尚未就绪, id=$id, hasShowerDisplay=$hasShowerDisplay, videoSize=${ShowerController.getVideoSize(agentId)}"
- **Line 1027**: "Shower 虚拟屏尚未就绪"

### common/markdown/CanvasMarkdownNodeRenderer.kt
- **Line 579**: "渲染失败"

### common/markdown/EnhancedCodeBlock.kt
- **Line 211**: "显示代码" / "渲染Mermaid"
- **Line 226**: "显示代码" / "预览HTML"
- **Line 253**: "全屏预览"
- **Line 267**: "关闭自动换行" / "开启自动换行"
- **Line 279**: "复制代码"
- **Line 421**: "已复制"
- **Line 455**: "退出全屏"

### common/markdown/MarkdownImageRenderer.kt
- **Line 313**: "加载失败"
- **Line 332**: "关闭"
- **Line 405**: "重置缩放"

### common/markdown/StreamMarkdownRenderer.kt
- **Line 124**: "XML内容"

---

## 6. API模块

### chat/AIForegroundService.kt
- **Line 83**: "Operit 正在运行"
- **Line 85**: "对话完成提醒"
- **Line 628**: "保持 Operit 在后台运行。"
- **Line 638**: "对话完成后提醒你。"
- **Line 921**: "唤醒识别输出(${if (result.isFinal) "
- **Line 1138**: "Operit 正在运行（唤醒暂停）"
- **Line 1140**: "Operit 正在运行（唤醒监听中）"
- **Line 1143**: "Operit 正在运行"
- **Line 1189**: "语音悬浮窗"
- **Line 1208**: "关闭唤醒" / "开启唤醒"
- **Line 1228**: "退出"
- **Line 1249**: "停止"

### chat/EnhancedAIService.kt
- **Line 420**: "正在处理输入..."
- **Line 447**: "sendMessage调用开始: 功能类型=$functionType, 提示词类型=$promptFunctionType, 思考引导=$thinkingGuidance"
- **Line 473**: "正在处理消息..."
- **Line 500**: "正在连接AI服务..."
- **Line 557**: "正在接收AI响应..."
- **Line 606**: "流收集完成，总计 $totalChars 字符，耗时: ${System.currentTimeMillis() - streamStartTime}ms"
- **Line 625**: "错误: ${e.message}"
- **Line 852**: "警告：工具调用和等待用户响应不能同时存在。工具调用被处理了，但这是极具危险性的。"
- **Line 867**: "检测到 ${extractedToolInvocations.size} 个工具调用，处理时间: ${System.currentTimeMillis() - startTime}ms"
- **Line 1147**: "正在接收工具执行后的AI响应..."
- **Line 1217**: "处理工具执行结果失败: ${e.message}"
- **Line 1441**: "记忆与记忆库工具"
- **Line 1472**: "Tool Call已启用，提供 ${selectedTools.size} 个工具 (enableTools=$enableTools, enableMemoryQuery=$enableMemoryQuery)"

### chat/enhance/ConversationMarkupManager.kt
- **Line 80**: "检测到多个工具调用。系统将只执行第一个工具 `$toolName`，忽略其它工具调用。请避免在单个消息中同时调用多个工具。"
- **Line 107-113**: 工具调用相关警告信息

### chat/enhance/ConversationService.kt
- **Line 100-172**: 对话总结相关提示文本
- **Line 197**: "请按照要求总结对话内容"
- **Line 211**: "总结完成"
- **Line 219**: "对话摘要：未能生成有效摘要。"
- **Line 474-510**: 用户信息描述（性别、年龄、出生日期等）
- **Line 662**: "**你必须遵守:禁止使用动作表情，禁止描述动作表情，只允许使用纯文本进行对话，禁止使用括号将动作表情包裹起来，禁止输出括号'()',但是会使用更多'呐，嘛~，诶？，嗯…，唔…，昂？，哦'等语气词**"
- **Line 676-679**: 情绪表达规则
- **Line 685-688**: 绘图（自拍）功能说明
- **Line 839-853**: 翻译助手说明
- **Line 915**: "你是一个专业的技术文档撰写助手，擅长为软件工具包编写简洁清晰的功能描述。"
- **Line 969-1082**: 图片/音频/视频识别相关提示

### chat/enhance/FileBindingService.kt
- **Line 76-77**: 文件操作说明
- **Line 551**: "并行块[$threadIndex] 发现更佳匹配: 行 ${i + 1}-$endLine, 相似度: $matchPercentage%"
- **Line 626-628**: 匹配完成提示

### chat/enhance/ToolExecutionManager.kt
- **Line 121**: "参数无效: ${validationResult.errorMessage}"

---

## 📊 统计信息 (更新于 2025-01-27 深夜)

### 按模块统计
- **✅ Floating模块**: 57个字符串 (7个文件) - **已完成**
- **✅ Memory模块**: 91个字符串 (9个文件) - **已完成**
- **✅ Common组件模块**: 16个字符串 (4个文件) - **已完成**
- **✅ Packages/Skills模块 (部分)**: 83个字符串 (15个文件) - **部分完成**
- **✅ Settings模块 (部分)**: 32个字符串 (8个文件) - **部分完成**
- **✅ Chat模块 (部分)**: 24个字符串 (10个文件) - **部分完成**
- **✅ Demo模块 (部分)**: 8个字符串 (2个文件) - **部分完成**
- **✅ Toolbox模块 (部分)**: 27个字符串 (8个文件) - **部分完成**
- **🔄 Packages/Skills模块 (剩余)**: 417+个字符串 (25+个文件) - **待处理**
- **🔄 Chat模块 (剩余)**: 1200+个字符串 (89个文件) - **待处理**
- **🔄 Workflow模块**: 274个字符串 (6个文件) - **待处理**
- **🔄 Settings模块 (剩余)**: 460+个字符串 (40个文件) - **待处理**
- **🔄 Toolbox模块 (剩余)**: 670+个字符串 (42个文件) - **待处理**
- **🔄 Demo模块 (剩余)**: 125个字符串 (7个文件) - **待处理**
- **🔄 Token模块**: 92个字符串 (8个文件) - **待处理**
- **🔄 Common模块**: 100+个字符串 - **待处理**

### 总体进度
- **已完成**: ~336个字符串 (63个文件)
- **待处理**: ~3325+个字符串 (215+个文件)
- **完成率**: ~9.2%

**总计**: 约 3700+ 处中文硬编码字符串需要国际化

---

## 建议优先级 (更新于 2025-01-26)

### 高优先级（用户直接可见的核心界面）
1. ✅ **Floating窗口模块** - 悬浮窗界面 (已完成)
2. ✅ **Memory模块** - 记忆管理界面 (104个字符串，8个文件) - **已完成**
3. ✅ **Common组件** - 通用UI组件 (16个字符串，4个文件) - **已完成**

4. 🔄 **Packages/Skills模块 - 关键文件**
   - ✅ MCPConfigScreen.kt (11个字符串) - **已完成**
   - ✅ PackageManagerScreen.kt (8个字符串) - **已完成**
   - PackageDetailsDialog.kt
   - MCPMarketScreen.kt
   - SkillManagerScreen.kt

5. 🔄 **Chat模块 - 关键文件**
   - CustomXmlRenderer.kt (181个字符串)
   - ChatViewModel.kt (79个字符串)
   - ExportDialogs.kt (68个字符串)
   - LocalWebServer.kt (64个字符串)
   - BubbleUserMessageComposable.kt (64个字符串)
   - UserMessageComposable.kt (63个字符串)

### 中优先级（辅助功能和配置界面）
6. 🔄 **Settings模块 - 配置界面** (460+个字符串)
   - ModelApiSettingsSection.kt (79个字符串)
   - ThemeSettingsScreen.kt (68个字符串)
   - TagMarketScreen.kt (50个字符串)
   - ModelPromptsSettingsScreen.kt (46个字符串)

7. 🔄 **Workflow模块** - 工作流界面 (274个字符串)
8. 🔄 **Toolbox模块 - 关键文件** (670+个字符串)
   - AppPermissionsScreen.kt (230个字符串)
   - ToolTesterScreen.kt (120个字符串)

### 低优先级（后台/日志/开发工具）
9. **Chat模块 - 代码编辑器支持**
   - KotlinSupport.kt (109个字符串) - 语法高亮配置
   - HtmlSupport.kt (101个字符串) - HTML支持
   - JavaScriptSupport.kt (85个字符串) - JS支持
   - WorkspaceUtils.kt (304个字符串) - 工作区工具

10. **Toolbox模块 - 开发工具**
    - FileUtils.kt (60个字符串) - 文件工具
    - FileContextMenu.kt (56个字符串) - 右键菜单

11. **ViewModel类** - 包含大量中文字符串，主要是模板和配置
    - MCPMarketViewModel.kt (118个字符串)
    - SkillMarketViewModel.kt (95个字符串)
    - WorkflowViewModel.kt (113个字符串)
    - AutoGlmViewModel.kt (38个字符串)

### 可选择性处理
- **Demo模块** - 示例/测试界面 (125个字符串)
- **Token模块** - Token配置 (92个字符串)
- **VirtualDisplayOverlay.kt** - 包含大量日志，可选择性处理UI部分

---

## 处理建议

1. **创建新的字符串资源文件**:
   - `app/src/main/res/values/strings_i18n.xml` - 新增需要国际化的字符串
   - `app/src/main/res/values-en/strings_i18n.xml` - 英文翻译

2. **批量替换策略**:
   - 使用IDE的"Replace in Path"功能
   - 先替换高频出现的字符串（如"关闭"、"取消"、"确定"等）
   - 按模块逐步处理，避免一次性修改过多

3. **测试验证**:
   - 每完成一个模块，切换语言验证效果
   - 特别注意字符串插值（如 `${xxx}`）的位置

4. **特殊注意**:
   - 保留原字符串中的变量插值
   - 注意字符串中的格式化符号（换行符、引号等）
   - 某些技术术语（如 "API"、"MCP"）不需要翻译

---

**生成时间**: 2025-01-26
**最后更新**: 2025-01-27 深夜
**工具**: Bash脚本 + 手动整理 + Claude辅助
**数据来源**: 正则表达式搜索，排除注释、AppLogger、已国际化字符串

**更新记录**:
- 2025-01-26: 初始统计
- 2025-01-27 深夜: 新增11个文件，49个字符串，清理4个重复项

