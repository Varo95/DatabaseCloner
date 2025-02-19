package com.alvaro.devutils

import com.alvaro.devutils.model.DatabaseConnection
import com.alvaro.devutils.model.DockerParams
import com.alvaro.devutils.model.ImageType
import com.alvaro.devutils.model.User
import com.alvaro.devutils.model.XMLWrapper
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import java.io.FileOutputStream
import java.io.IOException
import java.util.ResourceBundle


open class MyApp : Application() {

    private val bundle: ResourceBundle = ResourceBundle.getBundle("i18n.strings")

    @Throws(IOException::class)
    override fun start(stage: Stage) {
        stage.scene = Scene(FXMLLoader.load(MyApp::class.java.getResource("main.fxml"), this.bundle))
        stage.title = this.bundle.getString("app.title")
        stage.isResizable = true
        stage.icons.add(Image(MyApp::class.java.getResourceAsStream("icon.png")))
        stage.scene.stylesheets.add(MyApp::class.java.getResource("dark.css")?.toString())
        stage.show()
    }
}

@Throws(IOException::class)
fun main(args: Array<String>) {
    //testWrite()
    Application.launch(MyApp::class.java, *args)
    //testNull()
}

fun testWrite(){
    val dockerParams: DockerParams = DockerParams()
    dockerParams.containerName = "PruebaDDBB"
    dockerParams.containerPort = 1995
    dockerParams.imageVersion = "19.0.0.3"
    dockerParams.volumeName = "oracle-data"
    dockerParams.user = User("prueba@hotmail.com", "prueba")
    dockerParams.rootPassword = "root"
    dockerParams.imageType = ImageType.ORACLE
    val databaseOrigin: DatabaseConnection = DatabaseConnection()
    databaseOrigin.name = "Remoto"
    databaseOrigin.jdbcUrl = "jdbc:oracle:thin:@//192.168.0.225:1521/xe"
    databaseOrigin.users = listOf(User("SYSTEM", "oracle"), User("ALVARO", "oracle"))
    val databaseTarget: DatabaseConnection = DatabaseConnection()
    databaseTarget.name = "Local"
    databaseTarget.jdbcUrl = "jdbc:oracle:thin:@//localhost:1521/xe"
    databaseTarget.users = listOf(User("SYSTEM2", "oracle2"), User("ALVARO2", "oracle2"))
    val wrapper: XMLWrapper = XMLWrapper()
    wrapper.dockerParams = dockerParams
    wrapper.databaseConnections = listOf(databaseOrigin, databaseTarget)
    FileOutputStream("database.xml").use{ fos->
        val marshaller = JAXBContext.newInstance(XMLWrapper::class.java).createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        marshaller.marshal(wrapper, fos)
    }
}
