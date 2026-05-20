package com.tboat.controllers;

import com.tboat.controllers.helper.NotificationManager;
import com.tboat.utilsclient.HeaderUtils;
import com.tboat.controllers.helper.NotificationManager.NotificationItem;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Popup;

import java.net.URL;
import java.util.ResourceBundle;

public class HeaderController extends BaseController implements Initializable {

    @FXML private Label     lblGreeting;
    @FXML private ImageView userAvatar;
    @FXML private Button    btnBell;
    @FXML private Label     lblBadge;
    @FXML private StackPane bellPane;

    // ── Trỏ thẳng vào Singleton — KHÔNG tự lưu state ────────────────────────
    private final NotificationManager mgr = NotificationManager.getInstance();

    private FilteredList<NotificationItem> filteredItems;
    private String activeTab = "all";

    private Popup                      popup;
    private ListView<NotificationItem> notifList;
    private Label                      lblEmpty;
    private Button                     tabAllBtn;
    private Button                     tabUnreadBtn;
    private final javafx.collections.ListChangeListener<NotificationItem> notificationListener = c -> {
        updateBadge();
        updateEmptyState();
        if (notifList != null) {
            notifList.refresh();
        }
    };

    // ════════════════════════════════════════════════════════════════════════
    // KHỞI TẠO — chạy lại mỗi lần chuyển trang nhưng dữ liệu vẫn còn
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);

        // FilteredList bọc list của Singleton — dữ liệu không bao giờ mất
        filteredItems = new FilteredList<>(mgr.getItems(), item -> true);

        buildPopup();
        updateBadge(); // hiện đúng số badge ngay khi load lại trang
        updateEmptyState();

        mgr.getItems().addListener(new javafx.collections.WeakListChangeListener<>(notificationListener));

        // Đóng popup khi click ra ngoài
        btnBell.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnMousePressed(e -> {
                    if (popup.isShowing() && !bellPane.isHover()) {
                        popup.hide();
                    }
                });
            }
        });

        // Thông báo được đẩy vào từ NotificationManager đã đăng ký trong ClientApp.
    }

    @Override
    public void onReload() {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
        updateBadge();
    }

    // ════════════════════════════════════════════════════════════════════════
    // XÂY DỰNG POPUP
    // ════════════════════════════════════════════════════════════════════════
    private void buildPopup() {
        popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox popupRoot = new VBox();
        popupRoot.setPrefWidth(360);
        popupRoot.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 24, 0, 0, 8);"
        );

        popupRoot.getChildren().addAll(
                buildHeader(),
                buildTabs(),
                buildList(),
                buildEmptyLabel()
        );

        popup.getContent().add(popupRoot);
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12 12 0 0;" +
                        "-fx-border-color: transparent transparent #F0F0F0 transparent;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-padding: 14 16 12 16;"
        );
        Label title = new Label("Thông báo");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #050505;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button markAllBtn = buildTextBtn("Đánh dấu đã đọc");
        markAllBtn.setOnAction(e -> markAllRead());
        header.getChildren().addAll(title, spacer, markAllBtn);
        return header;
    }

    private HBox buildTabs() {
        HBox tabs = new HBox(8);
        tabs.setStyle("-fx-padding: 10 16 6 16; -fx-background-color: white;");
        tabAllBtn    = buildTabButton("Tất cả",   true);
        tabUnreadBtn = buildTabButton("Chưa đọc", false);

        tabAllBtn.setOnAction(e -> {
            if (!activeTab.equals("all")) {
                activeTab = "all";
                filteredItems.setPredicate(item -> true);
                setTabActive(tabAllBtn, true);
                setTabActive(tabUnreadBtn, false);
                updateEmptyState();
            }
        });

        tabUnreadBtn.setOnAction(e -> {
            if (!activeTab.equals("unread")) {
                activeTab = "unread";
                filteredItems.setPredicate(item -> !item.isRead());
                setTabActive(tabAllBtn, false);
                setTabActive(tabUnreadBtn, true);
                updateEmptyState();
            }
        });

        tabs.getChildren().addAll(tabAllBtn, tabUnreadBtn);
        return tabs;
    }

    // Chiều cao mỗi cell (title + subtitle + time + padding)
    private static final double CELL_HEIGHT = 76.0;
    // Hiện tối đa 5 cell, thêm thì scroll dọc
    private static final int    MAX_VISIBLE = 5;

    private ListView<NotificationItem> buildList() {
        notifList = new ListView<>(filteredItems);
        notifList.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0;"
        );
        updateListHeight();

        // Khi số item thay đổi → tính lại chiều cao popup
        filteredItems.addListener(
                (javafx.collections.ListChangeListener<NotificationItem>) c -> updateListHeight()
        );

        // Ẩn horizontal scrollbar — dùng CSS thay vì lookup (chạy sớm hơn, chắc chắn hơn)
        notifList.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0;"
        );
        // Backup: tắt hẳn horizontal scrollbar sau khi skin sẵn sàng
        notifList.skinProperty().addListener((obs, o, skin) -> {
            if (skin == null) return;
            // Chạy sau 1 pulse để đảm bảo scroll-bar nodes đã được tạo
            javafx.application.Platform.runLater(() -> {
                notifList.lookupAll(".scroll-bar").forEach(node -> {
                    ScrollBar sb = (ScrollBar) node;
                    if (sb.getOrientation() == Orientation.HORIZONTAL) {
                        sb.setPrefHeight(0);
                        sb.setMaxHeight(0);
                        sb.setVisible(false);
                        sb.setManaged(false);
                    }
                });
            });
        });
        notifList.setCellFactory(lv -> new NotificationCell());
        return notifList;
    }

    private void updateListHeight() {
        int visible = Math.min(filteredItems.size(), MAX_VISIBLE);
        notifList.setPrefHeight(visible * CELL_HEIGHT);
        notifList.setMaxHeight(MAX_VISIBLE * CELL_HEIGHT);
    }

    private Label buildEmptyLabel() {
        lblEmpty = new Label("Không có thông báo nào");
        lblEmpty.setStyle(
                "-fx-font-size: 14px; -fx-text-fill: #65676B;" +
                        "-fx-alignment: center; -fx-padding: 30 16;"
        );
        lblEmpty.setMaxWidth(Double.MAX_VALUE);
        lblEmpty.setVisible(false);
        lblEmpty.setManaged(false);
        return lblEmpty;
    }


    // ════════════════════════════════════════════════════════════════════════
    // CUSTOM CELL
    // ════════════════════════════════════════════════════════════════════════
    private class NotificationCell extends ListCell<NotificationItem> {
        private final HBox  row     = new HBox(12);
        private final Label avatar  = new Label();
        private final VBox  textBox = new VBox(3);
        private final Label lTitle  = new Label();
        private final Label lSub    = new Label();
        private final Label lTime   = new Label();
        private final Label dot     = new Label("●");

        NotificationCell() {
            row.setAlignment(Pos.CENTER_LEFT);
            avatar.setMinWidth(52);  avatar.setMinHeight(52);
            avatar.setMaxWidth(52);  avatar.setMaxHeight(52);
            lTitle.setWrapText(true); lTitle.setMaxWidth(230);
            lSub.setWrapText(true);   lSub.setMaxWidth(230);
            lSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676B;");
            dot.setStyle("-fx-font-size: 10px; -fx-text-fill: #1877F2;");
            textBox.getChildren().addAll(lTitle, lSub, lTime);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            row.getChildren().addAll(avatar, textBox, dot);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-padding: 0;");

            setOnMouseClicked(e -> {
                NotificationItem item = getItem();
                if (item != null && !item.isRead()) {
                    item.markRead(); // ghi thẳng vào object trong Singleton
                    updateBadge();
                    if (activeTab.equals("unread")) {
                        filteredItems.setPredicate(i -> !i.isRead());
                    }
                    notifList.refresh();
                    updateEmptyState();
                }
            });
            setOnMouseEntered(e -> {
                if (getItem() != null)
                    row.setStyle("-fx-padding: 10 16; -fx-background-color: #F2F2F2; -fx-background-radius: 8;");
            });
            setOnMouseExited(e -> updateRowStyle());
        }

        @Override
        protected void updateItem(NotificationItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                avatar.setText(item.getAvatarText());
                avatar.setStyle(
                        "-fx-background-color: " + item.getAvatarColor() + ";" +
                                "-fx-background-radius: 26; -fx-alignment: center;" +
                                "-fx-font-size: 17px; -fx-font-weight: bold;" +
                                "-fx-min-width: 52; -fx-min-height: 52;" +
                                "-fx-max-width: 52; -fx-max-height: 52;"
                );
                lTitle.setText(item.getTitle());
                lSub.setText(item.getSubtitle());
                lTime.setText(item.getTime());
                boolean unread = !item.isRead();
                lTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #050505;" +
                        (unread ? " -fx-font-weight: bold;" : ""));
                lTime.setStyle("-fx-font-size: 13px;" +
                        (unread ? "-fx-font-weight: bold; -fx-text-fill: #1877F2;"
                                : "-fx-text-fill: #65676B;"));
                dot.setVisible(unread);
                updateRowStyle();
                setGraphic(row);
            }
        }

        private void updateRowStyle() {
            NotificationItem item = getItem();
            if (item != null && !item.isRead()) {
                row.setStyle("-fx-padding: 10 16; -fx-background-color: #E8F0FE; -fx-background-radius: 8;");
            } else {
                row.setStyle("-fx-padding: 10 16; -fx-background-color: transparent;");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════
    private void updateBadge() {
        long unread = mgr.getUnreadCount();
        if (unread > 0) {
            lblBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            lblBadge.setVisible(true);
        } else {
            lblBadge.setVisible(false);
        }
    }

    private void updateEmptyState() {
        boolean empty = filteredItems.isEmpty();
        lblEmpty.setVisible(empty);
        lblEmpty.setManaged(empty);
        notifList.setVisible(!empty);
        notifList.setManaged(!empty);
    }

    private Button buildTabButton(String text, boolean active) {
        Button btn = new Button(text);
        setTabActive(btn, active);
        return btn;
    }

    private void setTabActive(Button btn, boolean active) {
        btn.setStyle(active
                ? "-fx-background-color: #E8F0FE; -fx-text-fill: #1877F2;" +
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 14;"
                : "-fx-background-color: #F0F2F5; -fx-text-fill: #050505;" +
                "-fx-font-size: 13px; -fx-background-radius: 20;" +
                "-fx-cursor: hand; -fx-padding: 6 14;"
        );
    }

    private Button buildTextBtn(String text) {
        Button btn = new Button(text);
        String normal = "-fx-background-color: transparent; -fx-text-fill: #1877F2;" +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;" +
                "-fx-background-radius: 8; -fx-padding: 6 10;";
        String hover  = "-fx-background-color: #E8F0FE; -fx-text-fill: #1877F2;" +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;" +
                "-fx-background-radius: 8; -fx-padding: 6 10;";
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(normal));
        return btn;
    }

    // ════════════════════════════════════════════════════════════════════════
    // FXML ACTIONS
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    private void toggleNotificationPopup() {
        if (popup.isShowing()) {
            popup.hide();
        } else {
            Bounds b = btnBell.localToScreen(btnBell.getBoundsInLocal());
            popup.show(btnBell, b.getMaxX() - 360, b.getMaxY() + 8);
        }
    }

    @FXML
    private void markAllRead() {
        mgr.markAllRead();
        if (activeTab.equals("unread")) {
            filteredItems.setPredicate(i -> !i.isRead());
        }
        notifList.refresh();
        updateBadge();
        updateEmptyState();
    }


}
