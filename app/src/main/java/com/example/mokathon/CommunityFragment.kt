package com.example.mokathon

import android.app.AlertDialog
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
    private var postList = mutableListOf<Post>()
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

        // PostAdapter 초기화 시 좋아요 및 댓글 클릭 리스너 모두 전달
        postAdapter = PostAdapter(postList,
            // 좋아요 클릭 리스너 구현
            object : PostAdapter.OnLikeClickListener {
                override fun onLikeClick(position: Int, post: Post) {
                    // UI를 먼저 업데이트하여 사용자에게 즉각적인 피드백 제공
                    post.isLiked = !post.isLiked
                    if (post.isLiked) {
                        post.likeCount++
                    } else {
                        post.likeCount--
                    }
                    postAdapter.notifyItemChanged(position)

                    // Firebase Firestore 업데이트 로직 (실제 구현 필요)
                }
            },
            // 댓글 클릭 리스너 구현 (PostDetailActivity로 이동하도록 수정)
            object : PostAdapter.OnCommentClickListener {
                override fun onCommentClick(post: Post) {
                    val intent = Intent(activity, PostDetailActivity::class.java)

                    // 게시글 전체 데이터를 Intent에 담아 전달
                    intent.putExtra("post", post)

                    startActivity(intent)
                }
            },
            object : PostAdapter.OnEditClickListener {
                override fun onEditClick(post: Post) {
                    val intent = Intent(activity, WritePostActivity::class.java)
                    intent.putExtra("post", post)
                    startActivity(intent)
                }
            },
            object : PostAdapter.OnDeleteClickListener {
                override fun onDeleteClick(post: Post) {
                    // 삭제 확인 다이얼로그 표시
                    AlertDialog.Builder(requireContext())
                        .setTitle("게시물 삭제")
                        .setMessage("정말로 이 게시물을 삭제하시겠습니까?")
                        .setPositiveButton("삭제") { _, _ ->
                            deletePostFromFirestore(post)
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
            }
        )
        recyclerView.adapter = postAdapter

        // SwipeRefreshLayout 초기화 및 리스너 설정
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            resetAndFetchPosts()
        }

        // 스크롤 리스너 추가 (무한 스크롤 로직)
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

    override fun onResume() {
        super.onResume()
        // 화면에 다시 돌아올 때마다 데이터를 새로고침
        resetAndFetchPosts()
    }

    private fun resetAndFetchPosts() {
        postList.clear()
        postAdapter.notifyDataSetChanged()
        lastVisible = null
        isLastPage = false
        isLoading = false
        fetchPostsFromFirestore()
    }

    private fun deletePostFromFirestore(post: Post) {
        db.collection("posts").document(post.postId)
            .delete()
            .addOnSuccessListener {
                // 로컬 목록에서 게시물 제거 및 UI 업데이트
                val position = postList.indexOf(post)
                if (position != -1) {
                    postList.removeAt(position)
                    postAdapter.notifyItemRemoved(position)
                }
            }
            .addOnFailureListener { e ->
                // 오류 처리
            }
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
                    post.postId = document.id
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