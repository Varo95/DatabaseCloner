package com.alvaro.devutils.tools.inheritance

import com.alvaro.devutils.model.CloneObjectUtil
import com.alvaro.devutils.model.Row
import com.alvaro.devutils.model.SpecialDBObject
import javafx.concurrent.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Blob
import java.sql.Clob
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement

/**
 * This class will be usable for cloning databases, making general code here and specific code in the subclasses
 */
abstract class DatabaseCloner(private val cloneObjectUtil: CloneObjectUtil) : Task<String>(){

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private var maxProgressBar: Long = 0
    private var actualProgressBar: Long = 0

    override fun call(): String{
        return ""
    }

    fun recreateUsers(): Unit{

    }

    abstract fun getUserData(): Unit

    fun executeQuery(target: Connection, query: String?): Unit {
        try {
            target.createStatement().use { statement: Statement ->
                val executed: Boolean = statement.execute(query)
                log.trace("Query executed: {}", executed)
                log.trace("Query executed: {}", query)
                this.updateProgress(++this.actualProgressBar, this.maxProgressBar)
            }
        } catch (e: SQLException) {
            log.error("Query mal formada: $query")
            log.error(e.message, e)
        }
    }

    fun insertData(target: Connection, rows: List<Row>, insertQuery: String) {
        rows.forEach { row ->
            try {
                target.prepareStatement(insertQuery).use { ps: PreparedStatement ->
                    for((index: Int, value: Any?) in row.columnValues.withIndex()){
                        if(value is SpecialDBObject){
                            if(value.data is ByteArray){
                                val b: Blob = target.createBlob()
                                b.setBytes(1, value.data)
                                ps.setObject(index + 1, b)
                            }else if(value.data is String){
                                val c: Clob = target.createClob()
                                c.setString(1, value.data)
                                ps.setObject(index + 1, c)
                            }
                        }else{
                            ps.setObject(index + 1, value)
                        }
                    }
                    val executed: Int = ps.executeUpdate()
                    log.trace("Query executed: {} rows affected", executed)
                    this.updateProgress(++this.actualProgressBar, this.maxProgressBar)
                }
            } catch (e: SQLException) {
                log.error(e.message, e)
            }
        }
    }
}