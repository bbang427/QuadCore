package com.example.mokathon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CommunityFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val db = Firebase.firestore
    private var postList = mutableListOf<Post>() // mutableList로 변경
    private lateinit var postAdapter: PostAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // 페이지네이션을 위한 변수들
    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false
    private var isLastPage = false
    private val PAGE_SIZE = 20

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView 및 어댑터 초기화
        recyclerView = view.findViewById(R.id.rv_posts)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        // OnLikeClickListener 구현체를 전달하여 어댑터 초기화
        postAdapter = PostAdapter(postList, object : PostAdapter.OnLikeClickListener {
            override fun onLikeClick(position: Int, post: Post) {
                // 1. UI를 먼저 업데이트하여 사용자에게 즉각적인 피드백 제공
                post.isLiked = !post.isLiked
                if (post.isLiked) {
                    post.likeCount++
                } else {
                    post.likeCount--
                }
                postAdapter.notifyItemChanged(position)

                // 2. 서버에 좋아요/좋아요 취소 요청 (Firebase Firestore 업데이트)
                // 실제 구현 시에는 Firestore 업데이트 로직을 추가해야 합니다.
                // 예시:
                // val postRef = db.collection("posts").document(post.postId)
                // postRef.update("likeCount", post.likeCount, "isLiked", post.isLiked)
                //    .addOnSuccessListener { /* 성공적으로 업데이트됨 */ }
                //    .addOnFailureListener { /* 업데이트 실패 시 UI 롤백 또는 오류 메시지 표시 */ }
            }
        })
        recyclerView.adapter = postAdapter

        // SwipeRefreshLayout 초기화 및 리스너 설정
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            resetAndFetchPosts()
        }

        // 스크롤 리스너 추가 (기존 무한 스크롤 로직)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        fetchPostsFromFirestore()
                    }
                }
            }
        })

        // 글쓰기 버튼 클릭 리스너 설정
        val fabAddPost: FloatingActionButton = view.findViewById(R.id.fab_add_post)
        fabAddPost.setOnClickListener {
            val intent = Intent(activity, WritePostActivity::class.java)
            startActivity(intent)
        }

        // 초기 데이터 로드
        resetAndFetchPosts()
    }

    private fun resetAndFetchPosts() {
        postList.clear()
        // 어댑터에 변경 사항을 알리는 notifyDataSetChanged()를 호출하기 전에 postList.clear() 후에 호출해야합니다.
        postAdapter.notifyDataSetChanged()
        lastVisible = null
        isLastPage = false
        isLoading = false
        fetchPostsFromFirestore()
    }

    private fun fetchPostsFromFirestore() {
        if (isLoading) return
        isLoading = true

        var query = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())

        if (lastVisible != null) {
            query = query.startAfter(lastVisible!!)
        }

        query.get()
            .addOnSuccessListener { result ->
                isLoading = false
                swipeRefreshLayout.isRefreshing = false

                if (result.isEmpty) {
                    isLastPage = true
                    return@addOnSuccessListener
                }

                lastVisible = result.documents[result.size() - 1]

                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    // Firestore에서 좋아요 개수와 사용자의 좋아요 여부 데이터를 함께 가져와야 합니다.
                    // 현재 코드로는 isLiked 상태를 알 수 없으므로, 이 부분은 백엔드 로직에 따라 추가 구현이 필요합니다.
                    postList.add(post)
                }
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                isLoading = false
                swipeRefreshLayout.isRefreshing = false
                // 오류 처리
            }
    }
}