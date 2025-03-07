package com.alvaro.devutils.tools

import com.alvaro.devutils.model.CloneObjectUtil
import com.alvaro.devutils.model.Comment
import com.alvaro.devutils.model.Table
import com.alvaro.devutils.model.User
import com.alvaro.devutils.model.UserSequencesTable
import com.alvaro.devutils.model.UserViewsComments
import javafx.concurrent.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.CopyOnWriteArrayList

class MariaDBClone(private val cloneObjectUtil: CloneObjectUtil): Task<String>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MariaDBClone::class.java)
    }

    private var maxProgressBar: Long = 0
    private var actualProgressBar: Long = 0


    override fun call(): String {
        this.updateMessage("Iniciando clonado")
        val targetJdbcUrl: String? = this.cloneObjectUtil.target.jdbcUrl
        val targetUsers: List<User>? = this.cloneObjectUtil.target.users
        val originJdbcUrl: String? = this.cloneObjectUtil.origin.jdbcUrl
        val originUsers: List<User>? = this.cloneObjectUtil.origin.users
        targetUsers?.forEach { targetUser: User ->
            try {
                DriverManager.getConnection(targetJdbcUrl, targetUser.username, targetUser.password).use { targetConnection: Connection ->
                    val usersSequencesTableList: MutableList<UserSequencesTable> = CopyOnWriteArrayList()
                    val usersViewsCommentsList: MutableList<UserViewsComments> = CopyOnWriteArrayList()
                    this.updateMessage("Obteniendo datos de origen")
                    originUsers?.forEach { originUser: User ->
                        try {
                            DriverManager.getConnection(originJdbcUrl, originUser.username, originUser.password).use { originConnection: Connection ->
                                val userSequencesTable: UserSequencesTable = UserSequencesTable(this.getOriginUserSequences(originConnection, originUser.username!!), this.getOriginUserTableObject(originConnection, originUser.username!!))
                                usersSequencesTableList.add(userSequencesTable)
                                val userViewComment: UserViewsComments = UserViewsComments(this.getOriginUserViews(originConnection, originUser.username!!), this.getOriginUserComments(originConnection, originUser.username!!))
                                usersViewsCommentsList.add(userViewComment)
                                this.maxProgressBar += userSequencesTable.sequences.size + userSequencesTable.tables.size + userViewComment.views.size + userViewComment.comments.size
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
                    usersSequencesTableList.forEach{ userData: UserSequencesTable ->
                        this.insertUserDataSequenceTables(targetConnection, userData)
                    }
                    usersViewsCommentsList.forEach{ userData: UserViewsComments ->
                        this.insertUserDataViewsComments(targetConnection, userData)
                    }
                }
                this.updateMessage("Clonado finalizado")
                //Paramos 5 segundos y limpiamos el mensaje
                Thread.sleep(5000)
                this.updateValue("")
                this.updateMessage("")
            }catch (e: SQLException){
                log.error(e.message, e)
            }
        }
        return ""
    }

    private fun recreateTarget(targetConnection: Connection) {
    }

    private fun getOriginUserSequences(originConnection: Connection, originUser: String): List<String> {
        return emptyList()
    }

    private fun getOriginUserTableObject(originConnection: Connection, originUser: String): List<Table> {
        return emptyList()
    }

    private fun getOriginUserViews(originConnection: Connection, originUser: String): List<String> {
        return emptyList()
    }

    private fun getOriginUserComments(originConnection: Connection, originUser: String): List<Comment> {
        return emptyList()
    }

    private fun insertUserDataSequenceTables(targetConnection: Connection, userDataSequencesTable: UserSequencesTable?){

    }

    private fun insertUserDataViewsComments(targetConnection: Connection, userDataViewsComments: UserViewsComments?){

    }


}