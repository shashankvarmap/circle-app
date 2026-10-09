package com.circle.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Lint's HardcodedText only covers XML. In Compose the same rule is checked here: no user-visible
 * sentence is written in a ui/ source; it comes from res/values so it can be translated.
 * Templates made only of values and symbols ("+${n}", "@${handle}") are fine.
 */
class HardcodedTextTest {

    private val ui = File("src/main/java/com/circle/app/ui")

    // A string literal passed as visible text: Text("..."), contentDescription, onClickLabel and the label parameters.
    private val visible = Regex(
        """(\bText\(\s*|contentDescription = |onClickLabel = |description = |placeholder = |title = |subtitle = )"((?:[^"\\]|\\.)*)""""
    )
    private val template = Regex("""\$\{[^}]*\}|\$[A-Za-z_]\w*""")

    @Test fun `ui text comes from resources`() {
        val files = ui.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val offenders = files.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                val t = line.trim()
                if (t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")) return@mapIndexedNotNull null
                val words = visible.findAll(line).any { template.replace(it.groupValues[2], "").contains(Regex("[A-Za-z]{2}")) }
                if (words) "${f.relativeTo(ui)}:${i + 1}: ${t.take(120)}" else null
            }
        }
        assertTrue("Move these strings to res/values/strings.xml:\n" + offenders.joinToString("\n"), offenders.isEmpty())
        assertTrue("expected to scan the UI sources", files.size > 20)
    }
}
