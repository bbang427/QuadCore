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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

class ChatbotFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var deleteHistoryButton: ImageButton
    private lateinit var startMessageTextView: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var generativeModel: GenerativeModel

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

        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

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

    private fun sendMessage(text: String) {
        val userMessage = Message(text, "user", System.currentTimeMillis())
        messages.add(userMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
        startMessageTextView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val botMessage = Message("", "model", System.currentTimeMillis())
                messages.add(botMessage)
                val botMessageIndex = messages.size - 1
                messageAdapter.notifyItemInserted(botMessageIndex)
                recyclerView.scrollToPosition(botMessageIndex)

                var fullResponse = ""
                generativeModel.generateContentStream(text).collect { chunk ->
                    fullResponse += chunk.text
                    messages[botMessageIndex] = botMessage.copy(text = fullResponse)
                    messageAdapter.notifyItemChanged(botMessageIndex)
                    recyclerView.scrollToPosition(botMessageIndex)
                }
            } catch (e: Exception) {
                Log.e("ChatbotFragment", "Error sending message", e)
                val errorMessage = Message("Error: " + e.message, "model", System.currentTimeMillis())
                messages.add(errorMessage)
                messageAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun clearChatHistory() {
        messages.clear()
        messageAdapter.notifyDataSetChanged()
        startMessageTextView.visibility = View.VISIBLE
    }
}