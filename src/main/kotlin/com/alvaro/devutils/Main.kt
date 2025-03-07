package com.alvaro.devutils

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
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
    Application.launch(MyApp::class.java, *args)
}
