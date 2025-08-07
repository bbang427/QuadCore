package com.example.mokathon

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.core.content.res.ResourcesCompat

class CommunityFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private var postList = mutableListOf<Post>()
    private lateinit var postAdapter: PostAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvSortLatest: TextView
    private lateinit var tvSortLikes: TextView

    private var currentSort = Sort.LATEST // 현재 정렬 상태

    enum class Sort {
        LATEST,
        LIKES
    }

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
                    val currentUser = auth.currentUser ?: return
                    val isLiked = !post.likers.contains(currentUser.uid)

                    // 좋아요 상태를 Firestore에 업데이트하고, 성공 시 UI 갱신
                    updateLikeInFirestore(post.postId, isLiked, currentUser.uid) {
                        // Firestore 업데이트 성공 시 로컬 데이터와 UI 업데이트
                        val currentLikers = post.likers.toMutableList() // 불변 리스트를 가변 리스트로 변환
                        if (isLiked) {
                            currentLikers.add(currentUser.uid)
                        } else {
                            currentLikers.remove(currentUser.uid)
                        }

                        // Post 객체의 `likers` 필드에 변경사항을 반영 (Post 객체는 var 필드가 있어야 함)
                        // `Post` 데이터 클래스의 `likers` 필드가 `val`이면 직접 할당이 불가능합니다.
                        // 이 경우, `postList`에서 해당 아이템을 제거하고 새로운 객체로 대체해야 합니다.
                        // 현재 `Post`의 `likers`가 `val`이라고 가정하고 아래와 같이 처리합니다.
                        val updatedPost = post.copy(
                            likers = currentLikers,
                            likeCount = currentLikers.size,
                            isLiked = isLiked
                        )

                        postList[position] = updatedPost // 새 객체로 대체
                        postAdapter.notifyItemChanged(position)
                    }
                }
            },
            // 댓글 클릭 리스너 구현 (PostDetailActivity로 이동하도록 수정)
            object : PostAdapter.OnCommentClickListener {
                override fun onCommentClick(post: Post) {
                    val intent = Intent(activity, PostDetailActivity::class.java)
                    intent.putExtra("post", post)
                    startActivity(intent)
                }
            },
            object : PostAdapter.OnEditClickListener {
                override fun onEditClick(post: Post) {
                    val intent = Intent(activity, WritePostActivity::class.java).apply {
                        // 게시글 수정 모드임을 명시적으로 알리는 플래그 추가
                        putExtra("isEditing", true)
                        putExtra("post", post)
                    }
                    startActivity(intent)
                }
            },
            object : PostAdapter.OnDeleteClickListener {
                override fun onDeleteClick(post: Post) {
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

        // 정렬 버튼 초기화 및 리스너 설정
        tvSortLatest = view.findViewById(R.id.tv_sort_latest)
        tvSortLikes = view.findViewById(R.id.tv_sort_likes)

        tvSortLatest.setOnClickListener {
            currentSort = Sort.LATEST
            updateSortButtons()
            resetAndFetchPosts()
        }

        tvSortLikes.setOnClickListener {
            currentSort = Sort.LIKES
            updateSortButtons()
            resetAndFetchPosts()
        }

        updateSortButtons()
    }

    private fun updateSortButtons() {
        val context = requireContext() // 안전하게 Context를 가져옵니다.

        when (currentSort) {
            Sort.LATEST -> {
                tvSortLatest.setTextColor(ContextCompat.getColor(context, R.color.black))
                tvSortLatest.typeface = ResourcesCompat.getFont(context, R.font.pretendard_gov_semibold)

                tvSortLikes.setTextColor(ContextCompat.getColor(context, R.color.gray))
                tvSortLikes.typeface = ResourcesCompat.getFont(context, R.font.pretendard_gov_medium)
            }
            Sort.LIKES -> {
                tvSortLatest.setTextColor(ContextCompat.getColor(context, R.color.gray))
                tvSortLatest.typeface = ResourcesCompat.getFont(context, R.font.pretendard_gov_medium)

                tvSortLikes.setTextColor(ContextCompat.getColor(context, R.color.black))
                tvSortLikes.typeface = ResourcesCompat.getFont(context, R.font.pretendard_gov_semibold)
            }
        }
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
            .orderBy(if (currentSort == Sort.LATEST) "createdAt" else "likeCount", Query.Direction.DESCENDING)
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

                val currentUser = auth.currentUser
                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    post.postId = document.id
                    post.isLiked = currentUser?.uid?.let { post.likers.contains(it) } ?: false
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

    /**
     * Firestore에서 좋아요 상태를 업데이트하고, 완료 후 콜백을 실행하는 함수
     * @param postId 게시글 ID
     * @param isLiked 좋아요를 누를지(true) 취소할지(false) 여부
     * @param userId 현재 사용자 ID
     * @param onComplete 좋아요 업데이트 성공 시 실행될 콜백 함수
     */
    private fun updateLikeInFirestore(postId: String, isLiked: Boolean, userId: String, onComplete: () -> Unit) {
        val postRef = db.collection("posts").document(postId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likers = snapshot.get("likers") as? MutableList<String> ?: mutableListOf()
            if (isLiked) {
                likers.add(userId)
            } else {
                likers.remove(userId)
            }
            transaction.update(postRef, "likers", likers)
            transaction.update(postRef, "likeCount", likers.size.toLong())
            null
        }.addOnSuccessListener {
            onComplete() // 트랜잭션 성공 시 콜백 호출
        }.addOnFailureListener { e ->
            // 오류 처리
        }
    }
}