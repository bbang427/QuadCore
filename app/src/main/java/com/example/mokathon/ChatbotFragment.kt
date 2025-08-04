package com.example.mokathon

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatbotFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var deleteHistoryButton: ImageButton
    private lateinit var startMessageTextView: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val database = FirebaseDatabase.getInstance().getReference("messages")
    private var isFirstMessageLoaded = false

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

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount.toInt() == 0) {
                    startMessageTextView.visibility = View.VISIBLE
                } else {
                    startMessageTextView.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction { startMessageTextView.visibility = View.GONE }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

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

        listenForMessages()

        return view
    }

    private fun sendMessage(text: String) {
        val message = Message(text, System.currentTimeMillis(), "user")
        database.push().setValue(message)
    }

    private fun clearChatHistory() {
        database.removeValue()
        messages.clear()
        messageAdapter.notifyDataSetChanged()

        if (startMessageTextView.visibility != View.VISIBLE) {
            startMessageTextView.animate()
                .alpha(1f)
                .setDuration(500)
                .withStartAction { startMessageTextView.visibility = View.VISIBLE }
        }

        isFirstMessageLoaded = false
    }

    private fun listenForMessages() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    if (!isFirstMessageLoaded) {
                        if (messages.isEmpty()) {
                            startMessageTextView.animate()
                                .alpha(0f)
                                .setDuration(500)
                                .withEndAction { startMessageTextView.visibility = View.GONE }
                        }
                        isFirstMessageLoaded = true
                    }

                    messages.add(message)
                    messageAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.post {
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}