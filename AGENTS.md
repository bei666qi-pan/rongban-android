# 项目约定

## 架构

- `app`：主界面、`AppViewModel`、悬浮前台服务与专注提醒；界面通过 ViewModel 和 Flow 访问数据与能力。
- `core`：Room 数据、DataStore 配置、Keystore 凭证、离线语音、指令解析、情境记忆、模型协议与卡片渲染。
- `cards`：第二个桌面入口；使用共享数据库和卡片渲染，不依赖主 Activity 已运行。
- 保持用户主动触发技能与录音；模型提取的记忆和摘要须经用户确认，删除或停用后不得继续跨会话引用。
- 架构或模块边界变化时同步更新本文件及 `docs/开发文档.md`；开源来源与模型权重授权见 `THIRD_PARTY.md`。

## 构建与验证

- 使用 Gradle Wrapper、JDK 17 或 21 和 Android SDK 35；首次构建先运行 `python3 scripts/prepare_speech.py`。
- 核心测试：`./gradlew :core:testDebugUnitTest`；设备测试：`./gradlew :app:connectedDebugAndroidTest`。按变更范围选择必要检查，完整 CI 见 `.github/workflows/android.yml`。
- 不将 API Key、模型生成文件或本机凭证提交到 Git；联网模型测试使用本地设备凭证。

## 提交署名

- AI 参与的提交使用实际参与模型的 `Co-Authored-By` trailer，不冒用其他模型或人员。
