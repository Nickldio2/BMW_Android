package com.example.mapboxtest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentDestinations(
    private val destinations: List<DestinationEntry>
) : RecyclerView.Adapter<RecentDestinations.DestinationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent, parent, false)
        return DestinationViewHolder(view)
    }

    override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
        val destination = destinations[position]
        holder.bind(destination)
    }

    override fun getItemCount(): Int = destinations.size

    class DestinationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val addressTextView: TextView = itemView.findViewById(R.id.addressTextView)
        private val countTextView: TextView = itemView.findViewById(R.id.countTextView)

        fun bind(destination: DestinationEntry) {
            nameTextView.text = destination.name
            addressTextView.text = destination.address
            countTextView.text = destination.count.toString()
        }
    }
}
