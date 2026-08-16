package com.codewiz.wordloop.ui.navigation

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.codewiz.wordloop.data.audio.PronunciationPlayer
import com.codewiz.wordloop.data.prefs.UserPrefs
import com.codewiz.wordloop.data.push.PushManager
import com.codewiz.wordloop.data.review.ReviewRequestManager
import com.codewiz.wordloop.data.store.WordLoopStore
import com.codewiz.wordloop.domain.model.LearnedWord
import com.codewiz.wordloop.ui.addword.AddWordContent
import com.codewiz.wordloop.ui.addword.AddWordViewModel
import com.codewiz.wordloop.ui.auth.AuthScreen
import com.codewiz.wordloop.ui.auth.AuthViewModel
import com.codewiz.wordloop.ui.components.ScreenBackground
import com.codewiz.wordloop.ui.components.WordLoopTabBar
import com.codewiz.wordloop.ui.library.LibraryScreen
import com.codewiz.wordloop.ui.onboarding.LanguagePicker
import com.codewiz.wordloop.ui.onboarding.OnboardingScreen
import com.codewiz.wordloop.ui.progress.ProgressScreen
import com.codewiz.wordloop.ui.quiz.QuizScreen
import com.codewiz.wordloop.ui.quiz.QuizViewModel
import com.codewiz.wordloop.ui.settings.SettingsScreen
import com.codewiz.wordloop.ui.theme.LocalAppLanguageCode
import com.codewiz.wordloop.ui.theme.WordLoopTheme
import com.codewiz.wordloop.ui.today.TodayScreen
import com.codewiz.wordloop.ui.word.WordDetailScreen
import com.codewiz.wordloop.util.AppUiLanguage
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import kotlinx.coroutines.launch

private sealed interface Destination {
    data object Tabs : Destination
    data class Detail(
        val word: LearnedWord,
        val list: List<LearnedWord>,
        val added: Boolean = false,
        val returnTo: Destination? = null,
    ) : Destination
    data class Quiz(val words: List<LearnedWord>, val mode: com.codewiz.wordloop.ui.quiz.QuizViewModel.Mode) : Destination
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLoopRoot(
    pendingPushType: String?,
    onPushConsumed: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    addWordViewModel: AddWordViewModel = hiltViewModel(),
    quizViewModel: QuizViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as Activity
    val focusManager = LocalFocusManager.current
    val app = activity.application
    val deps = remember {
        EntryPointAccessors.fromApplication(app, RootDeps::class.java)
    }
    val user by authViewModel.user.collectAsState()
    val checking by authViewModel.isChecking.collectAsState()
    val authLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.error.collectAsState()
    val profile by deps.store.userProfile.collectAsState()
    val due by deps.store.dueWords.collectAsState()
    val pendingReview by deps.reviewRequests.pendingReviewRequest.collectAsState()
    var tab by remember { mutableStateOf(AppTab.TODAY) }
    var destination by remember { mutableStateOf<Destination>(Destination.Tabs) }
    var showAdd by remember { mutableStateOf(false) }
    var showUpgrade by remember { mutableStateOf(false) }
    var languagePicker by remember { mutableStateOf<String?>(null) }
    var pickerSearch by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val languageCode = profile?.appLanguage ?: AppUiLanguage.fromSystemPreferred().code

    var permissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionCallback?.invoke(granted) }

