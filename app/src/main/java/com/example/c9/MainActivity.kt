
package com.example.c9

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


import java.sql.ResultSet


import java.sql.Connection
import java.sql.SQLException


data class User(val username: String, val password: String)




class MainActivity : AppCompatActivity() {

    private val users = mutableListOf<User>()
    private val posts = mutableListOf<Post>()
    private lateinit var currentUser: String
    private val cardViewsByPostId = HashMap<Int, CardView>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Referencias a vistas
        val loginForm: LinearLayout = findViewById(R.id.loginForm)
        val registerForm: LinearLayout = findViewById(R.id.registerForm)
        val createPostForm: LinearLayout = findViewById(R.id.createPostForm)
        val postsMenu: LinearLayout = findViewById(R.id.postsMenu)
        val menuButton: Button = findViewById(R.id.menuButton)

        val loginButton: Button = findViewById(R.id.loginButton)
        val registerButton: Button = findViewById(R.id.registerButton)
        val postButton: Button = findViewById(R.id.postButton)

        val toggleToRegister: TextView = findViewById(R.id.toggleToRegister)
        val toggleToLogin: TextView = findViewById(R.id.toggleToLogin)
        val backButtonCreatePost: Button = findViewById(R.id.backButtonCreatePost)
        val backButtonPosts: Button = findViewById(R.id.backButtonPosts)
        val searchInput: EditText = findViewById(R.id.searchInput)




