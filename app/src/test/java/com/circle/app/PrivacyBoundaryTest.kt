package com.circle.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Lint-style checks that fail the build (MASTER_PROMPT 2.1.3 and 2.3):
 * - nothing in ui/, navigation/ or the activity touches the store or seed data directly;
 *   screens only see the repository's redacted views, through ViewModels;
 * - no source logs anything (so note, comment and message text can never reach a log).
 */
class PrivacyBoundaryTest {

    private val main = File("src/main/java/com/circle/app").also {
        check(it.isDirectory) { "Run from the app module (found ${File(".").absolutePath})" }
    }

    private fun kotlinFiles(dir: File) = dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private val uiSide: List<File>
        get() = kotlinFiles(File(main, "ui")) + kotlinFiles(File(main, "navigation")) + File(main, "MainActivity.kt")

    private val forbiddenInUi = listOf(
        Regex("""\bCircleStore\b"""),
        Regex("""\bChatSeed\b"""),
        Regex("""\bSeed\."""),
        Regex("""\bFakeCircleRepository\b"""),
        Regex("""import\s+com\.circle\.app\.data\.\*"""), // a wildcard would bring the store into scope
    )

    @Test fun `ui never imports or touches the store`() {
        val offenders = uiSide.flatMap { f ->
            val text = f.readText()
            forbiddenInUi.filter { it.containsMatchIn(text) }.map { "${f.relativeTo(main)}: ${it.pattern}" }
        }
        assertTrue("UI reaches past the repository:\n" + offenders.joinToString("\n"), offenders.isEmpty())
        assertTrue("expected to scan the UI sources", uiSide.size > 20)
    }

    @Test fun `nothing is logged`() {
        val logging = listOf(Regex("""\bLog\.[vdiwe]\("""), Regex("""\bprintln\("""), Regex("""printStackTrace\("""), Regex("""\bTimber\b"""))
        val offenders = kotlinFiles(main).flatMap { f ->
            val text = f.readText()
            logging.filter { it.containsMatchIn(text) }.map { "${f.relativeTo(main)}: ${it.pattern}" }
        }
        assertTrue("Logging found (note, comment and message text must never be logged):\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    /** A mutual-friend count: the old field, or a number/placeholder followed by "mutual friend(s)". */
    private val mutualCount = Regex("""mutualFriends|(\d|\}|\$\w+|%\d?\$?d) mutual friend""", RegexOption.IGNORE_CASE)

    @Test fun `no mutual-friends number anywhere`() {
        val offenders = (kotlinFiles(main) + File("src/main/res/values").listFiles().orEmpty().filter { it.extension == "xml" })
            .filter { mutualCount.containsMatchIn(it.readText()) }
            .map { it.name }
        assertTrue("Mutual-friend counts are gone from this version: $offenders", offenders.isEmpty())
    }
}
