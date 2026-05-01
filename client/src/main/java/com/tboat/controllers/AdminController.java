package com.tboat.controllers;

import com.tboat.models.AuctionSession;
import com.tboat.socket.SocketListener;
import com.tboat.socket.SocketManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminController extends BaseController implements Initializable, SocketListener {

    // Khớp 100% với các fx:id và kiểu dữ liệu mới của bạn
    @FXML private TextField txtSearch;
    @FXML private TableView<AuctionSession> tableSessions;

    // Đã đổi String thành Integer ở đây
    @FXML private TableColumn<AuctionSession, Integer> colId;

    @FXML private TableColumn<AuctionSession, String> colName;
    @FXML private TableColumn<AuctionSession, Double> colStartPrice;
    @FXML private TableColumn<AuctionSession, Double> colJump;
    @FXML private TableColumn<AuctionSession, String> colSeller;
    @FXML private TableColumn<AuctionSession, Void> colApprove;
    @FXML private TableColumn<AuctionSession, Void> colReject;

    @FXML private Label err; // Nhãn thông báo trên UI (nếu bạn có)

    private ObservableList<AuctionSession> sessionList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Map các cột với thuộc tính của AuctionSession
        this.colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        this.colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice")); // Đã khớp với hàm khởi tạo 5 tham số
        this.colJump.setCellValueFactory(new PropertyValueFactory<>("bidIncrease")); // Đã sửa "jump" thành "bidIncrease" để khớp model
        this.colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerAccountName")); // Sửa "seller" thành "sellerAccountName"

        // 2. Setup 2 cột hành động (Duyệt / Từ chối) bằng CheckBox
        this.setupActionColumn(this.colApprove, "APPROVE");
        this.setupActionColumn(this.colReject, "REJECT");

        // 3. Khởi tạo danh sách và gắn vào bảng
        this.sessionList = FXCollections.observableArrayList();
        this.tableSessions.setItems(this.sessionList);

        // 4. Xin dữ liệu từ Server khi vừa mở trang
        SocketManager.getInstance().send("GET_PENDING_ITEMS");
    }

    private void setupActionColumn(TableColumn<AuctionSession, Void> column, String actionType) {
        column.setCellFactory((param) -> new TableCell<AuctionSession, Void>() {
            private final CheckBox checkBox = new CheckBox();

            {
                this.checkBox.setOnAction((event) -> {
                    if (this.checkBox.isSelected()) {
                        // Lấy ra sản phẩm ở dòng vừa được tích
                        AuctionSession session = getTableView().getItems().get(getIndex());

                        // 1. Gửi lệnh qua Socket lên Server
                        if (actionType.equals("APPROVE")) {
                            System.out.println("Gửi lệnh duyệt: " + session.getName());
                            SocketManager.getInstance().send("APPROVE_ITEM|" + session.getId());
                        } else {
                            System.out.println("Gửi lệnh từ chối: " + session.getName());
                            SocketManager.getInstance().send("REJECT_ITEM|" + session.getId());
                        }

                        // 2. Xóa ngay lập tức khỏi bảng UI (Giống hệt logic code mẫu của bạn)
                        getTableView().getItems().remove(session);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    this.setGraphic((Node) null);
                } else {
                    // Reset CheckBox về trạng thái chưa tích khi cuộn bảng
                    this.checkBox.setSelected(false);
                    this.setGraphic(this.checkBox);
                }
            }
        });
    }

    // ==========================================================
    // LẮNG NGHE KẾT QUẢ TRẢ VỀ TỪ SERVER
    // ==========================================================
    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            String[] parts = response.split("\\|", -1);

            switch (parts[0]) {
                case "PENDING_ITEMS_RESULT":
                    sessionList.clear(); // Dọn dẹp dữ liệu cũ
                    // Giả sử dữ liệu trả về: ID, Name, StartPrice, Jump, Seller
                    for (int i = 1; i < parts.length; i++) {
                        if (!parts[i].trim().isEmpty()) {
                            String[] itemData = parts[i].split(",");
                            if (itemData.length >= 5) {
                                // Đã ép kiểu sang int bằng Integer.parseInt()
                                int id = Integer.parseInt(itemData[0]);
                                String name = itemData[1];
                                double startPrice = Double.parseDouble(itemData[2]);
                                double jump = Double.parseDouble(itemData[3]);
                                String seller = itemData[4];

                                sessionList.add(new AuctionSession(id, name, startPrice, jump, seller));
                            }
                        }
                    }
                    break;

                case "APPROVE_SUCCESS":
                    if(err != null) {
                        err.setStyle("-fx-text-fill: green;");
                        err.setText("Đã DUYỆT sản phẩm: " + parts[1]);
                    }
                    // Không cần load lại bảng vì đã xóa UI cục bộ bằng getTableView().getItems().remove(session)
                    break;

                case "REJECT_SUCCESS":
                    if(err != null) {
                        err.setStyle("-fx-text-fill: #cc7a00;");
                        err.setText("Đã TỪ CHỐI sản phẩm: " + parts[1]);
                    }
                    break;

                case "ERROR":
                    if(err != null) {
                        err.setStyle("-fx-text-fill: red;");
                        err.setText("Lỗi: " + parts[1]);
                    }
                    // Load lại dữ liệu để đồng bộ nếu có lỗi xảy ra
                    SocketManager.getInstance().send("GET_PENDING_ITEMS");
                    break;
            }
        });
    }
}