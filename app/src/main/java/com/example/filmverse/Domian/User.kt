package com.example.filmverse.Domian
data class User(
    var username: String = "",
    var email: String = "",
    var movies: Map<String, Any> = emptyMap(),
    var history: Map<String, Any> = emptyMap()
)