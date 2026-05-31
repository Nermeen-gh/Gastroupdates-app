package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.db.BookmarkRepository
import com.example.db.GastroDatabase
import com.example.model.ChatMessage
import com.example.model.GastroDataRepository
import com.example.model.GastroUpdate
import com.example.network.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class GastroViewModel(
    application: Application,
    private val repository: BookmarkRepository
) : AndroidViewModel(application) {

    // Filter and search states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All") // All, Gastroenterology, Hepatology, Endoscopy
    val showBookmarkedOnly = MutableStateFlow(false)

    // Active detail modal/sheet state
    val selectedUpdateId = MutableStateFlow<String?>(null)

    // Master list of clinical updates compiled from repository combined with dynamic bookmarks
    val filteredUpdates: StateFlow<List<GastroUpdate>> = combine(
        searchQuery,
        selectedCategory,
        showBookmarkedOnly,
        repository.allBookmarks
    ) { query, category, bookmarkedOnly, bookmarkEntities ->
        val bookmarkIds = bookmarkEntities.map { it.updateId }.toSet()
        
        GastroDataRepository.updates.map { update ->
            update.copy(isBookmarked = bookmarkIds.contains(update.id))
        }.filter { update ->
            val matchesSearch = update.title.contains(query, ignoreCase = true) ||
                    update.summary.contains(query, ignoreCase = true) ||
                    update.fullDetails.contains(query, ignoreCase = true)
            
            val matchesCategory = category == "All" || update.category.equals(category, ignoreCase = true)
            val matchesBookmark = !bookmarkedOnly || update.isBookmarked
            
            matchesSearch && matchesCategory && matchesBookmark
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Bookmark Toggling logic ---
    fun toggleBookmark(updateId: String, isCurrentlyBookmarked: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyBookmarked) {
                repository.removeBookmark(updateId)
            } else {
                repository.addBookmark(updateId)
            }
        }
    }

    // --- FIB-4 calculator states ---
    val fibAge = MutableStateFlow("")
    val fibAst = MutableStateFlow("")
    val fibAlt = MutableStateFlow("")
    val fibPlatelets = MutableStateFlow("")
    val fib4Result = MutableStateFlow<Fib4ResultState>(Fib4ResultState.Empty)

    fun calculateFib4() {
        val age = fibAge.value.toDoubleOrNull()
        val ast = fibAst.value.toDoubleOrNull()
        val alt = fibAlt.value.toDoubleOrNull()
        val platelets = fibPlatelets.value.toDoubleOrNull()

        if (age == null || ast == null || alt == null || platelets == null) {
            fib4Result.value = Fib4ResultState.Error("Please enter valid positive numbers for all fields.")
            return
        }
        if (age <= 0 || ast <= 0 || alt <= 0 || platelets <= 0) {
            fib4Result.value = Fib4ResultState.Error("All clinical values must be greater than zero.")
            return
        }

        val result = (age * ast) / (platelets * Math.sqrt(alt))
        val interpretation = when {
            result < 1.30 -> "Low risk of advanced hepatic fibrosis. (FIB-4 < 1.30)"
            result <= 2.67 -> "Indeterminate / Intermediate risk zone. Consider additional screening (e.g., FibroScan) to rule out fibrosis. (FIB-4 1.30 - 2.67)"
            else -> "High risk of advanced hepatic fibrosis. Further clinical diagnostic assessment and hepatology consultation is advised. (FIB-4 > 2.67)"
        }
        fib4Result.value = Fib4ResultState.Success(result, interpretation)
    }

    fun clearFib4() {
        fibAge.value = ""
        fibAst.value = ""
        fibAlt.value = ""
        fibPlatelets.value = ""
        fib4Result.value = Fib4ResultState.Empty
    }

    // --- Maddrey's Discriminant Function (MDF) states ---
    val mdfPatientPt = MutableStateFlow("")
    val mdfControlPt = MutableStateFlow("")
    val mdfBilirubin = MutableStateFlow("")
    val mdfResult = MutableStateFlow<MdfResultState>(MdfResultState.Empty)

    fun calculateMdf() {
        val ptUser = mdfPatientPt.value.toDoubleOrNull()
        val ptCtrl = mdfControlPt.value.toDoubleOrNull()
        val bili = mdfBilirubin.value.toDoubleOrNull()

        if (ptUser == null || ptCtrl == null || bili == null) {
            mdfResult.value = MdfResultState.Error("Please enter valid positive numbers for all fields.")
            return
        }
        if (ptUser <= 0 || ptCtrl <= 0 || bili <= 0) {
            mdfResult.value = MdfResultState.Error("All measurements must be positive numbers.")
            return
        }

        val score = (4.6 * (ptUser - ptCtrl)) + bili
        val interpretation = if (score >= 32.0) {
            "Score: ${String.format("%.2f", score)} >= 32. Severe Alcoholic Hepatitis is diagnosed. High short-term mortality risk. Initiation of corticosteroids (such as Prednisolone 40 mg/day) should be evaluated immediately if no contraindications exist."
        } else {
            "Score: ${String.format("%.2f", score)} < 32. Non-severe Alcoholic Hepatitis. Low immediate mortality. Supportive care is standard; corticosteroids are not indicated based on current guidelines."
        }
        mdfResult.value = MdfResultState.Success(score, interpretation)
    }

    fun clearMdf() {
        mdfPatientPt.value = ""
        mdfControlPt.value = ""
        mdfBilirubin.value = ""
        mdfResult.value = MdfResultState.Empty
    }

    // --- Chat Room Interactive State Machine ---
    val chatInput = MutableStateFlow("")
    val customApiKey = MutableStateFlow("") // Let developer override in-app
    
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome_msg",
                text = "Welcome to GastroAssistant AI!\n\nI am configured with gastroenterology, hepatology, and endoscopy practice patterns and clinical datasets up to 2026.\n\nYou can ask me complex diagnostic questions, therapeutic dose inquiries, guideline clarifications, or help interprets scores. Try choosing a quick-start question below or compose your own query.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    fun sendChatMessage(text: String = chatInput.value) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return

        // If it came from search input, clear it
        if (prompt == chatInput.value) {
            chatInput.value = ""
        }

        val userMsgId = UUID.randomUUID().toString()
        val aiMsgId = UUID.randomUUID().toString()

        val newUserMessage = ChatMessage(id = userMsgId, text = prompt, isUser = true)
        val aiLoadingMessage = ChatMessage(id = aiMsgId, text = "Analyzing medical query...", isUser = false, isLoading = true)

        _chatMessages.value = _chatMessages.value + newUserMessage + aiLoadingMessage

        viewModelScope.launch {
            val keyOverride = customApiKey.value.ifBlank { null }
            val response = GeminiClient.generateClinicalResponse(prompt, keyOverride)
            
            // Replace loading state with real text
            _chatMessages.value = _chatMessages.value.map { msg ->
                if (msg.id == aiMsgId) {
                    msg.copy(
                        text = response,
                        isLoading = false,
                        isError = response.startsWith("API_KEY_ERROR:") || response.startsWith("Error from Gemini API:")
                    )
                } else msg
            }
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                id = "welcome_msg_reset",
                text = "Chat history cleared. I am ready to assist with your next gastroenterology or liver-related scientific query.",
                isUser = false
            )
        )
    }
}

// Result wrapper states
sealed interface Fib4ResultState {
    object Empty : Fib4ResultState
    data class Success(val score: Double, val interpretation: String) : Fib4ResultState
    data class Error(val message: String) : Fib4ResultState
}

sealed interface MdfResultState {
    object Empty : MdfResultState
    data class Success(val score: Double, val interpretation: String) : MdfResultState
    data class Error(val message: String) : MdfResultState
}

// Simple Factory provider
class GastroViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GastroViewModel::class.java)) {
            val database = GastroDatabase.getDatabase(application)
            val repository = BookmarkRepository(database.bookmarkDao())
            return GastroViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class context")
    }
}
