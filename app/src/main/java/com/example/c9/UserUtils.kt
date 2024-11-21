package com.example.c9 // Reemplaza con tu paquete

import java.sql.Connection
import java.sql.SQLException

object UserUtils {

    fun getUserIdByUsername(connection: Connection, username: String): Int {
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
        } finally {
            //  Se ha eliminado connection.close() de aquí
        }

        return userId
    }
}