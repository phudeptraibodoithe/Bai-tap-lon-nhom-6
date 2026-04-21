package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.io.IOException;

public class TrangChuController {

    // --- CÁC NÚT MENU ĐIỀU HƯỚNG CHUẨN ---
    @FXML private Button btnHome;
    @FXML private Button btnHistory;
    @FXML private Button btnPostItem;
    @FXML private Button btnWallet;
    @FXML private Button btnProfile;

    private Stage stage;
    private Scene scene;
    private Parent root;
    private String imagePath;

    // --- KHU VỰC TÌM KIẾM ---
    @FXML private MenuButton categoryMenu; // Nút xổ xuống danh sách loại SP
    @FXML private TextField searchField;   // Ô nhập chữ tìm kiếm
    @FXML private Button btnSearch;        // Nút ấn tìm kiếm

    // --- KHU VỰC CHỨA SẢN PHẨM ---
    @FXML private FlowPane liveAuctionsContainer; // Nơi bạn sẽ dùng Java để add() các VBox (thẻ SP) vào

    // ==============================================
    // XỬ LÝ SỰ KIỆN TÌM KIẾM & LỌC DANH MỤC
    // ==============================================

    @FXML
    void handleCategorySelect(ActionEvent event) {
        // Lấy MenuItem mà người dùng vừa bấm vào
        MenuItem selectedItem = (MenuItem) event.getSource();
        String categoryName = selectedItem.getText();

        // Đổi tên của nút MenuButton thành danh mục đang chọn để người dùng dễ nhìn
        categoryMenu.setText(categoryName);

        System.out.println("Đang lọc danh sách hiển thị theo: " + categoryName);
        // Viết logic gọi CSDL để load lại FlowPane tại đây
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        String currentCategory = categoryMenu.getText();

        System.out.println("Đang tìm kiếm từ khóa: '" + keyword + "' trong danh mục: '" + currentCategory + "'");
        // Viết logic tìm kiếm CSDL và cập nhật FlowPane tại đây
    }

    // ==============================================
    // CÁC HÀM ĐIỀU HƯỚNG THEO QUY CHUẨN
    // ==============================================

    public void switchToProfile(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/profile.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }

    public void switchToPostItem(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/postItem.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }

    public void switchToHistory(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/history.fxml"));
        scene = ((Node) e.getSource()).getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/Button.css").toExternalForm());
        scene.setRoot(root);
    }

    @FXML
    void switchToWallet(ActionEvent event) {
        System.out.println("Chuyển sang Ví...");
    }
}