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
import java.sql.SQLException

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

    fun initClone(): Unit{

    }

    fun createUsers(): Unit{

    }

    fun deleteUsers(): Unit{

    }

    abstract fun getUserData(): Unit

    fun executeQuery(target: Connection, query: String?): Unit {
        try {
            target.prepareStatement(query).use { ps ->
                val executed: Int = ps.executeUpdate()
                log.trace("Query executed: {} rows affected", executed)
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
                target.prepareStatement(insertQuery).use { ps ->
                    for ((index, value) in row.columnValues.withIndex()) {
                        when (value) {
                            is SpecialDBObject -> {
                                when (val data: Any = value.data) {
                                    is ByteArray -> {
                                        val b: Blob = target.createBlob()
                                        b.setBytes(1, data)
                                        ps.setObject(index + 1, b)
                                    }
                                    is String -> {
                                        val c: Clob = target.createClob()
                                        c.setString(1, data)
                                        ps.setObject(index + 1, c)
                                    }
                                    else -> ps.setObject(index + 1, data)
                                }
                            }
                            else -> ps.setObject(index + 1, value)
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