package com.alvaro.devutils.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlElementWrapper
import jakarta.xml.bind.annotation.XmlRootElement
import java.io.Serial
import java.io.Serializable
import java.util.Objects

@XmlRootElement(name = "Root")
@XmlAccessorType(XmlAccessType.FIELD)
class XMLWrapper : Serializable {
    @XmlElementWrapper(name = "DatabaseConnections")
    @XmlElement(name = "DatabaseConnection", type = DatabaseConnection::class)
    var databaseConnections: List<DatabaseConnection>? = null
    @XmlElement(name = "DockerParams", type = DockerParams::class)
    var dockerParams: DockerParams? = null

    override fun equals(other: Any?): Boolean {
        if (other !is XMLWrapper) return false
        return this.databaseConnections == other.databaseConnections
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this.databaseConnections)
    }

    companion object {
        @Serial
        private const val serialVersionUID = 1L
    }
}

@XmlRootElement(name = "DatabaseConnection")
@XmlAccessorType(XmlAccessType.FIELD)
class DatabaseConnection : Serializable {
    @XmlAttribute
    var name: String? = null

    @XmlAttribute
    var jdbcUrl: String? = null

    @XmlElementWrapper(name = "Users")
    @XmlElement(name = "User", type = User::class)
    var users: List<User>? = null

    override fun toString(): String {
        return name!!
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DatabaseConnection) return false
        return this.jdbcUrl == other.jdbcUrl
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this.jdbcUrl)
    }

    companion object {
        @Serial
        private const val serialVersionUID = 1L
    }
}

@XmlRootElement(name = "DatabaseConnection")
@XmlAccessorType(XmlAccessType.FIELD)
class User : Serializable {
    @XmlAttribute(name = "username")
    var username: String? = null

    @XmlAttribute(name = "password")
    var password: String? = null

    constructor(username: String?, password: String?) {
        this.username = username
        this.password = password
    }

    constructor()

    override fun equals(other: Any?): Boolean {
        if (other !is User) return false
        return this.username == other.username
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this.username)
    }

    companion object {
        @Serial
        private const val serialVersionUID = 1L
    }
}

enum class ImageType { ORACLE, POSTGRESQL, MARIADB, SQLSERVER }

@XmlRootElement(name = "DatabaseConnection")
@XmlAccessorType(XmlAccessType.FIELD)
class DockerParams : Serializable {
    @XmlElement(name = "ImageType")
    var imageType: ImageType? = null

    @XmlElement(name = "ContainerName")
    var containerName: String? = null

    @XmlElement(name = "ContainerPort")
    var containerPort = 0

    @XmlElement(name = "ContainerVolumeName")
    var volumeName: String? = null

    @XmlElement(name = "ContainerVersion")
    var imageVersion: String? = null

    //Variable para el uso del comando docker login contra el registry, principalmente, el de oracle
    @XmlElement(name = "User", type = User::class)
    var user: User? = null

    //Variable usada para la contraseña del usuario administrador de la base de datos
    @XmlElement(name = "RootPassword")
    var rootPassword: String? = null

    override fun equals(other: Any?): Boolean {
        if (other !is DockerParams) return false
        return this.containerName == other.containerName
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this.containerName)
    }

    companion object {
        @Serial
        private const val serialVersionUID = 1L
    }
}

@XmlRootElement(name = "Git")
@XmlAccessorType(XmlAccessType.FIELD)
class GitRoot: Serializable{
    @XmlAttribute
    var defaultBranch: String? = null

}