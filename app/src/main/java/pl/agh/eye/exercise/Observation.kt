package pl.agh.eye.exercise

import org.opencv.core.Point
import org.opencv.core.Rect

data class Observation(val face: Rect, val eye: Rect, val eyeGaze: Point, val eyeMatHeight: Int) {
    fun add(obs: Observation): Observation {
        return Observation(
            Rect(
                face.x + obs.face.x,
                face.y + obs.face.y,
                face.width + obs.face.width,
                face.height + obs.face.height
            ),
            Rect(
                eye.x + obs.eye.x,
                eye.y + obs.eye.y,
                eye.width + obs.eye.width,
                eye.height + obs.eye.height
            ),
            Point(eyeGaze.x + obs.eyeGaze.x, eyeGaze.y + obs.eyeGaze.y),
            eyeMatHeight + obs.eyeMatHeight
        )
    }

    fun divideByScalar(divider: Int): Observation {
        return Observation(
            Rect(face.x / divider, face.y / divider, face.width / divider, face.height / divider),
            Rect(eye.x / divider, eye.y / divider, eye.width / divider, eye.height / divider),
            Point(eyeGaze.x / divider, eyeGaze.y / divider),
            eyeMatHeight / divider
        )
    }
}