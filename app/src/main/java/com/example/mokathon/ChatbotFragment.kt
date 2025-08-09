package com.example.mokathon

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ChatbotFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var deleteHistoryButton: ImageView
    private lateinit var startMessageTextView: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private var generativeModel: GenerativeModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        recyclerView = view.findViewById(R.id.recyclerView_chatbot)
        editText = view.findViewById(R.id.editText_message)
        sendButton = view.findViewById(R.id.button_send)
        deleteHistoryButton = view.findViewById(R.id.button_delete_history)
        startMessageTextView = view.findViewById(R.id.tv_start_message)

        // 초기 문구에 파란색 그라데이션 적용
        val startColor = ContextCompat.getColor(requireContext(), R.color.gradient_blue_start)
        val endColor = ContextCompat.getColor(requireContext(), R.color.gradient_blue_end)

        val shader = LinearGradient(
            0f, 0f,
            startMessageTextView.paint.measureText(startMessageTextView.text.toString()),
            startMessageTextView.textSize,
            intArrayOf(startColor, endColor),
            null,
            Shader.TileMode.CLAMP
        )
        startMessageTextView.paint.shader = shader

        messageAdapter = MessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = messageAdapter

        if (messages.isEmpty()) {
            startMessageTextView.visibility = View.VISIBLE
        } else {
            startMessageTextView.visibility = View.GONE
        }

        fetchApiKeyAndInitChatbot()

        sendButton.setOnClickListener {
            val messageText = editText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                editText.text.clear()
            }
        }

        deleteHistoryButton.setOnClickListener {
            clearChatHistory()
        }

        return view
    }

    private fun fetchApiKeyAndInitChatbot() {
        val db = FirebaseFirestore.getInstance()
        db.collection("settings").document("apiKeys")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val apiKey = document.getString("geminiKey")
                    if (apiKey != null) {
                        generativeModel = GenerativeModel(
                            modelName = "gemini-1.5-flash",
                            apiKey = apiKey
                        )
                    } else {
                        Log.e("ChatbotFragment", "API key is null")
                        showApiKeyError()
                    }
                } else {
                    Log.e("ChatbotFragment", "No such document")
                    showApiKeyError()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ChatbotFragment", "Error getting documents: ", exception)
                showApiKeyError()
            }
    }

    private fun sendMessage(text: String) {
        val userMessage = Message(text, "user", System.currentTimeMillis())
        messages.add(userMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
        startMessageTextView.visibility = View.GONE

        lifecycleScope.launch {
            val botMessage = Message("", "model", System.currentTimeMillis(), isLoading = true)
            messages.add(botMessage)
            val botMessageIndex = messages.size - 1
            messageAdapter.notifyItemInserted(botMessageIndex)
            recyclerView.scrollToPosition(botMessageIndex)

            try {
                if (generativeModel == null) {
                    val errorMessage = Message("API 키를 가져오는 데 실패했습니다. 잠시 후 다시 시도해주세요.", "model", System.currentTimeMillis())
                    messages[botMessageIndex] = errorMessage
                    messageAdapter.notifyItemChanged(botMessageIndex)
                    recyclerView.scrollToPosition(botMessageIndex)
                    return@launch
                }

                var fullResponse = ""
                generativeModel?.generateContentStream(text)?.collect { chunk ->
                    fullResponse += chunk.text
                    messages[botMessageIndex] = botMessage.copy(text = fullResponse, isLoading = false)
                    messageAdapter.notifyItemChanged(botMessageIndex)
                    recyclerView.scrollToPosition(botMessageIndex)
                }
            } catch (e: Exception) {
                Log.e("ChatbotFragment", "Error sending message", e)
                val errorMessage = Message("Error: " + e.message, "model", System.currentTimeMillis())
                messages[botMessageIndex] = errorMessage
                messageAdapter.notifyItemChanged(botMessageIndex)
                recyclerView.scrollToPosition(botMessageIndex)
            }
        }
    }

    private fun showApiKeyError() {
        val errorMessage = Message("API 키를 불러오는 중 오류가 발생했습니다. 앱을 다시 시작하거나 관리자에게 문의하세요.", "model", System.currentTimeMillis())
        messages.add(errorMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun clearChatHistory() {
        messages.clear()
        messageAdapter.notifyDataSetChanged()
        startMessageTextView.visibility = View.VISIBLE
    }
}