package com.codewiz.wordloop.data.review

import com.codewiz.wordloop.data.prefs.ReviewSnapshot
import com.codewiz.wordloop.data.prefs.UserPrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class ReviewRequestManager @Inject constructor(
    private val prefs: UserPrefs,
) {
    private val mutex = Mutex()
    private val _pending = MutableStateFlow(false)
    val pendingReviewRequest: StateFlow<Boolean> = _pending.asStateFlow()

    suspend fun recordWordAdded() {
        update { it.copy(wordsAddedCount = it.wordsAddedCount + 1) }
        checkTrigger1()
    }

    suspend fun recordWordDetailViewed() {
        update { snap ->
            if (snap.hasViewedWordDetail) snap else snap.copy(hasViewedWordDetail = true)
        }
        checkTrigger1()
    }

    suspend fun recordReviewSessionCompleted() {
        update { it.copy(reviewSessionsCompletedCount = it.reviewSessionsCompletedCount + 1) }
        checkTrigger2()
    }

    suspend fun recordWordMastered() {
        update { snap ->
            if (snap.hasMasteredWord) snap else snap.copy(hasMasteredWord = true)
        }
        checkTrigger3()
    }

    fun clearPendingRequest() {
        _pending.value = false
    }

    private suspend fun checkTrigger1() {
        mutex.withLock {
            val snap = prefs.reviewSnapshot()
            if (!snap.trigger1Fired && snap.wordsAddedCount >= 2 && snap.hasViewedWordDetail) {
                prefs.writeReviewSnapshot(snap.copy(trigger1Fired = true))
                _pending.value = true
            }
        }
    }

    private suspend fun checkTrigger2() {
        mutex.withLock {
            val snap = prefs.reviewSnapshot()
            if (!snap.trigger2Fired && snap.reviewSessionsCompletedCount >= 2) {
                prefs.writeReviewSnapshot(snap.copy(trigger2Fired = true))
                _pending.value = true
            }
        }
    }

    private suspend fun checkTrigger3() {
        mutex.withLock {
            val snap = prefs.reviewSnapshot()
            if (!snap.trigger3Fired && snap.hasMasteredWord) {
                prefs.writeReviewSnapshot(snap.copy(trigger3Fired = true))
                _pending.value = true
            }
        }
    }

    private suspend fun update(transform: (ReviewSnapshot) -> ReviewSnapshot) {
        mutex.withLock {
            prefs.writeReviewSnapshot(transform(prefs.reviewSnapshot()))
        }
    }
}
