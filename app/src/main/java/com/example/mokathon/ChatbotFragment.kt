package com.example.mokathon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class ChatbotFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var deleteHistoryButton: ImageButton
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val database = FirebaseDatabase.getInstance().getReference("messages")

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

        messageAdapter = MessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = messageAdapter

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
    }

    private fun listenForMessages() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
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

