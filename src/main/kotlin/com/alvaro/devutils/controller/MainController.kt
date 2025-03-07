package com.alvaro.devutils.controller

import com.alvaro.devutils.model.CloneObjectUtil
import com.alvaro.devutils.model.DatabaseConnection
import com.alvaro.devutils.model.XMLWrapper
import com.alvaro.devutils.tools.Docker
import com.alvaro.devutils.tools.MariaDBClone
import com.alvaro.devutils.tools.OracleClone
import com.alvaro.devutils.tools.Utils
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.concurrent.WorkerStateEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import java.util.concurrent.TimeUnit

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
        this.cbDBType.items.addAll("Oracle", "PostgreSQL", "MariaDB", "SQL Server")
        val xmlWrapper: XMLWrapper? = Utils.readDBFromFile()
        if(xmlWrapper != null){
            this.cbOrigin.items.addAll(FXCollections.observableList(xmlWrapper.databaseConnections))
            this.cbTarget.items.addAll(FXCollections.observableList(xmlWrapper.databaseConnections))
        }
        this.btnClone.setOnAction {
            val startTime: Long = System.currentTimeMillis()
            var dockerThread: Thread? = null
            if(this.cbDocker.isSelected){
                val docker = Docker(xmlWrapper?.dockerParams!!)
                docker.setOnRunning {
                    this.bindProperties(it)
                }
                docker.setOnSucceeded {
                    this.unbindProperties()
                }
                dockerThread = Thread(docker)
                dockerThread.isDaemon = true
            }
            if((this.cbOrigin.selectionModel.selectedItem == null || this.cbTarget.selectionModel.selectedItem == null) ||
                (this.cbOrigin.selectionModel.selectedItem == this.cbTarget.selectionModel.selectedItem) ||
                (this.cbOrigin.selectionModel.selectedItem == null && this.cbTarget.selectionModel.selectedItem == null)){
                return@setOnAction
            }
            val recreateUsers: Boolean = this.cbUsers.isSelected
            val cloneObjectUtil: CloneObjectUtil = CloneObjectUtil(recreateUsers, this.cbOrigin.selectionModel.selectedItem, this.cbTarget.selectionModel.selectedItem)
            var cloneTask: Thread? = null
            when(this.cbDBType.selectionModel.selectedItem) {
                "Oracle" -> {
                    val cloneData: OracleClone = OracleClone(cloneObjectUtil)
                    cloneData.setOnRunning {
                        this.bindProperties(it)
                    }
                    cloneData.setOnSucceeded {
                        this.unbindProperties()
                    }
                    cloneTask = Thread(cloneData)
                    cloneTask.isDaemon = true
                }
                "PostgreSQL" -> {
                    println("PostgreSQL")
                }
                "MariaDB" -> {
                    val cloneData: MariaDBClone = MariaDBClone(cloneObjectUtil)
                    cloneData.setOnRunning {
                        this.bindProperties(it)
                    }
                    cloneData.setOnSucceeded {
                        this.unbindProperties()
                    }
                    cloneTask = Thread(cloneData)
                    cloneTask.isDaemon = true
                }
                "SQL Server" -> {
                    println("SQL Server")
                }
            }
            val joinedThreads: Thread = object : Thread() {
                override fun run() {
                    dockerThread?.start()
                    dockerThread?.join()
                    cloneTask?.start()
                    cloneTask?.join()
                    Platform.runLater{
                        val endTime: Long = System.currentTimeMillis()
                        val totalTime: Long = TimeUnit.MILLISECONDS.toSeconds(endTime - startTime)
                        val hours: Long = TimeUnit.SECONDS.toHours(totalTime)
                        val minutes: Long = TimeUnit.SECONDS.toMinutes(totalTime) - TimeUnit.HOURS.toMinutes(hours)
                        val seconds: Long = totalTime - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours)
                        lbTime.text = "Tiempo total: $hours:$minutes:$seconds"
                        disableEnableControls(false)
                    }
                }
            }
            joinedThreads.start()
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
        this.lbGeneral.textProperty().bind(Bindings.createStringBinding({ (worker.source.valueProperty().value?: "").toString() }, worker.source.valueProperty()))
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
