package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminController extends BaseController implements Initializable {

    @FXML
    private TextField txtSearch;
    @FXML
    private TableView<AuctionSession> tableSessions;
    @FXML
    private TableColumn<AuctionSession, String> colId;
    @FXML
    private TableColumn<AuctionSession, String> colName;
    @FXML
    private TableColumn<AuctionSession, Double> colStartPrice;
    @FXML
    private TableColumn<AuctionSession, Double> colJump;
    @FXML
    private TableColumn<AuctionSession, String> colSeller;
    @FXML
    private TableColumn<AuctionSession, Void> colApprove;
    @FXML
    private TableColumn<AuctionSession, Void> colReject;

    private ObservableList<AuctionSession> sessionList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colJump.setCellValueFactory(new PropertyValueFactory<>("jump"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));

        setupActionColumn(colApprove, "Đã duyệt");
        setupActionColumn(colReject, "Đã từ chối");

        sessionList = FXCollections.observableArrayList(
                new AuctionSession("SS001", "MacBook Pro M4", 35000000, 38500000,"Phuc"),
                new AuctionSession("SS002", "Màn hình Dell UltraSharp", 8000000, 8000000, "Phu"),
                new AuctionSession("SS003", "Bàn phím cơ Keychron", 1500000, 1500000, "Tam")
        );

        tableSessions.setItems(sessionList);
    }

    private void setupActionColumn(TableColumn<AuctionSession, Void> column, String actionMessage) {
        column.setCellFactory(param -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(event -> {
                    if (checkBox.isSelected()) {
                        AuctionSession session = getTableView().getItems().get(getIndex());
                        System.out.println(actionMessage + ": " + session.getName());
                        getTableView().getItems().remove(session);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(false);
                    setGraphic(checkBox);
                }
            }
        });
    }
}