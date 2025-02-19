package com.alvaro.devutils.controller

import com.alvaro.devutils.model.CloneObjectUtil
import com.alvaro.devutils.model.DatabaseConnection
import com.alvaro.devutils.model.XMLWrapper
import com.alvaro.devutils.tools.Docker
import com.alvaro.devutils.tools.OracleClone
import com.alvaro.devutils.tools.Utils
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.concurrent.WorkerStateEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import java.time.LocalDateTime
import java.time.ZoneOffset

open class MainController {
    @FXML
    private lateinit var btnClone: Button
    @FXML
    private lateinit var cbDBType: ComboBox<String>
    @FXML
    private lateinit var cbOrigin: ComboBox<DatabaseConnection>
    @FXML
    private lateinit var cbTarget: ComboBox<DatabaseConnection>
    @FXML
    private lateinit var cbUsers: CheckBox
    @FXML
    private lateinit var cbDocker: CheckBox
    @FXML
    private lateinit var progressBar: ProgressBar
    @FXML
    private lateinit var lbGeneral: Label
    @FXML
    private lateinit var lbSpecific: Label
    @FXML
    private lateinit var lbTime: Label

    @FXML
    protected fun initialize() {
        this.cbDBType.items.addAll("Oracle", "PostgreSQL", "MySQL", "SQL Server")
        val xmlWrapper: XMLWrapper? = Utils.readDBFromFile()
        if(xmlWrapper != null){
            this.cbOrigin.items.addAll(FXCollections.observableList(xmlWrapper.databaseConnections))
            this.cbTarget.items.addAll(FXCollections.observableList(xmlWrapper.databaseConnections))
        }
        this.btnClone.setOnAction {
            val startTime: LocalDateTime = LocalDateTime.now()
            var docker: Docker? = null
            if(this.cbDocker.isSelected){
                docker = Docker(xmlWrapper?.dockerParams!!)
                docker.setOnRunning {
                    this.bindProperties(it)
                }
                docker.setOnSucceeded {
                    this.unbindProperties()
                }
                val thread: Thread = Thread(docker)
                thread.isDaemon = true
                thread.start()
            }
            if(this.cbOrigin.selectionModel.selectedItem == null || this.cbTarget.selectionModel.selectedItem == null){
                return@setOnAction
            }
            val recreateUsers: Boolean = this.cbUsers.isSelected
            val cloneObjectUtil: CloneObjectUtil = CloneObjectUtil(recreateUsers, recreateUsers, this.cbOrigin.selectionModel.selectedItem, this.cbTarget.selectionModel.selectedItem)
            var cloneObject: Task<String>? = null
            when(this.cbDBType.selectionModel.selectedItem) {
                "Oracle" -> {
                    this.lbGeneral.textProperty().unbind()
                    this.lbSpecific.textProperty().unbind()
                    cloneObject = OracleClone(cloneObjectUtil, docker)
                    cloneObject.setOnRunning {
                        this.bindProperties(it)
                    }
                    cloneObject.setOnSucceeded {
                        this.unbindProperties()
                    }
                    val thread: Thread = Thread(cloneObject)
                    thread.isDaemon = true
                    thread.start()
                }
                "PostgreSQL" -> {
                    println("PostgreSQL")
                }
                "MySQL" -> {
                    println("MySQL")
                }
                "SQL Server" -> {
                    println("SQL Server")
                }
            }
            cloneObject?.setOnSucceeded {
                val endTime: LocalDateTime = LocalDateTime.now()
                //Calcular el tiempo total restando el total de ambos tiempos en milisegundos y despues convertirlo a horas, minutos y segundos
                val totalTime: Long = endTime.toEpochSecond(ZoneOffset.UTC) - startTime.toEpochSecond(ZoneOffset.UTC)
                val hours: Long = totalTime / 3600
                val minutes: Long = (totalTime % 3600) / 60
                val seconds: Long = totalTime % 60
                this.lbTime.text = "${endTime.hour - startTime.hour}:${endTime.minute - startTime.minute}:${endTime.second - startTime.second}"
                this.disableEnableControls(false)
            }
        }
    }

    private fun disableEnableControls(disable: Boolean) {
        this.cbOrigin.isDisable = disable
        this.cbTarget.isDisable = disable
        this.cbUsers.isDisable = disable
        this.cbDocker.isDisable = disable
        this.cbDBType.isDisable = disable
        this.btnClone.isDisable = disable
    }


    private fun bindProperties(worker: WorkerStateEvent){
        this.progressBar.progressProperty().bind(worker.source.progressProperty())
        this.lbGeneral.textProperty().bind(worker.source.valueProperty() as ObservableValue<out String>)
        this.lbSpecific.textProperty().bind(worker.source.messageProperty())
        this.disableEnableControls(true)
    }

    private fun unbindProperties(){
        this.progressBar.progressProperty().unbind()
        this.lbGeneral.textProperty().unbind()
        this.lbSpecific.textProperty().unbind()
        this.disableEnableControls(false)
    }
}
