module DatabaseCloner{
    requires kotlin.stdlib;
    requires java.sql;
    requires org.slf4j;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires jakarta.xml.bind;

    opens com.alvaro.devutils to javafx.fxml, javafx.controls, javafx.graphics, javafx.base;

    exports com.alvaro.devutils;
}