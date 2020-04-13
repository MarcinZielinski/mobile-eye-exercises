package pl.agh.eye

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.exercise_tile.view.*
import pl.agh.eye.exercise.Exercise


class ExerciseAdapter(private val mContext: Context, private val exercises: List<Exercise>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return exercises.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): Any {
        return exercises[position]
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val exercise: Exercise = exercises[position]

        val newView = if (convertView == null) {
            val layoutInflater = LayoutInflater.from(mContext)
            layoutInflater.inflate(R.layout.exercise_tile, null) // dunno why it's red... but works
        } else
            convertView

        val imageView: ImageView = newView.exerciseTileImageView
        val titleTextView: TextView = newView.exerciseTileTitle

        imageView.setImageResource(exercise.imageResource)
        titleTextView.text = exercise.title

        return newView
    }
}