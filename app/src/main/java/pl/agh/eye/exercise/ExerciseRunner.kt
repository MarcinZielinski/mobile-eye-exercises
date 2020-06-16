package pl.agh.eye.exercise

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import java.util.*
import kotlin.concurrent.schedule

class ExerciseRunner(
    val exercise: Exercise,
    val middleTextView: TextView,
    val textToSpeech: TextToSpeech
) {
    private val CLASS_TAG = "EXERCISE RUNNER"

    private val UIHandler = Handler(Looper.getMainLooper())

    private var currentInstructionIndex: Int = 0
    private var currentInstruction: EyePosition = exercise.instructions.first()

    private var initialObservations: List<Observation> = emptyList()
    private var currentPhase = RUNNER_PHASE.INIT
    private var eyesInTheMiddleObservation: Observation? = null

    private var faceSizeCoefficient: Double = 0.0
    private var eyeSizeCoefficient: Double = 0.0


    fun start() {
        val timer = Timer()
        middleTextView.text = "Stare at the screen for 5 seconds"

        timer.schedule(4000) { decreaseCountdown(timer, 60) }
    }


    private fun decreaseCountdown(timer: Timer, countdown: Int) {
        UIHandler.post { middleTextView.text = countdown.toString() }

        if (countdown > 0) {
            timer.schedule(1000) { decreaseCountdown(timer, countdown - 1) }
        } else {
            UIHandler.post { middleTextView.text = "" }
            val obsTable = initialObservations

            // TODO: handle case when obsTable is empty reduce and is throwing exception
            eyesInTheMiddleObservation = obsTable
                .reduce { acc, observation -> acc.add(observation) }
                .divideByScalar(obsTable.size)

            Log.i(
                CLASS_TAG, "face: ${eyesInTheMiddleObservation!!.face}\n" +
                        "eye: ${eyesInTheMiddleObservation!!.eye}\n" +
                        "eyeGaze: ${eyesInTheMiddleObservation!!.eyeGaze}\n" +
                        "eyeMat.height: ${eyesInTheMiddleObservation!!.eyeMatHeight}"
            )

            faceSizeCoefficient =
                eyesInTheMiddleObservation!!.face.width.toDouble() / eyesInTheMiddleObservation!!.face.height
            eyeSizeCoefficient =
                eyesInTheMiddleObservation!!.eye.width.toDouble() / eyesInTheMiddleObservation!!.eye.height

            currentPhase = RUNNER_PHASE.EXERCISE

            nextExercise()
        }
    }

    fun registerObservation(obs: Observation) {
        if (currentPhase == RUNNER_PHASE.INIT) {
            initialObservations = initialObservations + obs
        } else if (currentPhase == RUNNER_PHASE.EXERCISE) {
            val eyePosition = deduceEyePosition(obs)
            Log.i(CLASS_TAG, "Deduced position: ${eyePosition.name}")
            if (eyePosition == currentInstruction) {
                Log.i(CLASS_TAG, "Next exercise")
                nextExercise()
            }
        }
    }

    private fun deduceEyePosition(obs: Observation): EyePosition {
        // TODO: implement
        return EyePosition.CLOSED
    }

    private fun nextExercise() {
        if (currentInstructionIndex < exercise.instructions.size) {
            currentInstruction = exercise.instructions[currentInstructionIndex]
            textToSpeech.speak(
                currentInstruction.name.replace('_', ' '),
                TextToSpeech.QUEUE_ADD,
                null,
                currentInstruction.name
            )
            UIHandler.post { middleTextView.text = currentInstruction.name }
        } else {
            UIHandler.post {
                middleTextView.text = "Congratulations! You've finished exercise. You can go back."
            }
            Log.i(CLASS_TAG, "exercise finished")
        }
    }
}

enum class RUNNER_PHASE {
    INIT, EXERCISE, RECALIBRATION
}
