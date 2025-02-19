package com.alvaro.devutils.tools

import com.alvaro.devutils.model.XMLWrapper
import jakarta.xml.bind.JAXBContext
import java.io.FileInputStream
import java.io.FileNotFoundException

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
    }
}