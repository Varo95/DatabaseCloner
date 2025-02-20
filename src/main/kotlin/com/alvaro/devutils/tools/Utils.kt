package com.alvaro.devutils.tools

import com.alvaro.devutils.model.DatabaseConnection
import com.alvaro.devutils.model.DockerParams
import com.alvaro.devutils.model.ImageType
import com.alvaro.devutils.model.User
import com.alvaro.devutils.model.XMLWrapper
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

class Utils {
    companion object{
        fun readDBFromFile(): XMLWrapper? {
            var xmlWrapper: XMLWrapper? = null
            try {
                FileInputStream("database.xml").use { fis ->
                    xmlWrapper = JAXBContext.newInstance(XMLWrapper::class.java).createUnmarshaller().unmarshal(fis) as XMLWrapper
                }
            } catch (e: FileNotFoundException) {
                println("No se ha encontrado el archivo")
            }
            return xmlWrapper
        }

        fun createDBFileSample(){
            val dockerParams: DockerParams = DockerParams()
            dockerParams.containerName = "DDBB-Sample"
            dockerParams.containerPort = 1521
            dockerParams.imageVersion = "21.3.0.0"
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
    }
}