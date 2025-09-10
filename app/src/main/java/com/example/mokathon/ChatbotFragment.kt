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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class ChatbotFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var deleteHistoryButton: ImageView
    private lateinit var startMessageTextView: TextView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputLayout: View
    private lateinit var rootLayout: View // 루트 레이아웃 추가
    private val messages = mutableListOf<Message>()

    private lateinit var functions: FirebaseFunctions

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)

        // 루트 레이아웃 참조 추가
        rootLayout = view.findViewById(R.id.chatbot)
        inputLayout = view.findViewById(R.id.layout_input)
        recyclerView = view.findViewById(R.id.recyclerView_chatbot)
        editText = view.findViewById(R.id.editText_message)
        sendButton = view.findViewById(R.id.button_send)
        deleteHistoryButton = view.findViewById(R.id.button_delete_history)
        startMessageTextView = view.findViewById(R.id.tv_start_message)

        functions = Firebase.functions("us-central1")

        // UI 설정 코드
        val startColor = ContextCompat.getColor(requireContext(), R.color.gradient_blue_start)
        val endColor = ContextCompat.getColor(requireContext(), R.color.gradient_blue_end)
        val shader = LinearGradient(0f, 0f, startMessageTextView.paint.measureText(startMessageTextView.text.toString()), startMessageTextView.textSize, intArrayOf(startColor, endColor), null, Shader.TileMode.CLAMP)
        startMessageTextView.paint.shader = shader
        messageAdapter = MessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = messageAdapter
        if (messages.isEmpty()) {
            startMessageTextView.visibility = View.VISIBLE
        } else {
            startMessageTextView.visibility = View.GONE
        }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 키보드 인셋 리스너를 루트 뷰에 설정
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            if (imeVisible) {
                // 키보드가 보일 때: inputLayout의 bottom margin을 키보드 높이에서 기존 margin을 뺀 값으로 설정
                val layoutParams = inputLayout.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                val originalBottomMargin = 8.dpToPx() // XML에서 설정된 기존 marginBottom (8dp)
                layoutParams.bottomMargin = imeHeight - originalBottomMargin
                inputLayout.layoutParams = layoutParams
            } else {
                // 키보드가 사라질 때: 원래 margin으로 복구
                val layoutParams = inputLayout.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                layoutParams.bottomMargin = 8.dpToPx() // 원래 marginBottom으로 복구
                inputLayout.layoutParams = layoutParams
            }

            // 키보드가 올라왔을 때 최신 메시지로 스크롤
            if (imeVisible && messages.isNotEmpty()) {
                recyclerView.postDelayed({
                    recyclerView.scrollToPosition(messages.size - 1)
                }, 100)
            }

            insets
        }
    }

    // dp를 px로 변환하는 확장 함수
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun sendMessage(text: String) {
        val userMessage = Message(text, "user", System.currentTimeMillis())
        messages.add(userMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
        startMessageTextView.visibility = View.GONE

        // 로딩 메시지 추가
        val botMessage = Message("", "model", System.currentTimeMillis(), isLoading = true)
        messages.add(botMessage)
        val botMessageIndex = messages.size - 1
        messageAdapter.notifyItemInserted(botMessageIndex)
        recyclerView.scrollToPosition(botMessageIndex)

        val data = hashMapOf("query" to text)

        functions.getHttpsCallable("askRAG")
            .call(data)
            .addOnSuccessListener { result ->
                val answer = (result.data as? Map<*, *>)?.get("answer") as? String ?: "답변을 가져오지 못했습니다."
                val successMessage = Message(answer, "model", System.currentTimeMillis(), isLoading = false)
                messages[botMessageIndex] = successMessage
                messageAdapter.notifyItemChanged(botMessageIndex)
                recyclerView.scrollToPosition(botMessageIndex)
            }
            .addOnFailureListener { e ->
                Log.e("ChatbotFragment", "Error calling RAG function", e)
                val errorMessage = Message("Error: " + e.message, "model", System.currentTimeMillis())
                messages[botMessageIndex] = errorMessage
                messageAdapter.notifyItemChanged(botMessageIndex)
                recyclerView.scrollToPosition(botMessageIndex)
            }
    }

    private fun clearChatHistory() {
        messages.clear()
        messageAdapter.notifyDataSetChanged()
        startMessageTextView.visibility = View.VISIBLE
    }
}