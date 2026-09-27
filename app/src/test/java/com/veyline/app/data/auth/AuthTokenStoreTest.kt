package com.veyline.app.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 验证 [AuthTokenStore] 的保存、清除和登录状态派生行为 */
class AuthTokenStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /** DataStore 内部写入协程挂在 backgroundScope 上，随每次 runTest 结束自动取消，无需手动清理 */
    private fun TestScope.createTokenStore(): AuthTokenStore {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFolder.newFile("auth.preferences_pb") },
        )
        return AuthTokenStore(dataStore)
    }

    /** 验证保存 Token 后可以读回同一个值，登录状态也随之变为已登录 */
    @Test
    fun saveToken_thenTokenIsReadableAndLoggedIn() = runTest {
        val tokenStore = createTokenStore()

        tokenStore.saveToken("token-a")

        assertEquals("token-a", tokenStore.getToken())
        assertTrue(tokenStore.isLoggedIn.first())
    }

    /** 验证保存 Token 后清除，Token 变回 null，登录状态也随之变回未登录 */
    @Test
    fun clearToken_afterSavingToken_removesTokenAndLogsOut() = runTest {
        val tokenStore = createTokenStore()
        tokenStore.saveToken("token-a")

        tokenStore.clearToken()

        assertNull(tokenStore.getToken())
        assertFalse(tokenStore.isLoggedIn.first())
    }

    /** 验证 Token 从一个非空值刷新为另一个非空值时，isLoggedIn 不会重复发出 true */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun isLoggedIn_whenTokenRefreshedToAnotherNonBlankValue_doesNotEmitDuplicateTrue() = runTest {
        val tokenStore = createTokenStore()
        val loginStates = mutableListOf<Boolean>()

        backgroundScope.launch {
            tokenStore.isLoggedIn.toList(loginStates)
        }
        runCurrent()

        tokenStore.saveToken("token-a")
        runCurrent()

        tokenStore.saveToken("token-b")
        runCurrent()

        assertEquals(listOf(false, true), loginStates)
    }

    /** 验证保存空白 Token 时拒绝写入并抛出参数异常 */
    @Test
    fun saveToken_withBlankToken_throwsIllegalArgumentException() = runTest {
        val tokenStore = createTokenStore()

        assertFailsWith<IllegalArgumentException> {
            tokenStore.saveToken("   ")
        }
    }
}
