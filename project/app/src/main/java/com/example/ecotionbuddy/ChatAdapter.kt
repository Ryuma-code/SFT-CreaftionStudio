package com.example.ecotionbuddy

// File: ChatAdapter.kt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecotionbuddy.databinding.ItemChatBotBinding // Ganti dengan package project Anda
import com.example.ecotionbuddy.databinding.ItemChatUserBinding // Ganti dengan package project Anda

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    // Konstanta untuk tipe view, agar kode lebih mudah dibaca
    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_BOT = 2

    // ViewHolder untuk pesan user
    inner class UserMessageViewHolder(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chatMessage: ChatMessage) {
            binding.textMessage.text = chatMessage.message
        }
    }

    // ViewHolder untuk pesan bot
    inner class BotMessageViewHolder(private val binding: ItemChatBotBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chatMessage: ChatMessage) {
            binding.textMessage.text = chatMessage.message
        }
    }

    // Menentukan tipe view (user atau bot) berdasarkan posisi data
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).sender == Sender.USER) {
            VIEW_TYPE_USER
        } else {
            VIEW_TYPE_BOT
        }
    }

    // Membuat ViewHolder yang sesuai berdasarkan tipe view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val binding = ItemChatUserBinding.inflate(inflater, parent, false)
            UserMessageViewHolder(binding)
        } else {
            val binding = ItemChatBotBinding.inflate(inflater, parent, false)
            BotMessageViewHolder(binding)
        }
    }

    // Menghubungkan data ke ViewHolder yang tepat
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder.itemViewType == VIEW_TYPE_USER) {
            (holder as UserMessageViewHolder).bind(message)
        } else {
            (holder as BotMessageViewHolder).bind(message)
        }
    }
}

// Kelas untuk menghitung perbedaan antar list, membuat RecyclerView lebih efisien
class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}