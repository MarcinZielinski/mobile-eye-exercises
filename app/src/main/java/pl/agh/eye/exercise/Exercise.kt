package pl.agh.eye.exercise

import java.io.Serializable

data class Exercise(
    val title: String,
    val instructions: List<EyePosition>,
    val times: Int,
    val imageResource: Int
) : Serializable
