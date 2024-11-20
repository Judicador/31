package com.example.c9

import android.os.StrictMode
import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.lang.Exception
import java.sql.SQLException

class ConnectSql {
    private val ip = "192.168.1.39:1433"
    private val bd = "JobPosts"
    private val username = "Said"
    private val password = "2345678"

    fun dbConn(): Connection? {
        // Configuración de la política de StrictMode
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        var conn: Connection? = null
        val connString: String

        try {
            // Corrige el error tipográfico 'jbdc' -> 'jdbc'
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            connString = "jdbc:jtds:sqlserver://$ip;databaseName=$bd;user=$username;password=$password"
            conn = DriverManager.getConnection(connString)
        } catch (ex: SQLException) {
            Log.e("Error: ", ex.message ?: "SQL Error")
        } catch (ex1: ClassNotFoundException) {
            Log.e("Error: ", ex1.message ?: "Class Not Found")
        } catch (ex2: Exception) {
            Log.e("Error: ", ex2.message ?: "General Error")
        }

        return conn
    }
}
