package com.github.mr04vv.kondatecalendar.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueTest {

    private val env = "アプリ: 1.0 (1)\n端末: Xiaomi 2506BPN68R / Android 16"

    @Test
    fun draftNeedsATitle() {
        assertFalse(FeedbackDraft(FeedbackKind.FEATURE, "  ", "詳細").isSendable)
        assertTrue(FeedbackDraft(FeedbackKind.FEATURE, "お気に入り", "").isSendable)
    }

    @Test
    fun featureRequestGetsTheEnhancementLabel() {
        val issue = toNewIssue(FeedbackDraft(FeedbackKind.FEATURE, "週の合計を出したい", "買い物の目安に"), env)
        assertEquals(listOf("enhancement"), issue.labels)
    }

    @Test
    fun bugReportGetsTheBugLabel() {
        val issue = toNewIssue(FeedbackDraft(FeedbackKind.BUG, "落ちる", ""), env)
        assertEquals(listOf("bug"), issue.labels)
    }

    @Test
    fun titleIsTrimmed() {
        val issue = toNewIssue(FeedbackDraft(FeedbackKind.FEATURE, "  週の合計を出したい \n", ""), env)
        assertEquals("週の合計を出したい", issue.title)
    }

    @Test
    fun bodyHasTheDetailThenTheEnvironment() {
        val issue = toNewIssue(FeedbackDraft(FeedbackKind.BUG, "落ちる", " 保存を押すと落ちる \n"), env)
        assertEquals("保存を押すと落ちる\n\n---\n$env\n送信元: アプリ内フォーム", issue.body)
    }

    @Test
    fun emptyDetailLeavesOnlyTheEnvironment() {
        val issue = toNewIssue(FeedbackDraft(FeedbackKind.FEATURE, "要望", "   "), env)
        assertEquals("---\n$env\n送信元: アプリ内フォーム", issue.body)
    }

    @Test
    fun createdIssueIsReadFromTheResponse() {
        val json = """{"id":1,"number":12,"html_url":"https://github.com/mr04vv/kondate-calendar/issues/12","title":"x"}"""
        assertEquals(CreatedIssue(12, "https://github.com/mr04vv/kondate-calendar/issues/12"), parseCreatedIssue(json))
    }

    @Test
    fun errorMessageIsReadFromGitHubErrors() {
        assertEquals("Bad credentials", parseErrorMessage("""{"message":"Bad credentials","documentation_url":"x"}"""))
    }

    @Test
    fun errorMessageFallsBackToTheRawText() {
        assertEquals("Service Unavailable", parseErrorMessage("Service Unavailable"))
    }
}