        // Inicialización del TextWatcher para búsqueda
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchPosts(s.toString())
            }
        })

        // Inicialización: Mostrar solo el formulario de inicio de sesión al comenzar
        loginForm.visibility = View.VISIBLE
        registerForm.visibility = View.GONE
        createPostForm.visibility = View.GONE
        postsMenu.visibility = View.GONE

        // Manejo de eventos
        loginButton.setOnClickListener { login() }
        registerButton.setOnClickListener { register() }
        postButton.setOnClickListener { createPost() }


        toggleToRegister.setOnClickListener {
            loginForm.visibility = View.GONE
            registerForm.visibility = View.VISIBLE
        }

        toggleToLogin.setOnClickListener {
            registerForm.visibility = View.GONE
            loginForm.visibility = View.VISIBLE
        }

        backButtonCreatePost.setOnClickListener {
            createPostForm.visibility = View.GONE
            postsMenu.visibility = View.VISIBLE

        }

        backButtonPosts.setOnClickListener {
            postsMenu.visibility = View.GONE
            loginForm.visibility = View.VISIBLE
        }

        menuButton.setOnClickListener {
            loginForm.visibility = View.GONE
            registerForm.visibility = View.GONE
            createPostForm.visibility  = View.GONE
            postsMenu.visibility = View.VISIBLE
            displayPosts()
        }



    }



        // ... (resto del código de tu clase MainActivity) ...

        private fun getUserIdByUsername(connection: Connection, username: String): Int {
            var userId = -1 // Valor por defecto si no se encuentra el usuario

            try {
                if (!connection.isClosed) {
                    val query = "SELECT UserId FROM Users WHERE Username = ?"
                    val preparedStatement = connection.prepareStatement(query)
                    preparedStatement.setString(1, username)
                    val resultSet = preparedStatement.executeQuery()

                    if (resultSet.next()) {
                        userId = resultSet.getInt("UserId")
                    }

                    resultSet.close()
                    preparedStatement.close()
                } else {
                    // Manejar error de conexión (puedes usar tu función handleDbError)
                    println("Error al conectar con la base de datos")
                }
            } catch (e: SQLException) {
                // Manejar error de SQL (puedes usar tu función handleDbError)
                println("Error al obtener el ID del usuario: ${e.message}")
            } catch (e: Exception) {
                // Manejar error inesperado (puedes usar tu función handleDbError)
                println("Error inesperado: ${e.message}")
            }

            return userId
        }


    fun saveRatingToDatabase(rating: Float, postId: Int, postTextView: TextView) {
        // 1. Update the database with the new rating

        ConnectSql().dbConn()?.let { connection -> // Usa let para ejecutar el código solo si la conexión no es nula
            val userId = UserUtils.getUserIdByUsername(connection, currentUser)

            if (userId != -1) {
                // Inserta la calificación en PostRatings
                try {
                    if (connection != null && !connection.isClosed) {
                        val query = "INSERT INTO PostRatings (postId, UserID, rating) VALUES (?, ?, ?)"
                        val preparedStatement = connection.prepareStatement(query)
                        preparedStatement.setInt(1, postId)
                        preparedStatement.setInt(2, userId) // Usa el ID del usuario aquí
                        preparedStatement.setFloat(3, rating)
                        preparedStatement.executeUpdate()

                        // Update the average rating in the UI
                        updateAverageRating(postId, connection)
                    } else {
                        handleDbError("Error al conectar con la base de datos")
                    }
                } catch (e: SQLException) {
                    handleDbError("Error al guardar la calificación: ${e.message}", e)
                } catch (e: Exception) {
                    handleDbError("Error inesperado: ${e.message}", e)
                } finally {
                    // La conexión se cierra aquí, después de updateAverageRating()
                    connection.close()
                }
            } else {
                // Manejar error si no se encuentra el usuario
                handleDbError("No se pudo encontrar el usuario para guardar la calificación")
                // La conexión se cierra aquí también, si no se encuentra el usuario
                connection.close()
            }
        } ?: run {
            handleDbError("Error al conectar con la base de datos") // Ejecuta esto si la conexión es nula
        }
    }

    private fun updateAverageRating(postId: Int, connection: Connection) {
        try {
            val query = """
            SELECT AVG(rating) AS average_rating
            FROM PostRatings
            WHERE postId = ?
        """
            val preparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, postId)
            val resultSet = preparedStatement.executeQuery()

            if (resultSet.next()) {
                val averageRating = resultSet.getDouble("average_rating")
                // Update the UI with the new average rating
                val cardView = findCardViewByPostId(postId)
                val postTextView = cardView?.findViewById<TextView>(R.id.postTextView)
                postTextView?.text = postTextView?.text.toString().replace(
                    Regex("""\(Calificación: [0-9.]+\)"""),
                    "(Calificación: ${averageRating.format(1)})"
                )
            }

            resultSet.close()
            preparedStatement.close()
        } catch (e: Exception) {
            handleDbError("Error al actualizar calificación promedio: ${e.message}", e)
        }
    }

    internal fun findCardViewByPostId(postId: Int): CardView? {
        return cardViewsByPostId[postId]
    }

    private fun login() {
        val username = findViewById<EditText>(R.id.username).text.toString()
        val password = findViewById<EditText>(R.id.password).text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // Conectar a la base de datos
        val connection = ConnectSql().dbConn()

        var userExists = false

        try {
            // Verifica si la conexión es válida y está abierta
            if (connection != null && !connection.isClosed) {
                // Preparar la consulta para verificar el usuario
                val query = "SELECT * FROM Users WHERE Username = ? AND Password = ?"
                val preparedStatement = connection.prepareStatement(query)
                preparedStatement.setString(1, username)
                preparedStatement.setString(2, password)

                val resultSet = preparedStatement.executeQuery()
                userExists = resultSet.next() // Si existe al menos un registro, el usuario es válido

                resultSet.close() // Cerrar el ResultSet
                preparedStatement.close() // Cerrar el PreparedStatement
            } else {
                Log.e("DB Connection", "La conexión es nula o está cerrada.")
                Toast.makeText(this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al intentar iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        } finally {
            connection?.close() // Asegúrate de cerrar la conexión
        }

        if (userExists) {
            currentUser = username
            findViewById<LinearLayout>(R.id.loginForm).visibility = View.GONE
            findViewById<LinearLayout>(R.id.createPostForm).visibility = View.VISIBLE

        } else {
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
        }


    }

    private fun register() {
        val newUsername = findViewById<EditText>(R.id.newUsername).text.toString()
        val newPassword = findViewById<EditText>(R.id.newPassword).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.confirmPassword).text.toString()

        if (newUsername.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (users.any { it.username == newUsername }) {
            Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_SHORT).show()
            return
        }

        // Conectar a la base de datos
        val connection = ConnectSql().dbConn()

        // Insertar en la base de datos
        try {
// Verifica si la conexión es
            // Verifica si la conexión es válida y está abierta
            if (connection != null && !connection.isClosed) {
                // Preparar la consulta para insertar el nuevo usuario
                val query = "INSERT INTO Users (Username, Password) VALUES (?, ?)"
                val preparedStatement = connection.prepareStatement(query)
                preparedStatement.setString(1, newUsername)
                preparedStatement.setString(2, newPassword)
                preparedStatement.executeUpdate() // Ejecutar la consulta
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                clearRegisterForm()
            } else {
                Log.e("DB Connection", "La conexión es nula o está cerrada.")
                Toast.makeText(this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al registrar el usuario: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            // Asegúrate de cerrar la conexión
            connection?.close()
        }

        // Volver al formulario de inicio de sesión después del registro
        findViewById<LinearLayout>(R.id.registerForm).visibility = View.GONE
        findViewById<LinearLayout>(R.id.loginForm).visibility = View.VISIBLE
    }

    private fun clearRegisterForm() {
        findViewById<EditText>(R.id.newUsername).text.clear()
        findViewById<EditText>(R.id.newPassword).text.clear()
        findViewById<EditText>(R.id.confirmPassword).text.clear()
    }

    private fun createPost() {
        val jobTitle = findViewById<EditText>(R.id.jobTitle).text.toString()
        val salary = findViewById<EditText>(R.id.salary).text.toString()
        val location = findViewById<EditText>(R.id.location).text.toString()

        if (jobTitle.isEmpty() || salary.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val connection = ConnectSql().dbConn()

        try {
            if (connection != null && !connection.isClosed) {
                val userId = UserUtils.getUserIdByUsername(connection, currentUser)

                val query = "INSERT INTO Posts (JobTitle, Salary, Location, UserId) VALUES (?, ?, ?, ?)"
                val preparedStatement = connection.prepareStatement(query)
                preparedStatement.setString(1, jobTitle)
                preparedStatement.setString(2, salary)
                preparedStatement.setString(3, location)
                preparedStatement.setInt(4, userId)

                preparedStatement.executeUpdate()
                Toast.makeText(this, "Publicación creada", Toast.LENGTH_SHORT).show()
                clearPostForm()

                posts.add(Post(jobTitle, salary, location, currentUser))
                displayPosts()


            } else {
                Log.e("DB Connection", "La conexión es nula o está cerrada.")
                Toast.makeText(this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al crear la publicación: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            // El bloque finally ya no es necesario para cerrar la conexión
        }
        connection?.close()
        findViewById<LinearLayout>(R.id.createPostForm).visibility = View.GONE
        findViewById<LinearLayout>(R.id.postsMenu).visibility = View.VISIBLE

    }

    private fun clearPostForm() {
        findViewById<EditText>(R.id.jobTitle).text.clear()
        findViewById<EditText>(R.id.salary).text.clear()
        findViewById<EditText>(R.id.location).text.clear()
    }

    private fun handleDbError(message: String, exception: Exception? = null) {
        Log.e("Database Error", message, exception)
        Toast.makeText(this, "Error de base de datos: $message", Toast.LENGTH_LONG).show()
        // Agrega más acciones de manejo de errores según sea necesario
    }




    @SuppressLint("SetTextI18n")
    private fun searchPosts(query: String) {
        val postsList = findViewById<LinearLayout>(R.id.postsList)
        postsList.removeAllViews() // Limpia la lista antes de mostrar nuevos resultados

        val connection = ConnectSql().dbConn()
        try {
            if (connection != null && !connection.isClosed) {
                val querySQL = """
                SELECT 
                    p.postId,
                    p.JobTitle,
                    p.Salary,
                    p.Location,
                    u.Username AS Author  -- Obtiene el nombre de usuario de la tabla Users
                FROM 
                    Posts p
                INNER JOIN  -- Usa INNER JOIN para obtener el nombre de usuario
                    Users u ON p.UserId = u.UserId
                WHERE 
                    LOWER(p.JobTitle) LIKE ? OR LOWER(p.Location) LIKE ?
            """
                val preparedStatement = connection.prepareStatement(querySQL)
                preparedStatement.setString(1, "%${query.lowercase()}%")
                preparedStatement.setString(2, "%${query.lowercase()}%")
                val resultSet = preparedStatement.executeQuery()

                if (!resultSet.next()) { // verifica si hay resultados
                    val noPostsMessage = TextView(this)
                    noPostsMessage.text = "No se encontraron publicaciones que coincidan con la búsqueda."
                    postsList.addView(noPostsMessage)
                } else {
                    do {
                        // Extrae los datos del ResultSet, igual que en displayPosts
                        val postId = resultSet.getInt("postId") // Obtiene el postId del ResultSet
                        val jobTitle = resultSet.getString("JobTitle")
                        val salary = resultSet.getString("Salary")
                        val location = resultSet.getString("Location")
                        val author = resultSet.getString("Author") // Obtiene el nombre de usuario del autor

                        // Create CardView and add TextView to it
                        val cardView = CardView(this)
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams.setMargins(16, 16, 16, 16)
                        cardView.layoutParams = layoutParams
                        cardView.setCardElevation(8f)
                        cardView.setCardBackgroundColor(resources.getColor(android.R.color.white))

                        val postView = TextView(this)
                        postView.text = "Trabajo: $jobTitle\n" +
                                "Sueldo: $salary\n" +
                                "Ubicación: $location\n" +
                                "Autor: $author"
                        postView.setPadding(16, 16, 16, 16)

                        // Agrega el postId al RatingDialog
                        postView.setOnClickListener {
                            val ratingDialog = RatingDialog()
                            val bundle = Bundle()
                            bundle.putInt("postId", postId)
                            ratingDialog.arguments = bundle
                            ratingDialog.show(supportFragmentManager, "RatingDialog")
                        }

                        cardView.addView(postView)
                        postsList.addView(cardView) // Add CardView to postsList
                    } while (resultSet.next())
                }

                resultSet.close()
                preparedStatement.close()
            } else {
                // Manejar el caso donde la conexión falla.
                val noPostsMessage = TextView(this)
                noPostsMessage.text = "Error al conectar con la base de datos."
                postsList.addView(noPostsMessage)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            val errorMessage = TextView(this)
            errorMessage.text = "Error al realizar la búsqueda: ${e.message}"
            postsList.addView(errorMessage)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = TextView(this)
            errorMessage.text = "Error inesperado: ${e.message}"
            postsList.addView(errorMessage)
        } finally {

        }
    }

    private fun displayPosts() {
        val postsList = findViewById<LinearLayout>(R.id.postsList)
        postsList.removeAllViews()
        val connection = ConnectSql().dbConn()

        try {
            if (connection != null && !connection.isClosed) {
                val query = """
                SELECT 
                    p.postId,  -- Agrega esta línea para obtener el postId
                    p.JobTitle,
                    p.Salary,
                    p.Location,
                    u.Username AS Author,
                    COALESCE(AVG(r.rating), 0) AS average_rating,
                    p.CreatedAt
                FROM 
                    Posts p
                LEFT JOIN 
                    PostRatings r ON p.postId = r.postId
               INNER JOIN  -- Usa INNER JOIN para obtener el nombre de usuario
                    Users u ON p.UserId = u.UserId
                GROUP BY 
                    p.postId,  -- Agrega esta línea para agrupar por postId
                    p.JobTitle, p.Salary, p.Location, u.Username, p.CreatedAt
                ORDER BY p.CreatedAt DESC;
            """
                val preparedStatement = connection.prepareStatement(query)
                val resultSet = preparedStatement.executeQuery()

                if (!resultSet.next()) {
                    val noPostsMessage = TextView(this)
                    noPostsMessage.text = "No se encontraron publicaciones."
                    postsList.addView(noPostsMessage)
                } else {
                    do {
                        val postId = resultSet.getInt("postId")  // Obtén el postId del ResultSet
                        val jobTitle = resultSet.getString("JobTitle")
                        val salary = resultSet.getString("Salary")
                        val location = resultSet.getString("Location")
                        val author = resultSet.getString("Author") // Obtiene el nombre de usuario del autor
                        val averageRating = resultSet.getDouble("average_rating")
                        val userId = getUserIdByUsername(connection, author)

                        val cardView = CardView(this)
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams.setMargins(16, 16, 16, 16)
                        cardView.layoutParams = layoutParams
                        cardView.setCardElevation(8f)
                        cardView.setCardBackgroundColor(resources.getColor(android.R.color.white))

                        val postView = TextView(this)
                        postView.id = R.id.postTextView // Asigna un ID único al TextView
                        postView.text = """
                        Título: $jobTitle
                        Salario: $salary
                        Ubicación: $location
                        Autor: $author (Calificación: ${averageRating.format(1)})
                    """.trimIndent()
                        postView.setPadding(16, 16, 16, 16)

                        // Listener combinado
                        postView.setOnClickListener {
                            val ratingDialog = RatingDialog()
                            // Pasa el postId al RatingDialog
                            val bundle = Bundle()
                            bundle.putInt("postId", postId)
                            ratingDialog.arguments = bundle
                            ratingDialog.show(supportFragmentManager, "RatingDialog")
                        }

                        cardView.addView(postView)
                        postsList.addView(cardView)

                        // Agrega la entrada al HashMap cardViewsByPostId
                        cardViewsByPostId[postId] = cardView
                    } while (resultSet.next())
                    postsList.invalidate()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection?.close()
        }
    }

    fun Double.format(digits: Int) = String.format("%.${digits}f", this)





}

