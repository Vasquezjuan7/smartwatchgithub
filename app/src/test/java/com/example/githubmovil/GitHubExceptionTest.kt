package com.example.githubmovil

import com.example.githubmovil.data.GitHubException
import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubExceptionTest {
    @Test
    fun propagatesStatusCodeAndMessage() {
        val ex = GitHubException(404, "Repository Not Found")
        assertEquals(404, ex.code)
        assertEquals("Repository Not Found", ex.message)
    }
}
