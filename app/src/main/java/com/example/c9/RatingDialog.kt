package com.example.c9

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.c9.rating.RatingAverageCalculator
import java.sql.Connection
import java.sql.SQLException

// Interfaz para la comunicación entre RatingDialog y MainActivity
interface RatingDialogListener {
    fun onRatingSubmitted()
}

class RatingDialog : DialogFragment() {

    private var postId: Int = -1
    private var rating: Float = 0f
    private var listener: RatingDialogListener? = null


    companion object {
        const val ARG_POST_ID = "postId" // Constante para la clave del argumento

        fun newInstance(postId: Int): RatingDialog {
            val fragment = RatingDialog()
            val args = Bundle().apply {
                putInt(ARG_POST_ID, postId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            postId = it.getInt(ARG_POST_ID)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_rating, null)

        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val buttonSubmit: Button = view.findViewById(R.id.buttonSubmit)

        buttonSubmit.setOnClickListener {
            rating = ratingBar.rating
            if (postId == -1) {
                Toast.makeText(context, "Error: PostId no definido", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }
            saveRatingToDatabase(rating)
            dismiss()
        }

        builder.setView(view)
            .setTitle("Calificación")

        return builder.create()
    }

    private fun saveRatingToDatabase(rating: Float) {
        val connection = ConnectSql().dbConn()
        try {
            if (connection != null && !connection.isClosed) {
                val query = "INSERT INTO PostRatings (PostID, Rating) VALUES (?, ?)"
                val preparedStatement = connection.prepareStatement(query)
                preparedStatement.setInt(1, postId)
                preparedStatement.setFloat(2, rating)
                preparedStatement.executeUpdate()
                Toast.makeText(context, "Calificación guardada", Toast.LENGTH_SHORT).show()
                updateAverageRating(postId, connection)
                listener?.onRatingSubmitted() //Notifica a MainActivity que se ha guardado
            } else {
                Toast.makeText(context, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SQLException) {
            Toast.makeText(context, "Error al guardar la calificación: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } catch (e: Exception) {
            Toast.makeText(context, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } finally {
            connection?.close()
        }
    }

    private fun updateAverageRating(postId: Int, connection: Connection) {
        try {
            RatingAverageCalculator.calculateAverageRating(postId, connection)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al actualizar calificación promedio: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
