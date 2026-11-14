package com.example.githubmovil

import com.example.githubmovil.data.PullRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PullRequestTest {
    @Test
    fun compositeKeyFormatsCorrectly() {
        val pr = PullRequest(
            repoFullName = "Vasquezjuan7/smartwatchgithub",
            number = 42,
            title = "Test PR",
            author = "dev",
            headBranch = "feat/test",
            baseBranch = "main",
            body = "Desc",
            updatedAt = "2026-11-18T00:00:00Z",
            draft = false
        )
        assertEquals("Vasquezjuan7/smartwatchgithub#42", pr.key)
        assertFalse(pr.draft)
    }
}
