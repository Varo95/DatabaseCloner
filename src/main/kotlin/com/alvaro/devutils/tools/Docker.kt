package com.alvaro.devutils.tools

import com.alvaro.devutils.model.DockerParams
import com.alvaro.devutils.model.ImageType
import javafx.concurrent.Task
import java.util.Locale

/**
 * Clase que se encarga de ejecutar los comandos de docker. Esta clase hereda de Process para poder ejecutar los comandos
 * en un hilo diferente a javafx y no bloquear la interfaz de usuario.
 * @param dockerParams Parametros necesarios para la creacion del contenedor
 */
class Docker(private val dockerParams: DockerParams) : Task<String>() {

    private val downloadProgressStrings: MutableList<String> = mutableListOf()
    private val commands: Array<String>

    init {
        val imageVersion: String = this.dockerParams.imageVersion ?: "latest"
        when (this.dockerParams.imageType) {
            ImageType.ORACLE -> {
                this.commands = arrayOf(
                    "docker login --username=${this.dockerParams.user?.username} --password=${this.dockerParams.user?.password} container-registry.oracle.com",
                    "docker volume create ${this.dockerParams.volumeName}",
                    "docker pull container-registry.oracle.com/database/enterprise:${imageVersion}",
                    "docker run --name ${this.dockerParams.containerName} --restart always -e ORACLE_SID=ee -e ORACLE_PDB=ORCLPDB1 -e ORACLE_PWD=${this.dockerParams.rootPassword} -p${this.dockerParams.containerPort}:1521 -v ${this.dockerParams.volumeName}:/opt/oracle/oradata container-registry.oracle.com/database/enterprise:${imageVersion}"
                )
            }
            ImageType.MARIADB -> {
                this.commands = arrayOf(
                    "docker volume create ${this.dockerParams.volumeName}",
                    "docker pull mariadb:${imageVersion}",
                    "docker run --name ${this.dockerParams.containerName} --restart always -e MYSQL_ROOT_PASSWORD=${this.dockerParams.rootPassword} -p${this.dockerParams.containerPort}:3306 -v ${this.dockerParams.volumeName}:/var/lib/mysql mariadb:${imageVersion}"
                )
            }
            ImageType.POSTGRESQL -> {
                this.commands = arrayOf(
                    "docker volume create ${this.dockerParams.volumeName}",
                    "docker pull postgres:${imageVersion}",
                    "docker run --name ${this.dockerParams.containerName} --restart always -e POSTGRES_PASSWORD=${this.dockerParams.rootPassword} -p${this.dockerParams.containerPort}:5432 -v ${this.dockerParams.volumeName}:/var/lib/postgresql/data postgres:${imageVersion}"
                )
            }
            ImageType.SQLSERVER -> {
                this.commands = arrayOf(
                    "docker volume create ${this.dockerParams.volumeName}",
                    "docker pull mcr.microsoft.com/mssql/server:${imageVersion}",
                    "docker run --name ${this.dockerParams.containerName} --restart always -e ACCEPT_EULA=Y -e SA_PASSWORD=${this.dockerParams.rootPassword} -p${this.dockerParams.containerPort}:1433 -v ${this.dockerParams.volumeName}:/var/opt/mssql mcr.microsoft.com/mssql/server:${imageVersion}"
                )
            }
            null -> this.commands = arrayOf()
        }
    }

    override fun call(): String {
        try {
            for ((actualProgress, command) in this.commands.withIndex()) {
                val proc: Process = ProcessBuilder(command.split(" ")).start()
                proc.inputStream.reader().buffered().use { reader ->
                    val outputText: String = when {
                        command.contains("docker login") -> "Logueando en el registry de oracle"
                        command.contains("docker volume create") -> "Creando volumen ${this.dockerParams.volumeName}"
                        command.contains("docker pull") -> "Descargando imagen de ${this.dockerParams.imageType.toString().lowercase(Locale.getDefault())}: ${this.dockerParams.imageVersion}"
                        command.contains("docker run") -> "Creando contenedor ${this.dockerParams.containerName}"
                        else -> ""
                    }
                    this.updateValue(outputText)
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        when {
                            (line as String).contains("Pulling fs layer") -> this.downloadProgressStrings.add(line?.split(":")!![0])
                            (line as String).contains("Download complete") -> {
                                val beforeSize: Int = this.downloadProgressStrings.size
                                this.downloadProgressStrings.remove(line?.split(":")!![0])
                                val progress: Double = (actualProgress - 0.1) - (this.downloadProgressStrings.size.toDouble() / beforeSize) + actualProgress
                                this.updateProgress(progress, this.commands.size.toDouble())
                            }
                            (line as String).contains("% complete") -> {
                                val progress: Double = (line?.split(" ")!![0].split("%")[0].toDouble() / 100) + actualProgress
                                this.updateProgress(progress, this.commands.size.toDouble())
                            }
                            (line as String).contains("DATABASE IS READY TO USE!") ||
                            (line as String).contains("MariaDB init process done. Ready for start up.") ||
                            (line as String).contains("database system is ready to accept connections") ||
                            (line as String).contains("Recovery is complete. This is an informational message only. No user action is required.") -> {
                                reader.close()
                                proc.destroy()
                                break
                            }
                        }
                        this.updateMessage(line)
                    }
                }
                proc.waitFor()
                this.updateProgress(actualProgress + 1L, this.commands.size.toLong())
            }
            return "Creado contenedor ${this.dockerParams.containerName} en docker"
        } catch (e: Exception) {
            println("Error al ejecutar el comando")
            println(e.message)
        }
        return ""
    }


}