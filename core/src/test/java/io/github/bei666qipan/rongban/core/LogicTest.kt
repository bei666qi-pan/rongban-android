package io.github.bei666qipan.rongban.core
import org.junit.Assert.*
import org.junit.Test
class LogicTest {
 @Test fun chineseAndArabicFocusSlots() { assertEquals(25,(CommandParser.parse("开始专注二十五分钟") as Command.Focus).minutes);assertEquals(12,(CommandParser.parse("专注12分钟") as Command.Focus).minutes);assertTrue(CommandParser.parse("专注200分钟") is Command.Missing) }
 @Test fun rememberDoesNotBecomeChat() { assertEquals("我喜欢抹茶",(CommandParser.parse("帮我记住：我喜欢抹茶") as Command.Remember).content);assertTrue(CommandParser.parse("记住") is Command.Missing);assertTrue(CommandParser.parse("你今天怎么样") is Command.Chat) }
 @Test fun disabledAndPendingMemoryNeverEntersContext() { val memories=listOf(Memory(title="偏好",content="抹茶",enabled=false),Memory(title="待审",content="咖啡",pending=true),Memory(title="饮食",content="清淡"));val selected=MemoryContext.retrieve(memories,"饮食");assertEquals(listOf("清淡"),selected.map { it.content }) }
 @Test fun relevantMemoryBeatsRecency() { val old=Memory(title="学校",content="农林大学",aliases="学校",updated=1);val recent=Memory(title="颜色",content="绿色",updated=99);assertEquals(old.id,MemoryContext.retrieve(listOf(recent,old),"学校").first().id) }
 @Test fun csvPreservesQuotesAndBlocksFormulaCells() { val csv=MemoryContext.csv(listOf(Memory(title="=1+2",content="我说\"你好\"\n明天")));assertTrue(csv.contains("'=1+2"));assertTrue(csv.contains("\"\"你好\"\""));assertTrue(csv.startsWith("\uFEFF")) }
 @Test fun skillRequiresValidMetadataAndBody() { assertThrows(IllegalArgumentException::class.java) { BuiltinSkills.parse("plain text") }; val s=BuiltinSkills.parse("---\nname: 温柔教练\ndescription: 帮助复习\n---\n用提问帮我复习");assertEquals("温柔教练",s.name);assertEquals("prompt",s.executor) }
}
