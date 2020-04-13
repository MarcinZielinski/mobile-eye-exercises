package pl.agh.eye

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_first.*
import pl.agh.eye.exercise.ExerciseService


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exerciseService = ExerciseService()
        val exercises = exerciseService.exercises

        val listAdapter = ExerciseAdapter(context!!, exercises)
        exercisesGridView.adapter = listAdapter


        exercisesGridView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val exercise = exercises[position]
            val myIntent = Intent(activity, CameraActivity::class.java)
            myIntent.putExtra("exercise", exercise)
            startActivity(myIntent)
        }
    }

}
