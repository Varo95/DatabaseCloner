package com.alvaro.devutils.tools
import javafx.fxml.FXML
import kotlin.test.Test

class UtilsTest {

    @Test
    fun testWriteXML(){
        Utils.createDBFileSample()
    }

    @FXML
    fun testReadXML(){
        Utils.readDBFromFile()
    }
}