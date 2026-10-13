package io.github.bei666qipan.rongban.core

object BuiltinSkills {
 val all=listOf(
 Skill("remember","记住一件事","把重要偏好与事实留在本地。","请说：记住，我喜欢……","remember",builtin=true),
 Skill("recall","回忆近况","找回之前告诉绒伴的事情。","请说：回忆，或回忆某个关键词。","recall",builtin=true),
 Skill("focus","开始专注","设定专注时间，结束时提醒。","请说：开始专注二十五分钟。","focus",builtin=true),
 Skill("review","今日回顾","从今天的对话中整理值得记住的片段。","依据今天的真实对话整理一份温柔简短的回顾。不要编造经历。","review",builtin=true),
 Skill("card","生成卡片","选择对话或记忆，生成可以分享的卡片。","选择需要保存的内容，打开卡片工作室。","card",builtin=true),
 Skill("encourage","鼓励陪伴","结合你的心情与记忆，给你一点支持。","请结合用户当前心情和记忆给出具体、简短、自然的鼓励，避免空泛口号。","encourage",builtin=true))
 fun parse(content:String):Skill {
  require(content.toByteArray().size<=64*1024) { "技能文件不能超过 64KB" }
  val front=SkillFrontmatterParser.parse(content)
  val name=front["name"]?.trim().orEmpty();val description=front["description"]?.trim().orEmpty()
  require(name.isNotBlank() && name.length<=80 && description.isNotBlank()) { "请提供 YAML name、description 和 Markdown 正文" }
  val body=SkillFrontmatterParser.extractBody(content).trim();require(body.isNotBlank()) { "技能正文不能为空" }
  return Skill(newId(),name,description,body)
 }
}
