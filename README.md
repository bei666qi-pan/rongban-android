# 绒伴 Rongban

原生 Android 情境记忆语音桌宠，课程 C3 + P4。开发进度通过功能分支和 PR 记录。

## 运行

Android 10 及以上；离线语音支持 ARM64、x86_64。演示环境为 Android 15 模拟器。安装一个 APK 后，桌面提供「绒伴」与「绒伴卡片」两个入口。

首次进入「我的」页面填写 API Key，可修改协议、Base URL 和模型名。安装包不含凭证；离线语音、记忆、专注与卡片不需要 Key。录音第一次准备模型需要数秒，等出现「正在听」后说话，识别结果可编辑后发送。

构建要求 JDK 17 或 21、Android SDK 35。语音资源采用固定版本和 SHA256 检查，从第三方下载后放到忽略目录。

```sh
python3 scripts/prepare_speech.py
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew :app:connectedDebugAndroidTest
```

如本机使用网络代理，可通过 `python3 scripts/build.py assembleDebug` 调用，代理配置不写入仓库。调试设备密钥注入请参阅 `scripts/provision_key.py --help`，仅用于本机联调。

## 项目结构

- `app`：陪伴、对话、记忆、技能、设置、悬浮桌宠与专注提醒。
- `core`：Room、DataStore、Keystore、语音、指令、记忆检索、模型协议与卡片渲染。
- `cards`：第二桌面入口、共享内容选取、分页预览、PNG / ZIP 保存与分享。
- `docs`：开发文档、测试结果、关键代码讲解与实际界面证据。
- `third_party`：上游版本、下载校验和测试人声来源；详见 THIRD_PARTY.md。

## PR 与课程交付

六组 PR 依次覆盖工程原型、本地数据、语音指令、悬浮桌宠、情境记忆和卡片交付。它们按依赖堆叠，审查时从 PR 1 到 PR 6 合并。

CI 执行编译、Lint、核心逻辑和设备测试，并提供可安装 APK。真实接口测试只有在本机调试设备配置了 Key 后运行，CI 不注入凭证。演示录屏用专门的 paced walkthrough，默认不会在 CI 执行。

公开发布源码需继续保留 AGPL-3.0 与上游来源。当前语音质量和性能仅在模拟器验收，识别可能有误字，后续应补充真实手机和不同说话者的测试。

## 完整版本与下载

完整实现位于 `feat/06-cards-delivery`，六组 PR 已开放审查。`main` 保留初始基线，按依赖顺序合并后获得完整版本。

课程交付包见私有仓库的 [v0.1.0-course 发布页](https://github.com/bei666qi-pan/rongban-android/releases/tag/v0.1.0-course)，包含离线模型 APK、源码压缩包、10 页展示 PPT、开发文档 PDF 和两段约三分钟的真实演示。安装包不含 API Key，需在「我的」自行配置。

最终自动化验收：14 项 JVM 测试、8 项 Android 设备测试通过；MiMo 真实接口与跨会话引用通过。详细证据和环境限制见 [验收报告](docs/验收报告.md)。
