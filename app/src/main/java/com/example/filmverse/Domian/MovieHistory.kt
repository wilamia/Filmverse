package com.example.filmverse.Domain

import java.util.Date

data class MovieHistory(
    val id: String,
    val title: String,
    val ratingKp: String,
    val ratingImdb: String,
    val year: String,
    val posterUrl: String,
    val uniqueId: Date?
)