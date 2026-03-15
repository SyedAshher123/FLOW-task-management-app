package com.ahmedprojects.flow.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ahmedprojects.flow.R
import com.ahmedprojects.flow.models.Message

class MessageAdapter(
    private val list: MutableList<Message>,
    private val currentUserId: String,
    private val onLongClick: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val SENT_TEXT = 1
    private val RECEIVED_TEXT = 2
    private val SENT_IMAGE = 3
    private val RECEIVED_IMAGE = 4

    override fun getItemViewType(position: Int): Int {
        val m = list[position]

        return when {
            m.senderId == currentUserId && m.type == "image" -> SENT_IMAGE
            m.senderId == currentUserId -> SENT_TEXT
            m.type == "image" -> RECEIVED_IMAGE
            else -> RECEIVED_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        return when (type) {
            SENT_TEXT -> SentTextVH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_sent, parent, false))
            RECEIVED_TEXT -> ReceivedTextVH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_received, parent, false))
            SENT_IMAGE -> SentImageVH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_image_sent, parent, false))
            else -> ReceivedImageVH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_image_received, parent, false))
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val m = list[pos]

        holder.itemView.setOnLongClickListener {
            if (m.senderId == currentUserId) onLongClick(m)
            true
        }

        when (holder) {
            is SentTextVH -> holder.bind(m)
            is ReceivedTextVH -> holder.bind(m)
            is SentImageVH -> holder.bind(m)
            is ReceivedImageVH -> holder.bind(m)
        }
    }

    class SentTextVH(v: View) : RecyclerView.ViewHolder(v) {
        private val txt = v.findViewById<TextView>(R.id.sent_text)
        private val vanish = v.findViewById<TextView>(R.id.vanishBadge)
        fun bind(m: Message) {
            txt.text = m.messageText
            vanish.visibility = if (m.isVanish) View.VISIBLE else View.GONE
        }
    }

    class ReceivedTextVH(v: View) : RecyclerView.ViewHolder(v) {
        private val txt = v.findViewById<TextView>(R.id.received_text)
        private val vanish = v.findViewById<TextView>(R.id.vanishBadge)
        fun bind(m: Message) {
            txt.text = m.messageText
            vanish.visibility = if (m.isVanish) View.VISIBLE else View.GONE
        }
    }

    class SentImageVH(v: View) : RecyclerView.ViewHolder(v) {
        private val img = v.findViewById<ImageView>(R.id.image_sent)
        private val vanish = v.findViewById<TextView>(R.id.vanishBadge)
        fun bind(m: Message) {
            loadImage(img, m.imageUrl)
            vanish.visibility = if (m.isVanish) View.VISIBLE else View.GONE
        }
    }

    class ReceivedImageVH(v: View) : RecyclerView.ViewHolder(v) {
        private val img = v.findViewById<ImageView>(R.id.image_received)
        private val vanish = v.findViewById<TextView>(R.id.vanishBadge)
        fun bind(m: Message) {
            loadImage(img, m.imageUrl)
            vanish.visibility = if (m.isVanish) View.VISIBLE else View.GONE
        }
    }

    companion object {
        fun loadImage(view: ImageView, base64: String?) {
            if (!base64.isNullOrEmpty()) {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                view.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }
        }
    }
}
