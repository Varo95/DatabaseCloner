package com.alvaro.devutils.tools

import com.alvaro.devutils.model.CloneObjectUtil
import com.alvaro.devutils.model.Comment
import com.alvaro.devutils.model.PKUtil
import com.alvaro.devutils.model.Row
import com.alvaro.devutils.model.SpecialDBObject
import com.alvaro.devutils.model.Table
import com.alvaro.devutils.model.User
import com.alvaro.devutils.model.UserData
import com.alvaro.devutils.model.UserSequencesTable
import com.alvaro.devutils.model.UserViewsComments
import javafx.concurrent.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Blob
import java.sql.Clob
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Clase que se encarga de clonar una base de datos Oracle a otra base de datos Oracle. Esta clase hereda de Process para poder ejecutar el proceso
 * en un hilo diferente a javafx y no bloquear la interfaz de usuario.
 * @param cloneObjectUtil objeto con los datos necesarios para la clonacion
 */
class OracleClone(private val cloneObjectUtil: CloneObjectUtil): Task<String>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OracleClone::class.java)
    }

    private var maxProgressBar: Long = 0
    private var actualProgressBar: Long = 0


    public override fun call(): String{
        this.updateMessage("Iniciando clonado")
        val targetJdbcUrl: String? = this.cloneObjectUtil.target.jdbcUrl
        val targetUsers: List<User>? = this.cloneObjectUtil.target.users
        val originJdbcUrl: String? = this.cloneObjectUtil.origin.jdbcUrl
        val originUsers: List<User>? = this.cloneObjectUtil.origin.users
        targetUsers?.forEach { targetUser: User ->
            try {
                DriverManager.getConnection(targetJdbcUrl, targetUser.username, targetUser.password).use { targetConnection: Connection ->
                    val usersData: MutableList<UserData> = CopyOnWriteArrayList()
                    this.updateMessage("Obteniendo datos de origen")
                    originUsers?.forEach { originUser: User ->
                        try {
                            DriverManager.getConnection(originJdbcUrl, originUser.username, originUser.password).use { originConnection: Connection ->
                                usersData.add(this.getUserData(originUser, originConnection))
                            }
                        } catch (e: SQLException) {
                            log.error(e.message, e)
                            //Fallo con la conexion en origen del usuario
                        }
                    }
                    this.updateProgress(0, this.maxProgressBar)
                    if(this.cloneObjectUtil.recreateTarget){
                        this.updateValue("Recreando usuarios en la base de datos destino")
                        this.recreateTarget(targetConnection)
                    }
                    usersData.forEach{ userData: UserData ->
                        this.insertUserDataSequenceTables(targetConnection, userData.userSequencesTable)
                    }
                    usersData.forEach{ userData: UserData ->
                        this.insertUserDataViewsComments(targetConnection, userData.userViewsComments)
                    }
                }
                this.updateMessage("Clonado finalizado")
                //Paramos 5 segundos y limpiamos el mensaje
                Thread.sleep(5000)
                this.updateValue("")
                this.updateMessage("")
            } catch (e: SQLException) {
                log.error(e.message, e)
                //Fallo con la conexion en destino
            }
        }
        return ""
    }

    private fun insertUserDataSequenceTables(targetConnection: Connection, userDataSequencesTable: UserSequencesTable?){
        userDataSequencesTable?.sequences?.forEach { sequence: String ->
            this.updateValue("Creando secuencia $sequence")
            this.executeQuery(targetConnection, sequence)
        }
        userDataSequencesTable?.tables?.forEach { table: Table ->
            this.updateValue("Creando tabla ${table.tableName}")
            this.executeQuery(targetConnection, table.getCreateTable())
            if(table.getAlterTablePK() != null) {
                this.executeQuery(targetConnection, table.getAlterTablePK())
            }
            table.alterTableFK.forEach { alterTableFk: String ->
                if(alterTableFk.trim().isNotEmpty()) {
                    this.executeQuery(targetConnection, alterTableFk)
                }
            }
            this.updateValue("Insertando datos en la tabla ${table.tableName}")
            this.insertData(targetConnection, table.rows, table.getInsertInto())
        }
    }

    private fun insertUserDataViewsComments(targetConnection: Connection, userDataViewsComments: UserViewsComments?){
        userDataViewsComments?.views?.forEach { view: String ->
            this.updateValue("Creando vista ${view.split(" ")[2]}")
            this.executeQuery(targetConnection, view)
        }
        userDataViewsComments?.comments?.forEach { comment: Comment ->
            this.updateValue("Insertando comentario en la tabla ${comment.insertComment.split(" ")[3]}")
            this.executeQuery(targetConnection, comment.insertComment)
        }
    }

    private fun getUserData(originUser: User, originConnection: Connection): UserData {
        val userSequences: List<String> = this.getOriginUserSequences(originConnection, originUser.username!!)
        this.updateValue("${userSequences.size} secuencias obtenidas del usuario ${originUser.username}")
        log.info("{} secuencias obtenidas del usuario {}", userSequences.size, originUser.username)

        val userTables: List<Table> = this.getOriginUserTableObject(originConnection, originUser.username!!)
        this.updateValue("${userTables.size} tablas obtenidas del usuario ${originUser.username}")
        log.info("{} tablas obtenidas del usuario {}", userTables.size, originUser.username)

        val userViews: List<String> = this.getOriginUserViews(originConnection, originUser.username!!)
        this.updateValue("${userViews.size} vistas obtenidas del usuario ${originUser.username}")
        log.info("{} vistas obtenidas del usuario {}", userViews.size, originUser.username)

        val userComments: List<Comment> = this.getOriginUserComments(originConnection, originUser.username!!)
        this.updateValue("${userComments.size} comentarios obtenidos del usuario ${originUser.username}")
        log.info("{} comentarios obtenidos del usuario {}", userComments.size, originUser.username)

        this.maxProgressBar += userSequences.size + userTables.size + userViews.size + userComments.size
        return UserData(UserSequencesTable(userSequences, userTables), UserViewsComments(userViews, userComments))
    }

    /**
     * Obtiene la lista de create sequences a partir de la conexion de origen
     *
     * @param originConnection conexion de origen de los datos
     * @param originUser       usuario actual a consultar las sequencias
     * @return lista de cadenas "CREATE SEQUENCE"
     */
    private fun getOriginUserSequences(originConnection: Connection, originUser: String): List<String> {
        val result: MutableList<String> = CopyOnWriteArrayList()
        val selectSequences = "SELECT o.owner, o.object_name FROM all_objects o WHERE o.owner ='$originUser' AND o.object_type='SEQUENCE' ORDER BY 1,2"
        try {
            originConnection.prepareStatement(selectSequences).use { psSequences: PreparedStatement ->
                psSequences.executeQuery().use { rsSequences: ResultSet ->
                    while (rsSequences.next()) {
                        val sequenceName: String = rsSequences.getString(2)
                        val sequenceOwner: String = rsSequences.getString(1)
                        val psSeqCurrVal: PreparedStatement = originConnection.prepareStatement("SELECT $sequenceOwner.$sequenceName.NEXTVAL FROM DUAL")
                        val rsSeqCurrVal: ResultSet = psSeqCurrVal.executeQuery()
                        rsSeqCurrVal.next()
                        result.add("CREATE SEQUENCE $sequenceOwner.$sequenceName INCREMENT BY 1 START WITH ${rsSeqCurrVal.getLong(1) + 1L}")
                        rsSeqCurrVal.close()
                        psSeqCurrVal.close()
                    }
                }
            }
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al obtener las sequencias y sus valores del remoto
        }
        return result
    }

    /**
     * Obtenemos la lista de tablas ordenada (Orden de creación). Con el nombre y atributos de las columnas, claves primarias, claves foráneas,
     * y datos de cada una de las tablas
     *
     * @param originConnection conexion de origen de los datos a leer
     * @param originUser       usuario de origen de los datos a leer
     * @return lista de tablas ordenada
     */
    private fun getOriginUserTableObject(originConnection: Connection, originUser: String): List<Table> {
        val result: MutableList<Table> = CopyOnWriteArrayList()
        try {
            originConnection.metaData.getTables(null, originUser, "%", arrayOf("TABLE")).use { rsTables: ResultSet ->
                //Este objeto lo utilizaremos posteriormente para averiguar las tablas que hacen referencia a otras tablas
                val tableDependencies: MutableMap<Table, MutableList<Table>> = HashMap()
                //Este objeto lo usaremos para guardar la lista de tablas que serán posteriormente ordenadas
                val tables: MutableList<Table> = CopyOnWriteArrayList()
                while (rsTables.next()) {
                    val tableName: String = rsTables.getString(3)
                    val table: Table = Table("$originUser.$tableName")
                    //Una vez obtenida la tabla, obtenemos los datos de las columnas de la misma (DDL)
                    val rsColumns: ResultSet = originConnection.metaData.getColumns(null, originUser, tableName, "%")
                    while (rsColumns.next()) {
                        val columnName: String = rsColumns.getString(4)
                        table.populateDDLColumnsCreateTable(columnName, rsColumns.getString(6), rsColumns.getString(7), rsColumns.getString(18))
                        table.populateDDLColumnsInsertInto(columnName)
                    }
                    rsColumns.close()
                    table.deleteLastCharFromCreateAndInsert()
                    val tablePKs: PKUtil? = this.getTablePKs(originConnection, originUser, tableName)
                    log.debug("Claves primarias obtenidas de la tabla {}", tableName)
                    table.createPK(tablePKs)
                    val alterTableFK: List<String> = this.getTableFK(originConnection, originUser, tableName)
                    table.alterTableFK.addAll(alterTableFK)
                    log.debug("Claves foráneas obtenidas de la tabla {}", tableName)
                    val tableData: List<Row> = this.getTableRows(originConnection, table.getSelectQuery())
                    table.rows.addAll(tableData)
                    log.debug("Datos obtenidos de la tabla {}", tableName)
                    tables.add(table)
                    tableDependencies[table] = CopyOnWriteArrayList()
                }
                tables.forEach { table: Table ->
                    this.updateTableDependencies(originConnection, originUser, table, tableDependencies, tables)
                }
                result.addAll(this.topologicalSort(tableDependencies))
                this.updateValue("${result.size} tablas obtenidas del usuario $originUser")
            }
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al obtener las tablas del usuario originUser
        }
        return result
    }

    private fun recreateTarget(targetConnection: Connection){
        targetConnection.prepareStatement("SELECT USERNAME FROM ALL_USERS").use{ ps: PreparedStatement ->
            this.executeQuery(targetConnection, "ALTER SESSION SET \"_ORACLE_SCRIPT\" = TRUE")
            ps.executeQuery().use{ rs: ResultSet ->
                while(rs.next()){
                    val username: String = rs.getString(1)
                    if(this.cloneObjectUtil.origin.users?.any{ it.username == username } == true){
                        this.executeQuery(targetConnection, "DROP USER $username CASCADE")
                        log.info("Usuario $username eliminado")
                    }
                }
            }
        }
        this.createTargetUsers(targetConnection)
        this.executeQuery(targetConnection, "ALTER PROFILE DEFAULT LIMIT PASSWORD_LIFE_TIME UNLIMITED")
    }

    private fun createTargetUsers(targetConnection: Connection) {
        this.cloneObjectUtil.origin.users?.forEach { user: User ->
            this.executeQuery(targetConnection, "CREATE USER ${user.username} IDENTIFIED BY ${user.password}")
            //TODO tener cuidado con el grant all a la hora de hacer el clonado en sitios sensibles
            this.executeQuery(targetConnection, "GRANT ALL PRIVILEGES TO ${user.username}")
        }
    }

    /**
     * Este metodo se encarga de replegar los comentarios de todas las tablas y columnas de la BBDD en una lista de objeto Comentario
     *
     * @param originConnection conexión origen de datos
     * @param originUser       usuario origen a los que consultar los datos
     * @return lista completa de los comentarios pertenecientes a este usuario
     */
    private fun getOriginUserComments(originConnection: Connection, originUser: String): List<Comment> {
        val result: MutableList<Comment> = CopyOnWriteArrayList()
        try {
            originConnection.prepareStatement("SELECT table_name, comments FROM user_tab_comments").use { psTables: PreparedStatement ->
                originConnection.prepareStatement("SELECT table_name, column_name, comments FROM user_col_comments").use { psColumns: PreparedStatement ->
                    psTables.executeQuery().use { rsTables: ResultSet ->
                        psColumns.executeQuery().use { rsColumns: ResultSet ->
                            while (rsTables.next()) {
                                val tableName: String = rsTables.getString(1)
                                val comment: String? = rsTables.getString(2)
                                if (comment != null && !tableName.contains("BIN$")) {
                                    result.add(Comment("$originUser.$tableName", null, comment))
                                }
                            }
                            while (rsColumns.next()) {
                                val tableName: String = rsColumns.getString(1)
                                val columnName: String = rsColumns.getString(2)
                                val comment: String? = rsColumns.getString(3)
                                if (comment != null && !tableName.contains("BIN$")) {
                                    result.add(Comment("$originUser.$tableName", columnName, comment))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al obtener los comentarios del origen con el usuario originUser
        }
        return result
    }

    /**
     * Obtiene un objeto de las claves primarias de cada tabla
     *
     * @param originConnection conexion origen a la que consultar los datos
     * @param originUser       usuario origen de consulta de datos
     * @param tableName        nombre de la tabla a consultar los valores de PK
     * @return objeto de nombre columnas PK o nulo si no tiene clave primaria
     */
    private fun getTablePKs(originConnection: Connection, originUser: String, tableName: String): PKUtil? {
        val result = PKUtil()
        try {
            originConnection.metaData.getPrimaryKeys(null, originUser, tableName).use { rsPK: ResultSet ->
                while (rsPK.next()) {
                    val pk: String = rsPK.getString(4)
                    val pkName: String = rsPK.getString(6)
                    if (pk.trim { it <= ' ' }.isNotEmpty()) {
                        result.pk = pkName
                        result.addPk(pk)
                    }
                }
            }
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al intentar recuperar las claves primarias del usuario en la tabla
        }
        return if (result.pk == null) null else result
    }

    /**
     * Obtiene un listado de alter table para la tabla especifica que contiene claves foráneas
     *
     * @param originConnection conexion origen a la que consultar los datos
     * @param originUser       usuario origen de consulta de datos
     * @param tableName        nombre de la tabla a consultar los valores de FK
     * @return lista de alter table referidas a la talbla que se le pasa como tercer parametro
     */
    private fun getTableFK(originConnection: Connection, originUser: String, tableName: String): List<String> {
        val result: MutableList<String> = CopyOnWriteArrayList()
        try {
            originConnection.metaData.getImportedKeys(null, originUser, tableName).use { rs: ResultSet ->
                while (rs.next()) {
                    result.add("ALTER TABLE $originUser.$tableName ADD CONSTRAINT ${rs.getString(12)} FOREIGN KEY (${rs.getString(8)}) REFERENCES $originUser.${rs.getString(3)}")
                }
            }
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al intentar recuperar las claves foraneas del usuario en la tabla
        }
        return result
    }

    /**
     * Obtiene la lista de filas que contiene la tabla
     *
     * @param originConnection origen de datos a la que hace la peticion select
     * @param selectQuery      consulta a la tabla de la que queremos extraer los datos
     * @return lista de las filas de datos
     */
    private fun getTableRows(originConnection: Connection, selectQuery: String): List<Row> {
        val result: MutableList<Row> = CopyOnWriteArrayList()
        try {
            originConnection.prepareStatement(selectQuery).use { ps: PreparedStatement ->
                ps.executeQuery().use { rs: ResultSet ->
                    while (rs.next()) {
                        val rowValues: MutableList<Any?> = CopyOnWriteArrayList()
                        for (i in 1..rs.metaData.columnCount) {
                            when (val obj: Any? = rs.getObject(i)) {
                                is Blob-> rowValues.add(SpecialDBObject(obj.getBytes(1, obj.length().toInt())))
                                is Clob-> rowValues.add(SpecialDBObject(obj.getSubString(1, obj.length().toInt())))
                                else -> rowValues.add(obj)
                            }
                        }
                        result.add(Row(rowValues))
                    }
                }
            }
            log.info("${result.size} filas obtenidas de la tabla ${selectQuery.split(" ")[3]}")
            this.maxProgressBar += result.size
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al intentar recuperar los datos de la tabla
        }
        return result
    }

    /**
     * Obtiene una lista de las vistas que tiene el usuario
     *
     * @param originConnection origen de los datos
     * @param originUser       usuario de origen
     * @return lista de cadenas "create view"
     */
    private fun getOriginUserViews(originConnection: Connection, originUser: String): List<String> {
        val result: MutableList<String> = CopyOnWriteArrayList()
        try {
            originConnection.metaData.getTables(null, originUser, "%", arrayOf("VIEW")).use { rs: ResultSet ->
                while (rs.next()) {
                    val viewName: String = rs.getString(3)
                    val sb: StringBuilder = StringBuilder("CREATE VIEW $originUser.$viewName AS ")
                    originConnection.prepareStatement("SELECT TEXT FROM ALL_VIEWS WHERE OWNER = '$originUser' AND VIEW_NAME = '$viewName'").use{
                        it.executeQuery().use { rsView: ResultSet ->
                            while (rsView.next()) {
                                sb.append(rsView.getString(1))
                            }
                        }
                    }
                    result.add(sb.toString())
                }
            }
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al intentar recuperar las vistas del usuario
        }
        return result
    }

    /**
     * Se encarga de actualizar el mapeado de tableDependencies para que posteriormente se puedan organizar las tablas
     *
     * @param originConnection  conexion de origen de los datos
     * @param originUser        usuario de conexion de origen
     * @param table             tabla a la que se hace la consulta de las claves importadas
     * @param tableDependencies mapeado de tabla con referencias a actualizar
     * @param tables            listado completo de todas las tablas del usuario
     */
    private fun updateTableDependencies(originConnection: Connection, originUser: String, table: Table, tableDependencies: Map<Table, MutableList<Table>>, tables: List<Table>) {
        try {
            originConnection.metaData.getImportedKeys(null, originUser, table.tableName.split(".")[1]).use { rs: ResultSet ->
                while (rs.next()) {
                    val fkSchemaTableName: String = "$originUser.${rs.getString("PKTABLE_NAME")}"
                    tables.forEach { table1: Table ->
                        if (table1.tableName == fkSchemaTableName) {
                            tableDependencies[table]?.add(table1)
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al obtener las claves foráneas del usuario en la tabla para ordenar las tablas
        }
    }

    /**
     * Ejecuta una sentencia en una conexion de destino
     *
     * @param target conexion a hacer la sentencia
     * @param query  cadena a ejecutar
     */
    private fun executeQuery(target: Connection, query: String?) {
        try {
            target.prepareStatement(query).use { ps: PreparedStatement ->
                val executed: Int = ps.executeUpdate()
                log.trace("Query executed: {} rows affected", executed)
                log.trace("Query executed: $query")
                this.updateProgress(++this.actualProgressBar, this.maxProgressBar)
            }
        } catch (e: SQLException) {
            log.error(e.message, e)
            //Hubo un error al ejecutar la sentencia query
        }
    }

    /**
     * Inserta las filas de datos en una conexion de destino
     *
     * @param target      conexion a ejecutar la sentencia
     * @param rows        filas de datos
     * @param insertQuery query de insert into
     */
    private fun insertData(target: Connection, rows: List<Row>, insertQuery: String) {
        rows.forEach{ row: Row->
            try {
                target.prepareStatement(insertQuery).use { ps: PreparedStatement ->
                    for((index, value) in row.columnValues.withIndex()){
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
                //Hubo un error al insertar una fila en la tabla
            }
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    /**
     * Recoge los objetos de tableDependencies y devuelve una lista ordenada
     *
     * @param tableDependencies objeto base con los datos a ordenar
     * @return lista de datos ordenada
     */
    private fun topologicalSort(tableDependencies: Map<Table, MutableList<Table>>): List<Table> {
        val result: MutableList<Table> = CopyOnWriteArrayList()
        val visited: MutableSet<Table> = HashSet()
        val visiting: MutableSet<Table> = HashSet()
        tableDependencies.keys.forEach { table: Table ->
            if (!visited.contains(table)) {
                this.topologicalSortUtil(table, tableDependencies, visited, visiting, result)
            }
        }
        return result
    }

    /**
     * Función recursiva que se encarga de ordenar las tablas
     * @param table tabla a ordenar
     * @param tableListMap mapa de tablas
     * @param visited tablas visitadas
     * @param visiting tablas visitando
     * @param result resultado con las tablas ordenadas
     */

    private fun topologicalSortUtil(table: Table, tableListMap: Map<Table, MutableList<Table>>, visited: MutableSet<Table>, visiting: MutableSet<Table>, result: MutableList<Table>) {
        if (visiting.contains(table)) {
            return
        }
        if (!visited.contains(table)) {
            visiting.add(table)
            tableListMap[table]?.forEach { table1: Table ->
                this.topologicalSortUtil(table1, tableListMap, visited, visiting, result)
            }
            visiting.remove(table)
            visited.add(table)
            result.add(table)
        }
    }
}