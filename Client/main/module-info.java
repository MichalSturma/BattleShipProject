module cz.vse.klient {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;


    opens cz.vse.klient to javafx.fxml;
    exports cz.vse.klient;
}