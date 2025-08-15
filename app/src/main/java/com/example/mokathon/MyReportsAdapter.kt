package com.example.mokathon

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mokathon.databinding.ItemMyReportBinding
import java.text.SimpleDateFormat
import java.util.*

class MyReportsAdapter(private var reportList: List<Report>, private val onDeleteClick: (Report) -> Unit) : RecyclerView.Adapter<MyReportsAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemMyReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reportList[position])
    }

    override fun getItemCount(): Int {
        return reportList.size
    }

    fun updateData(newReportList: List<Report>) {
        reportList = newReportList
        notifyDataSetChanged()
    }

    inner class ReportViewHolder(private val binding: ItemMyReportBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.ivDelete.setOnClickListener {
                onDeleteClick(reportList[adapterPosition])
            }
        }

        fun bind(report: Report) {
            binding.tvPhoneNumber.text = formatPhoneNumber(report.phoneNumber)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.tvTimestamp.text = report.timestamp?.toDate()?.let { sdf.format(it) } ?: ""
        }

        private fun formatPhoneNumber(number: String): String {
            if (number.length == 11 && number.startsWith("010")) {
                return number.substring(0, 3) + "-" + number.substring(3, 7) + "-" + number.substring(7)
            }
            return number
        }
    }
}
