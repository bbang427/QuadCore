package com.example.mokathon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CommunityFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val db = Firebase.firestore
    private var postList = mutableListOf<Post>()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃 파일을 인플레이트하여 뷰 객체를 반환합니다.
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView 초기화
        recyclerView = view.findViewById(R.id.rv_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 어댑터 초기화 (빈 리스트로 먼저 초기화)
        postAdapter = PostAdapter(postList)
        recyclerView.adapter = postAdapter

        // 글쓰기 버튼 클릭 리스너 설정
        val fabAddPost: FloatingActionButton = view.findViewById(R.id.fab_add_post)
        fabAddPost.setOnClickListener {
            val intent = Intent(activity, WritePostActivity::class.java)
            startActivity(intent)
        }
    }

    // 화면이 다시 활성화될 때마다 게시글을 새로 불러오도록 함
    override fun onResume() {
        super.onResume()
        fetchPostsFromFirestore()
    }

    private fun fetchPostsFromFirestore() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                postList.clear() // 기존 리스트 초기화
                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    postList.add(post)
                }
                postAdapter.notifyDataSetChanged() // 어댑터에게 데이터 변경을 알림
            }
            .addOnFailureListener { e ->
                // 오류가 발생했을 경우 로그를 찍어볼 수 있습니다.
            }
    }
}