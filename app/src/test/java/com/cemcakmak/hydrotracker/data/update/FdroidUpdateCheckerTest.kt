package com.cemcakmak.hydrotracker.data.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FdroidUpdateCheckerTest {
    @Test
    fun `F-Droid checker always leaves updates to F-Droid`() = runBlocking {
        assertEquals(InstallSource.F_DROID, FdroidUpdateChecker.source)
        assertEquals(UpdateStatus.Unsupported, FdroidUpdateChecker.check())
    }
}
