package com.example.mokathon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout // 이 부분을 import 해주세요.
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CommunityFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val db = Firebase.firestore
    private var postList = mutableListOf<Post>()
    private lateinit var postAdapter: PostAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // SwipeRefreshLayout 변수 추가

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

        postAdapter = PostAdapter(postList)
        recyclerView.adapter = postAdapter

        // SwipeRefreshLayout 초기화 및 리스너 설정
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            // 새로고침 로직
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
                        fetchPostsFromFirestore() // 다음 게시글 로드
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

    // onResume() 메서드는 `resetAndFetchPosts()`로 대체
    // 화면이 다시 활성화될 때마다 게시글을 새로 불러오고 싶다면, onResume()에서 `resetAndFetchPosts()`를 호출할 수 있습니다.
    // override fun onResume() {
    //     super.onResume()
    //     resetAndFetchPosts()
    // }

    // 새로고침을 위한 새로운 메서드
    private fun resetAndFetchPosts() {
        postList.clear() // 기존 리스트를 비웁니다.
        postAdapter.notifyDataSetChanged() // 어댑터에게 변경 사항을 알립니다.
        lastVisible = null // 페이지네이션 상태를 초기화합니다.
        isLastPage = false
        isLoading = false

        fetchPostsFromFirestore() // 첫 페이지부터 다시 로드합니다.
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
                swipeRefreshLayout.isRefreshing = false // 새로고침 애니메이션 중단

                if (result.isEmpty) {
                    isLastPage = true
                    return@addOnSuccessListener
                }

                lastVisible = result.documents[result.size() - 1]

                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    postList.add(post)
                }
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                isLoading = false
                swipeRefreshLayout.isRefreshing = false // 실패 시에도 애니메이션 중단
                // 오류 처리
            }
    }
}