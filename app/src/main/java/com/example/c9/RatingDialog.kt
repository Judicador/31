package com.example.c9

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

import java.sql.SQLException
import android.widget.TextView

// Interfaz para la comunicación entre RatingDialog y MainActivity
interface RatingDialogListener {
    fun onRatingSubmitted()
}

class RatingDialog : DialogFragment() {

    private var postId: Int = -1

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

        // Obtén el postId de los argumentos del DialogFragment
        val postId = arguments?.getInt("postId") ?: -1

        buttonSubmit.setOnClickListener {
            val rating = ratingBar.rating

            if (postId == -1) {
                Toast.makeText(context, "Error: PostId no definido", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }

            // Encuentra el CardView correspondiente al postId
            val cardView = (activity as MainActivity).findCardViewByPostId(postId)
            // Encuentra el TextView dentro del CardView (si existe)
            val postTextView = cardView?.findViewById<TextView>(R.id.postTextView)

            // Verifica si postTextView es nulo antes de usarlo
            if (postTextView != null) {
                (activity as MainActivity).saveRatingToDatabase(rating, postId, postTextView)
            } else {
                // Manejar el caso nulo, por ejemplo, mostrar un mensaje de error
                Toast.makeText(context, "Error: No se pudo encontrar el TextView", Toast.LENGTH_SHORT).show()
            }

            dismiss()
        }

        return builder.setView(view)
            .setTitle("Calificar Publicación")
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            .create()
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


}
