package pl.agh.eye.exercise

import pl.agh.eye.R
import pl.agh.eye.exercise.EyePosition.*

class ExerciseService {
    private val circle = Exercise("Circle right", arrayListOf(UP, UP_LEFT, LEFT, DOWN_LEFT, DOWN, DOWN_RIGHT, RIGHT, UP_RIGHT), 10, R.drawable.circle)
    private val circleLeft = Exercise("Circle left", arrayListOf(UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT, LEFT, UP_LEFT), 10, R.drawable.circle_left)
    private val leftUpToRightDown = Exercise("Left up to right down", arrayListOf(UP_LEFT, DOWN_RIGHT), 10, R.drawable.left_up_to_right_down)
    private val middleToNose = Exercise("Nose staring", arrayListOf(CENTER, NOSE_STARING), 10, R.drawable.middle_to_nose)
    private val openClose = Exercise("Open and close", arrayListOf(CENTER, CLOSED), 10, R.drawable.open_close)
    private val openWide = Exercise("Wide opening", arrayListOf(CENTER, WIDE_OPEN), 10, R.drawable.open_wide)
    private val rightToLeft = Exercise("Right to left", arrayListOf(RIGHT, LEFT), 10, R.drawable.right_and_left)
    private val upToDown = Exercise("Up to down", arrayListOf(UP, DOWN), 10, R.drawable.up_and_down)
    private val upRightToDownLeft = Exercise("Right up to left down", arrayListOf(UP_RIGHT, DOWN_LEFT), 10, R.drawable.up_right_to_down_left)

    val exercises = arrayListOf(
        circle,
        circleLeft,
        leftUpToRightDown,
        middleToNose,
        openClose,
        openWide,
        rightToLeft,
        upToDown,
        upRightToDownLeft
    )
}