    fun requestNotifications(cb: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || deps.pushManager.hasNotificationPermission()) {
            cb(true)
        } else {
            permissionCallback = cb
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            deps.store.refreshAll()
            deps.pushManager.syncOnForeground()
        } else {
            deps.store.clearLocalState()
        }
    }

    LaunchedEffect(pendingPushType, user, due) {
        if (user == null || pendingPushType == null) return@LaunchedEffect
        tab = AppTab.TODAY
        if (pendingPushType == PushManager.TYPE_REVIEW && due.isNotEmpty()) {
            destination = Destination.Quiz(due, QuizViewModel.Mode.REVIEW)
        }
        if (pendingPushType == PushManager.TYPE_WOTD) {
            deps.store.refreshWordOfTheDay()
        }
        onPushConsumed()
    }

    LaunchedEffect(pendingReview) {
        if (pendingReview) {
            runCatching {
                val manager = ReviewManagerFactory.create(activity)
                manager.requestReviewFlow().addOnSuccessListener { info ->
                    manager.launchReviewFlow(activity, info)
                }
            }
            deps.reviewRequests.clearPendingRequest()
        }
    }

    CompositionLocalProvider(
        LocalAppLanguageCode provides languageCode,
    ) {
        when {
            checking -> {
                Box(Modifier.fillMaxSize()) {
                    ScreenBackground()
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            user == null -> AuthScreen(
                isLoading = authLoading,
                error = authError,
                onClearError = authViewModel::clearError,
                onGoogle = authViewModel::signInWithGoogle,
                onApple = authViewModel::signInWithApple,
                onGuest = authViewModel::continueAsGuest,
                onEmailSignIn = authViewModel::signIn,
                onEmailSignUp = authViewModel::signUp,
                onForgotPassword = authViewModel::sendPasswordReset,
            )
            else -> {
                when (val dest = destination) {
                    is Destination.Quiz -> {
                        BackHandler {
                            destination = Destination.Tabs
                            scope.launch { deps.store.refreshAll() }
                        }
                        QuizScreen(
                            words = dest.words,
                            mode = dest.mode,
                            viewModel = quizViewModel,
                            player = deps.player,
                            onClose = {
                                destination = Destination.Tabs
                                scope.launch { deps.store.refreshAll() }
                            },
                            onOpenWord = { word ->
                                destination = Destination.Detail(word, listOf(word), returnTo = dest)
                            },
                        )
                    }
                    is Destination.Detail -> {
                        BackHandler { destination = dest.returnTo ?: Destination.Tabs }
                        WordDetailScreen(
                            initial = dest.word,
                            list = dest.list,
                            store = deps.store,
                            player = deps.player,
                            reviewRequests = deps.reviewRequests,
                            userId = user?.uid,
                            onPractice = { word ->
                                destination = Destination.Quiz(listOf(word), QuizViewModel.Mode.PRACTICE)
                            },
                            showDone = dest.added,
                            onDone = { destination = dest.returnTo ?: Destination.Tabs },
                            onBack = { destination = dest.returnTo ?: Destination.Tabs },
                        )
                    }
                    Destination.Tabs -> {
                        BackHandler(enabled = tab != AppTab.TODAY) { tab = AppTab.TODAY }
                        Scaffold(
                            bottomBar = {
                                WordLoopTabBar(
                                    selected = tab,
                                    onSelect = {
                                        focusManager.clearFocus()
                                        tab = it
                                    },
                                    onAdd = {
                                        focusManager.clearFocus()
                                        scope.launch { addWordViewModel.prepare() }
                                        showAdd = true
                                    },
                                )
                            },
                        ) { padding ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(
                                        top = padding.calculateTopPadding(),
                                        bottom = padding.calculateBottomPadding(),
                                    )
                                    .clipToBounds(),
                            ) {
                                when (tab) {
                                    AppTab.TODAY -> TodayScreen(
                                        store = deps.store,
                                        player = deps.player,
                                        onOpenWord = { destination = Destination.Detail(it, due) },
                                        onStartReview = {
                                            destination = Destination.Quiz(due, QuizViewModel.Mode.REVIEW)
                                        },
                                    )
                                    AppTab.LIBRARY -> LibraryScreen(
                                        store = deps.store,
                                        onOpenWord = { word, list -> destination = Destination.Detail(word, list) },
                                    )
                                    AppTab.PROGRESS -> ProgressScreen(deps.store)
                                    AppTab.SETTINGS -> SettingsScreen(
                                        store = deps.store,
                                        prefs = deps.prefs,
                                        pushManager = deps.pushManager,
                                        auth = deps.auth,
                                        isGuest = authViewModel.isGuest,
                                        displayEmail = authViewModel.displayEmail,
                                        userId = user?.uid,
                                        onSignOut = authViewModel::signOut,
                                        onUpgrade = { showUpgrade = true },
                                        onChangeLanguage = { language ->
                                            scope.launch {
                                                deps.prefs.setAppLanguage(language)
                                                deps.store.updateUserProfile(
                                                    com.codewiz.wordloop.data.api.UpdateUserProfileBody(appLanguage = language.code),
                                                )
                                            }
                                        },
                                        onOpenLearningLanguages = { languagePicker = "learning" },
                                        onOpenNativeLanguage = { languagePicker = "native" },
                                        notificationPermissionGranted = deps.pushManager.hasNotificationPermission(),
                                        onRequestNotificationPermission = ::requestNotifications,
                                    )
                                }
                            }
                        }
                    }
                }

                if (profile?.hasCompletedOnboarding == false) {
                    ModalBottomSheet(
                        onDismissRequest = {},
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        dragHandle = null,
                    ) {
                        OnboardingScreen(
                            store = deps.store,
                            pushManager = deps.pushManager,
                            onRequestNotificationPermission = ::requestNotifications,
                        )
                    }
                }

                if (showAdd) {
                    val addState by addWordViewModel.state.collectAsState()
                    ModalBottomSheet(
                        onDismissRequest = {
                            if (!addState.isLoading) {
                                showAdd = false
                                addWordViewModel.reset()
                            }
                        },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        if (addState.generatedWord != null) {
                            WordDetailScreen(
                                initial = addState.generatedWord!!,
                                list = listOf(addState.generatedWord!!),
                                store = deps.store,
                                player = deps.player,
                                reviewRequests = deps.reviewRequests,
                                userId = user?.uid,
                                onPractice = { word ->
                                    showAdd = false
                                    destination = Destination.Quiz(listOf(word), QuizViewModel.Mode.PRACTICE)
                                },
                                showDone = true,
                                applyStatusBars = false,
                                onDone = {
                                    showAdd = false
                                    addWordViewModel.reset()
                                },
                            )
                        } else {
                            AddWordContent(
                                state = addState,
                                languages = profile?.learningLanguages.orEmpty(),
                                onWordChange = addWordViewModel::updateWord,
                                onLanguage = addWordViewModel::selectLanguage,
                                onSubmit = { scope.launch { addWordViewModel.generate() } },
                                onDismiss = {
                                    showAdd = false
                                    addWordViewModel.reset()
                                },
                                onUseSuggestion = { scope.launch { addWordViewModel.useSuggestion() } },
                                onViewExisting = addWordViewModel::viewExisting,
                                onRegenerate = { scope.launch { addWordViewModel.regenerateExisting(it) } },
                                onCancelExisting = addWordViewModel::dismissExisting,
                            )
                        }
                    }
                }

                if (showUpgrade) {
                    ModalBottomSheet(onDismissRequest = { showUpgrade = false }) {
                        AuthScreen(
                            isLoading = authLoading,
                            error = authError,
                            onClearError = authViewModel::clearError,
                            onGoogle = authViewModel::signInWithGoogle,
                            onApple = authViewModel::signInWithApple,
                            onGuest = {},
                            onEmailSignIn = authViewModel::signIn,
                            onEmailSignUp = authViewModel::signUp,
                            onForgotPassword = authViewModel::sendPasswordReset,
                            compact = true,
                        )
                    }
                }

                if (languagePicker != null) {
                    ModalBottomSheet(onDismissRequest = {
                        languagePicker = null
                        pickerSearch = ""
                    }) {
                        val current = profile?.learningLanguages.orEmpty()
                        LanguagePicker(
                            title = if (languagePicker == "learning") "Word Languages" else "Your native language",
                            search = pickerSearch,
                            onSearch = { pickerSearch = it },
                            selected = if (languagePicker == "learning") current else listOfNotNull(profile?.nativeLanguage),
                            onSelect = { language ->
                                scope.launch {
                                    if (languagePicker == "learning") {
                                        val next = if (language in current) {
                                            if (current.size > 1) current - language else current
                                        } else current + language
                                        deps.store.updateUserProfile(
                                            com.codewiz.wordloop.data.api.UpdateUserProfileBody(learningLanguages = next),
                                        )
                                    } else {
                                        deps.store.updateUserProfile(
                                            com.codewiz.wordloop.data.api.UpdateUserProfileBody(nativeLanguage = language),
                                        )
                                        languagePicker = null
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface RootDeps {
    val store: WordLoopStore
    val prefs: UserPrefs
    val pushManager: PushManager
    val player: PronunciationPlayer
    val reviewRequests: ReviewRequestManager
    val auth: com.codewiz.wordloop.data.auth.AuthRepository
}
