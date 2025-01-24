package com.example.filmverse.Domian

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import javax.annotation.processing.Generated

@Generated("jsonschema2pojo")
class GenreResponse {
    @SerializedName("genres")
    @Expose
    var genres: List<GenreSolo> = ArrayList()
}