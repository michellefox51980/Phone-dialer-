package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ClarityFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveContactAndObserve_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repository = ClarityFirestoreRepository(firestore)

        val contact = ContactEntity(
            name = "Elena Vance",
            phoneNumber = "(415) 890-2314",
            normalizedNumber = "4158902314",
            label = "Mobile",
            email = "elena@vance.io",
            organization = "Vance Studio",
            isVip = true,
            importSource = "VCF_FILE"
        )
        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveContact(contact) }
        assertTrue("Expected saveContact to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val contactId = saveResult.getOrThrow()

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeContacts(uid).first { list -> list.any { it.id == contactId } }
        }
        assertTrue(observed.any { it.id == contactId && it.name == "Elena Vance" })
    }

    @Test
    fun addCallLogAndFilterRule_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("owner_${UUID.randomUUID()}@test.com")
        val repository = ClarityFirestoreRepository(firestore)

        val ruleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveFilterRule(
                FilterRuleEntity(
                    label = "Block Specific Spam",
                    pattern = "4155550199",
                    matchType = "EXACT",
                    action = "BLOCK",
                    isEnabled = true
                )
            )
        }
        assertTrue(ruleResult.isSuccess)

        val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addCallLog(
                CallLogEntity(
                    phoneNumber = "(415) 555-0199",
                    contactName = "Spam Caller",
                    direction = "MISSED",
                    filterVerdict = "BLOCKED",
                    matchedRuleLabel = "Block Specific Spam",
                    durationSeconds = 0
                )
            )
        }
        assertTrue(logResult.isSuccess)
        val logId = logResult.getOrThrow()

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCallLogs(uid).first { list -> list.any { it.id == logId } }
        }
        assertTrue(logs.any { it.id == logId && it.filterVerdict == "BLOCKED" })
    }

    @Test
    fun getContact_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_cross_${UUID.randomUUID()}@test.com")
        val repository = ClarityFirestoreRepository(firestore)
        val contactId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveContact(
                ContactEntity(
                    name = "Alice Private Contact",
                    phoneNumber = "(415) 111-2222",
                    normalizedNumber = "4151112222"
                )
            ).getOrThrow()
        }

        // Switch to Bob and attempt to read Alice's contact
        signInTestUser("bob_cross_${UUID.randomUUID()}@test.com")
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getContactById(aliceUid, contactId)
        }
        assertTrue("Cross-user read must fail", crossRead.isFailure)
        val fsEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeContacts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_unauth_${UUID.randomUUID()}@test.com")
        auth.signOut()
        val repository = ClarityFirestoreRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeContacts(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected exception when unauthenticated", caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
