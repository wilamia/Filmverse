package com.example.filmverse.Domain

data class MovieFavourite(
    val id: String,
    val title: String,
    val description: String,
    val ratingKp: String,
    val ratingImdb: String,
    val year: String,
    val posterUrl: String
)