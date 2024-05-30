module kz.medet {
    requires javafx.controls;
    requires javafx.fxml;

    opens kz.medet.client to javafx.fxml;
    exports kz.medet.client;
}