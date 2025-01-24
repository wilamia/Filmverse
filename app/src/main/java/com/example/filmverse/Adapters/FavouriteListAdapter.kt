package com.example.filmverse.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.filmverse.Activities.DetailActivity
import com.example.filmverse.Activities.FavouriteActivity
import com.example.filmverse.Domain.MovieFavourite
import com.example.filmverse.databinding.ViewholderFavouriteBinding

class FavouriteListAdapter(private val context: FavouriteActivity, private val items: MutableList<MovieFavourite>) : RecyclerView.Adapter<FavouriteListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderFavouriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val film = items[position]
        holder.bind(film)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(private val binding: ViewholderFavouriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(film: MovieFavourite) {
            binding.movieTitle.text = film.title
            binding.movieDescription.text = film.description
            binding.movieRating.visibility = when {
                film.ratingImdb != "N/A" -> {
                    binding.movieRating.text = "IMDb: ${film.ratingImdb}"
                    View.VISIBLE
                }
                film.ratingKp != "N/A" -> {
                    binding.movieRating.text = "КП: ${film.ratingKp}"
                    View.VISIBLE
                }
                else -> {
                    View.GONE
                }
            }
            binding.movieYear.text = film.year

            Glide.with(binding.root.context)
                .load(film.posterUrl)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(30)))
                .into(binding.moviePoster)

            binding.root.setOnClickListener {
                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra("id", film.id)
                context.startActivity(intent)

            }
        }
    }
}
