module cz.vse.klient {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;
    // pokud používáš Log4j2 core, přidej také:


    opens cz.vse.klient to javafx.fxml;
    exports cz.vse.klient;
}