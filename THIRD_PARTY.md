# 开源来源与复用

- RikkaHub: https://github.com/rikkahub/rikkahub, commit 0966502c4d65c1ee4b2a3e49a19703bb1978016c, AGPL-3.0。技能 YAML 解析与路径校验保留原始代码；记忆数据与模型流协议根据课程需求适配。派生项目遵循仓库 LICENSE。
- Now in Android: https://github.com/android/nowinandroid, Apache-2.0。参考分层、Flow 与 Compose 生命周期架构。
- sherpa-onnx v1.13.8: https://github.com/k2-fsa/sherpa-onnx, Apache-2.0。使用上游 Kotlin JNI 包装和 Android 动态库。模型下载来源与摘要记录在 third_party/speech-downloads。
- OkHttp / Room / Hilt / DataStore / Compose / Lottie Android：使用发布的库，不重复实现网络、持久化、依赖注入或动画引擎。

绒伴角色与界面为本项目原创。

模型来源单独记录，不将运行时许可证直接推定为权重许可证：

- 中文 ASR：[上游转换模型](https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30)，训练来源在模型自带 README.md。2026-10-13 查询的模型卡未注明权重许可证。本仓库不提交这些权重，课程演示 APK 按计划本地集成；进一步发布前需补齐上游权重授权说明。
- 中文 TTS：[AISHELL3 VITS 模型卡](https://huggingface.co/csukuangfj/icefall-tts-aishell3-vits-low-2024-04-06)，标注 Apache-2.0。
- ASR 回归测试人声样本来自同一 ASR 上游的 test_wavs/0.wav，保持原文件，SHA256 与来源见 third_party/asr-reference.json，仅用于验证关键识别文本。
