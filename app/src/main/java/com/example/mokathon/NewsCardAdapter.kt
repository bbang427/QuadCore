package com.example.mokathon

import android.content.Intent
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Locale

class NewsCardAdapter : ListAdapter<NewsItem, NewsCardAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tv_description)
        private val pubDateTextView: TextView = itemView.findViewById(R.id.tv_pubDate)
        private val linkTextView: TextView = itemView.findViewById(R.id.tv_link)

        fun bind(newsItem: NewsItem) {
            titleTextView.text = Html.fromHtml(newsItem.title, Html.FROM_HTML_MODE_LEGACY)
            descriptionTextView.text = Html.fromHtml(newsItem.description, Html.FROM_HTML_MODE_LEGACY)
            pubDateTextView.text = formatDate(newsItem.pubDate)

            linkTextView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.link))
                itemView.context.startActivity(intent)
            }
        }

        // 날짜 형식 변환 함수
        private fun formatDate(dateString: String): String? {
            val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
            return try {
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) }
            } catch (e: Exception) {
                null // 파싱 실패 시 원본 날짜 반환 또는 null
            }
        }
    }
}

class NewsDiffCallback : DiffUtil.ItemCallback<NewsItem>() {
    override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
        return oldItem.link == newItem.link
    }

    override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
        return oldItem == newItem
    }
}