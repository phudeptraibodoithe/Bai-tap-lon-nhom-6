package com.tboat.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MyItemsController extends BaseController implements Initializable {

    @FXML
    private TableView<MyItem> tableMyItems;
    @FXML
    private TableColumn<MyItem, String> colId;
    @FXML
    private TableColumn<MyItem, String> colName;
    @FXML
    private TableColumn<MyItem, String> colRole;
    @FXML
    private TableColumn<MyItem, Double> colPrice;
    @FXML
    private TableColumn<MyItem, String> colStatus;
    @FXML
    private TableColumn<MyItem, Void> colAction;

    private ObservableList<MyItem> itemList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Map các cột với thuộc tính của class MyItem
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Cấu hình cột Thao tác (Hiển thị nút "Xem")
        setupActionButton();

        // 3. Tạo dữ liệu mẫu
        itemList = FXCollections.observableArrayList(
                new MyItem("SP001", "MacBook Pro M4 2026", "Người đăng", 35000000, "Đang diễn ra", "Laptop Apple chip M4 mới nhất, RAM 16GB, SSD 512GB", 1000000),
                new MyItem("SP002", "Chuột Logitech MX Master 3", "Người đăng", 1500000, "Chờ duyệt", "Chuột không dây công thái học, màu xám đen, mới 100%", 50000),
                new MyItem("SP005", "iPhone 16 Pro Max", "Người đấu giá", 28500000, "Đang dẫn đầu", "Điện thoại iPhone 16 Pro Max 256GB Titan tự nhiên", 500000),
                new MyItem("SP008", "Đồng hồ Apple Watch Series 10", "Người đấu giá", 9000000, "Bị vượt giá", "Apple Watch Series 10 viền nhôm dây cao su", 200000)
        );

        tableMyItems.setItems(itemList);
    }

    private void setupActionButton() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("Xem chi tiết");

            {
                // Style cho nút bấm
                btnView.setStyle("-fx-background-color: #08103F; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");

                // Xử lý sự kiện khi bấm nút
                btnView.setOnAction(event -> {
                    // Lấy ra sản phẩm tương ứng với dòng được bấm
                    MyItem selectedItem = getTableView().getItems().get(getIndex());
                    System.out.println("Đang mở chi tiết sản phẩm: " + selectedItem.getName());
                    changeScene(tableMyItems,"items.fxml");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnView);
                }
            }
        });
    }
}