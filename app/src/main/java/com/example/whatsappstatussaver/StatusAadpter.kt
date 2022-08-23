package com.example.whatsappstatussaver

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StatusAadpter(private val context: Context,
                    private val modelClass: ArrayList<ModelClass>,
                    private val clickListner: (ModelClass)->Unit): RecyclerView.Adapter<StatusAadpter.StatusViewHolder>(){

    class StatusViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val iv_status: ImageView = itemView.findViewById(R.id.iv_status)
        val iv_video_icon: ImageView = itemView.findViewById(R.id.iv_video_icon)
        val cv_video_card: CardView = itemView.findViewById(R.id.cv_video_card)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        return StatusViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.status_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        if(modelClass[position].fileName.endsWith(".mp4")) {
            holder.cv_video_card.isVisible = true
            holder.iv_video_icon.isVisible = true
        }else {
            holder.cv_video_card.isVisible = false
            holder.iv_video_icon.isVisible = false
        }
        Glide.with(context).load(Uri.parse(modelClass[position].fileUri)).into(holder.iv_status)
        holder.iv_status.setOnClickListener {
            clickListner(modelClass[position])
        }
    }

    override fun getItemCount(): Int {
        return modelClass.size
    }

}