package com.alvaro.devutils.model

import java.util.Objects
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

/**
 * Objeto que representa a una tabla en BBDD. Se incluyen las sentencias de 'Create Table', 'Alter Table' y 'Insert Into'.
 * Además se incluye la sentencia de 'Select * from' para obtener los datos, que posteriormente se insertan en el objeto 'Rows'.
 * @param tableName Nombre de la tabla. Deberá de ser en el formato 'USUARIO.TABLA' para oracle
 */
class Table(val tableName: String) {
    val alterTableFK: MutableList<String> = CopyOnWriteArrayList()
    val rows: MutableList<Row> = CopyOnWriteArrayList()
    private val createTable: StringBuilder = StringBuilder("CREATE TABLE ").append(this.tableName).append(" (")
    private val alterTablePK: StringBuilder = StringBuilder()
    private val insertInto: StringBuilder = StringBuilder("INSERT INTO ").append(this.tableName).append(" (")
    private val selectQuery = StringBuilder("SELECT * FROM ").append(this.tableName)
    private var columnCount = 0

    /**
     * Estas funciones son necesarias para que devuelva el toString en lugar del StringBuilder
     */
    fun getCreateTable(): String {
        return this.createTable.toString()
    }

    fun getAlterTablePK(): String? {
        return if(this.alterTablePK.toString() == "") null else this.alterTablePK.toString()
    }

    fun getInsertInto(): String {
        return this.insertInto.toString()
    }

    fun getSelectQuery(): String {
        return this.selectQuery.toString()
    }

    /**
     * Esta funcion se utiliza para añadir las claves primarias de la tabla.
     */
    fun createPK(pkUtil: PKUtil?) {
        if (pkUtil != null) {
            this.alterTablePK.append("ALTER TABLE ").append(this.tableName).append(" ADD CONSTRAINT ").append(pkUtil.pk).append(" PRIMARY KEY (")
            pkUtil.pks.forEach { pk -> this.alterTablePK.append(pk).append(", ") }
            this.alterTablePK.deleteCharAt(this.alterTablePK.length - 2).append(")")
        }
    }

    /**
     * Esta funcion se utiliza para añadir los campos y sus tipos al create table.
     * Además en la select query se añade para que se ordene por el primer campo. Esto es importante debido a que puede haber referencias en la misma tabla
     * @param columnName Nombre de la columna
     * @param type Tipo de la columna
     * @param size Tamaño de la columna
     * @param nullable Indica si la columna puede ser nula o no
     */
    fun populateDDLColumnsCreateTable(columnName: String?, type: String, size: String, nullable: String) {
        val isSpecialType: Boolean = this.isSpecialType(type)
        this.createTable.append(columnName).append(" ").append(type)
            .append(if (size == "0" || isSpecialType) "" else "($size)")
            .append(if (nullable == "NO") " NOT NULL" else "").append(", ")
        if(columnCount == 0){
            this.selectQuery.append(" ORDER BY ").append(columnName).append(" ASC")
        }
        this.columnCount++
    }

    fun populateDDLColumnsInsertInto(columnName: String?) {
        this.insertInto.append(columnName).append(", ")
    }

    /**
     * Esta función nos permite diferenciar si es un objeto especial en BBDD para hacer su tratamiento
     */
    private fun isSpecialType(type: String): Boolean {
        return type == "TIMESTAMP(6)" || type == "DATE" || type == "BLOB" || type == "CLOB"
    }

    /**
     * Esta funcion elimina el último caracter de las sentencias 'Create Table' y 'Insert Into'.
     * Además añade los paréntesis finales a las sentencias.
     */
    fun deleteLastCharFromCreateAndInsert() {
        this.createTable.deleteCharAt(this.createTable.length - 2).append(")")
        this.insertInto.deleteCharAt(this.insertInto.length - 2).append(")").append("VALUES(")
            .append("?,".repeat(max(0.0, this.columnCount.toDouble()).toInt()))
            .deleteCharAt(this.insertInto.length - 1).append(")")
    }

    /**
     * El objeto es igual si los nombres coinciden
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Table) return false
        return this.tableName == other.tableName
    }

    /**
     * Se ordenará por el nombre de la tabla de manera alfabética
     */
    override fun hashCode(): Int {
        return Objects.hashCode(this.tableName)
    }
}

/**
 * Objeto que se utiliza para almacenar las PKs de una tabla.
 */
class PKUtil {
    val pks: MutableSet<String> = HashSet()
    var pk: String? = null

    fun addPk(pk: String) {
        this.pks.add(pk)
    }

}

/**
 * Objeto comentario que se puede añadir a una tabla o a una columna.
 */
class Comment(tableName: String, columnName: String?, comment: String) {
    val insertComment = if (columnName == null) "COMMENT ON TABLE $tableName IS '$comment'" else "COMMENT ON COLUMN $tableName.$columnName IS '$comment'"
}

/**
 * Objeto que representa una fila de una tabla en BBDD.
 */
@JvmRecord
data class Row(val columnValues: List<Any?>)

/**
 * Objeto que representa los datos necesarios para hacer la clonación de una BBDD a otra.
 * @param recreateTarget Indica si se debe rehacer la base de datos destino. Borrando todos los datos de los usuarios
 * @param origin Conexión a la BBDD origen
 * @param target Conexión a la BBDD destino
 */
@JvmRecord
data class CloneObjectUtil(val recreateTarget: Boolean, val origin: DatabaseConnection, val target: DatabaseConnection)

/**
 * Objeto que representa las secuencias de una tabla.
 * Se creó para agrupar las secuencias y las tablas.
 * Primero se crean las secuencias y luego las tablas.
 * @param sequences Lista de secuencias. Cada una deberá de ser en el formato 'USUARIO.SEQ' para oracle. EJ: 'CREATE SEQUENCE USUARIO.SEQ'
 * @param tables Lista de tablas. Cada una deberá de ser en el formato 'USUARIO.TABLA' para oracle. EJ: 'CREATE TABLE USUARIO.TABLA'
 */
@JvmRecord
data class UserSequencesTable(val sequences: List<String>, val tables: List<Table>)

/**
 * Objeto que agrupa las vistas y los comentarios.
 * Se creó porque las vistas y los comentarios necesitan tener las tablas creadas previamente.
 * Utilizamos este objeto al final del proceso de clonado para traernos los últimos datos
 * @param views Lista de vistas. Cada una deberá de ser en el formato 'USUARIO.VISTA' para oracle. EJ: 'CREATE OR REPLACE VIEW USUARIO.VISTA AS SELECT * FROM USUARIO.TABLA'
 * @param comments Lista de comentarios. Cada uno deberá de ser en el formato 'COMMENT ON TABLE USUARIO.TABLA IS 'COMENTARIO'' o 'COMMENT ON COLUMN USUARIO.TABLA.COLUMNA IS 'COMENTARIO''
 */
@JvmRecord
data class UserViewsComments(val views: List<String>, val comments: List<Comment>)

/**
 * Objeto que representa un objeto especial en BBDD como un Blob ó Clob.
 * Debido a que no se pueden pasar directamente a la BBDD, se necesita un objeto especial para poder tratarlos.
 * En el codigo, detectamos que es un objeto especial y creamos el respectivo en la BBDD de destino, copiando posteriormente los datos que este contiene.
 */
@JvmRecord
data class SpecialDBObject(val data: Any)