package com.example.filmverse.Adapters

import Movie
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.filmverse.Activities.DetailActivity
import com.example.filmverse.Domian.SliderItems
import com.example.filmverse.R

class SliderAdapters(
    private var sliderItems: List<SliderItems>,
    private var films: List<Movie>,
    private var context: Context
) : RecyclerView.Adapter<SliderAdapters.SliderViewHolder>() {

    inner class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(sliderItem: SliderItems, film: Movie) {
            val requestOptions = RequestOptions()
                .transform(CenterCrop(), RoundedCorners(30))

            Glide.with(context)
                .load(sliderItem.imageUrl)
                .apply(requestOptions)
                .into(imageView)

            itemView.setOnClickListener {
                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra("id", film.id)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.slide_item_container, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        Log.d("SliderAdapter", "Binding position: $position")
        val currentItem = sliderItems[position % sliderItems.size]
        val film = films[position % films.size]
        holder.bind(currentItem, film)
    }
    override fun getItemCount(): Int {
        return Int.MAX_VALUE
    }
}