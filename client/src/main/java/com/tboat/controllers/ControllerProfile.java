package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.tboat.models.core.User;
import com.tboat.session.UserSession;
import com.tboat.socket.SocketHelper;
import com.tboat.socket.SocketListener;
import com.tboat.ucb.DataCache;
import com.tboat.ucb.NavigationContext;
import com.tboat.utilsclient.AlertUtils;
import com.tboat.utilsclient.CurrencyFormatter;
import com.tboat.utilsclient.ImageUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ControllerProfile extends BaseController implements Initializable, SocketListener {

    private User user;
    private File selectedFile;
    private boolean isEditing = false;

    private static final double CIRCLE_RADIUS = 110.0;
    private static final Logger log = Logger.getLogger(ControllerProfile.class.getName());

    @FXML private ImageView myImageView;
    @FXML private Label nickname, balance, err;
    @FXML private TextField tfNickname, tfEmail, tfPhone;
    @FXML private TextArea desc;
    @FXML private Button btnEdit, btnSave, btnCancel;

    private static final String STYLE_SUCCESS    = "#2ecc71";
    private static final String STYLE_ERROR      = "#e74c3c";
    private static final String STYLE_PROCESSING = "#3498db";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (err != null) err.setText("");
        setEditMode(false); // Mặc định: chỉ xem

        user = UserSession.getInstance().getUser();
        if (user != null) updateUI(user);

        String screenKey = BaseController.toScreenKey("profile.fxml");
        String cached    = DataCache.getInstance().get("PROFILE");
        if (cached != null) {
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            handleServerResponse(cached);
        } else {
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
        }
        SocketHelper.sendRequest("PROFILE");
    }

    // ── Edit mode toggle ──────────────────────────────────────────────────────

    @FXML
    public void onEdit(ActionEvent e) {
        setEditMode(true);
    }

    @FXML
    public void onSave(ActionEvent e) {
        if (!validateInputs()) return;

        String imageData = (selectedFile != null)
                ? ImageUtils.fileToBase64(selectedFile)
                : (user.getAvatarURL() != null && !user.getAvatarURL().isEmpty()
                ? user.getAvatarURL() : "null");

        AlertUtils.showStatus(err, "Đang xử lý...", STYLE_PROCESSING);

        JsonObject payload = new JsonObject();
        payload.addProperty("nickname",    tfNickname.getText().trim());
        payload.addProperty("description", desc.getText() == null ? "" : desc.getText().trim());
        payload.addProperty("avatarURL",   imageData);
        payload.addProperty("email",       tfEmail.getText().trim());
        payload.addProperty("phone",       tfPhone.getText().trim());
        SocketHelper.sendRequest("UPDATE_PROFILE", payload);
    }

    @FXML
    public void onCancelEdit(ActionEvent e) {
        // Hoàn tác về giá trị cũ
        if (user != null) updateUI(user);
        selectedFile = null;
        setEditMode(false);
        err.setText("");
    }

    private boolean validateInputs() {
        String nick  = tfNickname.getText().trim();
        String email = tfEmail.getText().trim();
        String phone = tfPhone.getText().trim();
        if (nick.isEmpty()) {
            AlertUtils.showStatus(err, "Nickname không được để trống!", STYLE_ERROR); return false;
        }
        if (!email.isEmpty() && !email.endsWith("@gmail.com")) {
            AlertUtils.showStatus(err, "Email phải có đuôi @gmail.com!", STYLE_ERROR); return false;
        }
        if (!phone.isEmpty() && !phone.matches("\\d{9,11}")) {
            AlertUtils.showStatus(err, "Số điện thoại không hợp lệ!", STYLE_ERROR); return false;
        }
        return true;
    }

    private void setEditMode(boolean editing) {
        isEditing = editing;
        // Fields chỉ chỉnh được khi đang edit
        tfNickname.setEditable(editing);
        tfEmail.setEditable(editing);
        tfPhone.setEditable(editing);
        desc.setEditable(editing);
        // Ảnh chỉ click được khi đang edit
        myImageView.setOnMouseClicked(editing ? this::uploadImage : null);
        myImageView.setStyle(editing ? "-fx-cursor: hand; -fx-opacity: 1.0;"
                : "-fx-cursor: default; -fx-opacity: 0.85;");
        // Nút
        btnEdit.setVisible(!editing);
        btnSave.setVisible(editing);
        btnCancel.setVisible(editing);
        // Style field khi view mode: xám nhạt
        String fieldStyle = editing
                ? "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14;"
                : "-fx-background-color: #F4F5F7; -fx-border-color: transparent; -fx-background-radius: 10; -fx-padding: 12 15;";
        tfNickname.setStyle(fieldStyle);
        tfEmail.setStyle(fieldStyle);
        tfPhone.setStyle(fieldStyle);
    }

    // ── UI update ─────────────────────────────────────────────────────────────

    private void updateUI(User u) {
        nickname.setText(u.getNickname());
        balance.setText(CurrencyFormatter.formatDisplay(u.getBalance()));
        desc.setText(u.getDescription() == null ? "" : u.getDescription());
        tfNickname.setText(u.getNickname() == null ? "" : u.getNickname());
        tfEmail.setText(u.getEmail() == null ? "" : u.getEmail());
        tfPhone.setText(u.getPhone() == null ? "" : u.getPhone());
        loadUserAvatar(u.getAvatarURL());
    }

    public void uploadImage(MouseEvent event) {
        if (!isEditing) return;
        selectedFile = ImageUtils.chooseImageFile(myImageView.getScene().getWindow(), "Chọn ảnh đại diện");
        if (selectedFile != null) setCircularImage(new Image(selectedFile.toURI().toString()));
    }

    public void loadUserAvatar(String pathOrBase64) {
        try {
            if (pathOrBase64 == null || pathOrBase64.isEmpty() || pathOrBase64.equals("null")) {
                loadDefaultAvatar(); return;
            }
            Image image = (!pathOrBase64.startsWith("file:/") && !pathOrBase64.startsWith("http"))
                    ? ImageUtils.base64ToImage(pathOrBase64)
                    : new Image(pathOrBase64, true);
            if (image != null) setCircularImage(image);
            else loadDefaultAvatar();
        } catch (Exception e) {
            loadDefaultAvatar();
        }
    }

    private void loadDefaultAvatar() {
        setCircularImage(new Image(getClass().getResource("/images/avtDefault.jpg").toExternalForm()));
    }

    private void setCircularImage(Image image) {
        myImageView.setImage(image);
        myImageView.setFitWidth(CIRCLE_RADIUS * 2);
        myImageView.setFitHeight(CIRCLE_RADIUS * 2);
        myImageView.setPreserveRatio(false);
        myImageView.setSmooth(true);
        myImageView.setClip(new Circle(CIRCLE_RADIUS, CIRCLE_RADIUS, CIRCLE_RADIUS));
    }

    public void logout(ActionEvent e) {
        if (AlertUtils.showConfirmation("Xác nhận đăng xuất", "Bạn có chắc chắn muốn đăng xuất không?")) {
            SocketHelper.sendRequest("LOGOUT");
            UserSession.getInstance().cleanUserSession();
            changeScene((Node) e.getSource(), "start.fxml");
        }
    }

    // ── Server response ───────────────────────────────────────────────────────

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                String type = SocketHelper.getType(response);
                if (!"GET_PROFILE".equals(type) && !"UPDATE_PROFILE".equals(type)
                        && !"LOGOUT".equals(type)) return;

                String status  = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                if ("SUCCESS".equals(status)) {
                    if ("GET_PROFILE".equals(type)) {
                        JsonObject data = SocketHelper.getPayloadObject(response);
                        if (data != null) {
                            user.setNickname(   data.has("nickname")    ? data.get("nickname").getAsString()    : "");
                            user.setBalance(    data.has("balance")     ? data.get("balance").getAsDouble()     : 0.0);
                            user.setAvatar(     data.has("avatarURL")   ? data.get("avatarURL").getAsString()   : "");
                            user.setDescription(data.has("description") ? data.get("description").getAsString() : "");
                            user.setEmail(      data.has("email")       ? data.get("email").getAsString()       : "");
                            user.setPhone(      data.has("phone")       ? data.get("phone").getAsString()       : "");
                            updateUI(user);
                        }
                    } else if ("UPDATE_PROFILE".equals(type)) {
                        // Cập nhật local session
                        user.setNickname(tfNickname.getText().trim());
                        user.setDescription(desc.getText());
                        user.setEmail(tfEmail.getText().trim());
                        user.setPhone(tfPhone.getText().trim());
                        if (selectedFile != null) user.setAvatar(ImageUtils.fileToBase64(selectedFile));
                        nickname.setText(user.getNickname()); // cập nhật label tên
                        selectedFile = null;
                        setEditMode(false);
                        AlertUtils.showStatus(err, "Cập nhật hồ sơ thành công!", STYLE_SUCCESS);
                    }
                } else {
                    AlertUtils.showStatus(err, "Lỗi: " + message, STYLE_ERROR);
                }
            } catch (Exception e) {
                AlertUtils.showStatus(err, "Lỗi kết nối với server.", STYLE_ERROR);
            }
        });
    }
}