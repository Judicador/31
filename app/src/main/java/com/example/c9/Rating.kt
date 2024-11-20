package com.example.c9

data class Rating(val jobTitle: String, val salary: String, val location: String, val author: String, var rating: Int) {

    companion object {
        private val ratings = mutableListOf<Rating>()

        fun addRating(rating: Rating) {
            ratings.add(rating)
        }

        fun getRatings(): List<Rating> = ratings.toList() // Devuelve una copia inmutable

        fun getRatingByJobTitle(jobTitle: String): Rating? = ratings.find { it.jobTitle == jobTitle }

        fun updateRating(jobTitle: String, newRating: Int) {
            ratings.find { it.jobTitle == jobTitle }?.let {
                it.rating = newRating
            }
        }
    }
}
