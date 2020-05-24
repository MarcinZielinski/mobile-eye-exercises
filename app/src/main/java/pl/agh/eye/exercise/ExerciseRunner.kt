package pl.agh.eye.exercise

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import com.google.common.collect.EvictingQueue
import java.util.*
import kotlin.concurrent.schedule


class ExerciseRunner(
    val exercise: Exercise,
    val middleTextView: TextView,
    val textToSpeech: TextToSpeech
) {
    private val HISTORY_BUFFER_SIZE: Int = 5
    private val CLASS_TAG = "EXERCISE RUNNER"

    private val UIHandler = Handler(Looper.getMainLooper())

    private var currentInstructionIndex: Int = 0
    private var currentInstruction: EyePosition = exercise.instructions.first()

    private var initialObservations: List<Observation> = emptyList()
    private var historyObservation: EvictingQueue<Pair<Observation, EyePosition>> =
        EvictingQueue.create(HISTORY_BUFFER_SIZE)
    private var currentPhase = RUNNER_PHASE.INIT
    private var eyesInTheMiddleObservation: Observation? = null

    private var faceSizeCoefficient: Double = 0.0
    private var eyeSizeCoefficient: Double = 0.0


    fun start() {
        val timer = Timer()
        middleTextView.text = "Stare at the screen for 5 seconds"

        timer.schedule(4000) { decreaseCountdown(timer, 5) }
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
        val factor = 0.3
        when {
            obs.eyeGaze.x > (1 + factor )* eyesInTheMiddleObservation?.eyeGaze?.x!! -> {
                when {
                    obs.eyeGaze.y > factor * eyesInTheMiddleObservation?.eyeGaze?.y!! -> {
                        historyObservation.add(Pair(obs, EyePosition.UP_RIGHT))
                        Log.i(CLASS_TAG, "UP Right detection")
                    }
                    obs.eyeGaze.y < (1 - factor) * eyesInTheMiddleObservation?.eyeGaze?.y!! -> {
                        historyObservation.add(Pair(obs, EyePosition.DOWN_RIGHT))
                        Log.i(CLASS_TAG, "Down Right detection")
                    }
                    else -> {
                        historyObservation.add(Pair(obs, EyePosition.RIGHT))
                        Log.i(CLASS_TAG, "Right detection")
                    }
                }
            }
            obs.eyeGaze.x < (1 - factor) * eyesInTheMiddleObservation?.eyeGaze?.x!! -> {
                when {
                    obs.eyeGaze.y > (1 + factor) * eyesInTheMiddleObservation?.eyeGaze?.y!! -> {
                        historyObservation.add(Pair(obs, EyePosition.UP_LEFT))
                        Log.i(CLASS_TAG, "Up Left detection")
                    }
                    obs.eyeGaze.y < (1 - factor) * eyesInTheMiddleObservation?.eyeGaze?.y!! -> {
                        historyObservation.add(Pair(obs, EyePosition.DOWN_LEFT))
                        Log.i(CLASS_TAG, "Down Left detection")
                    }
                    else -> {
                        historyObservation.add(Pair(obs, EyePosition.LEFT))
                        Log.i(CLASS_TAG, "Left detection")
                    }
                }
            }
            else -> {
                when {
                    obs.eyeGaze.y > (1 + factor) * eyesInTheMiddleObservation?.eyeGaze?.y!! -> {
                        historyObservation.add(Pair(obs, EyePosition.UP))
                        Log.i(CLASS_TAG, "Up detection")
                    }
                    obs.eyeGaze.y < (1 - factor) * eyesInTheMiddleObservation?.eyeGaze?.y!! -> {
                        historyObservation.add(Pair(obs, EyePosition.UP))
                        Log.i(CLASS_TAG, "Down detection")
                    }
                }
            }
        }

        if (historyObservation.size >= HISTORY_BUFFER_SIZE) {
            val firstFoundPosition: EyePosition = historyObservation.first().second
            for (pair in historyObservation) {
                if (pair.second != firstFoundPosition) {
                    return EyePosition.CLOSED;
                }
            }
            Log.i(CLASS_TAG, "Detected confirmed: $firstFoundPosition")
            return firstFoundPosition;
        }

        return EyePosition.CLOSED;
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
            currentInstructionIndex++
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
