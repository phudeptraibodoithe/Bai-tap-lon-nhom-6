package com.tboat.controllers;

import com.google.gson.JsonObject;
import com.tboat.models.User;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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

    private static final double CIRCLE_RADIUS = 110.0;
    private static final Logger log = Logger.getLogger(ControllerProfile.class.getName());

    @FXML private ImageView myImageView;
    @FXML private Label nickname, balance, err;
    @FXML private TextArea desc;

    private static final String STYLE_SUCCESS    = "#2ecc71";
    private static final String STYLE_ERROR      = "#e74c3c";
    private static final String STYLE_PROCESSING = "#3498db";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (err != null) err.setText("");

        String screenKey = BaseController.toScreenKey("profile.fxml");
        String cached    = DataCache.getInstance().get("PROFILE");

        user = UserSession.getInstance().getUser();
        if (user != null) {
            updateUI(user.getNickname(), user.getBalance(), user.getAvatarURL(), user.getDescription());
        }

        if (cached != null) {
            log.info("[Profile] Cache HIT → dùng cache + refresh ngầm");
            NavigationContext.getInstance().reportCacheHit(screenKey, true);
            handleServerResponse(cached);
        } else {
            NavigationContext.getInstance().reportCacheHit(screenKey, false);
        }
        SocketHelper.sendRequest("PROFILE");
    }

    private void updateUI(String nick, double bal, String avt, String description) {
        nickname.setText(nick);
        balance.setText(CurrencyFormatter.formatDisplay(bal));
        desc.setText(description == null ? "" : description);
        loadUserAvatar(avt);
    }

    public void updateProfile(ActionEvent e) {
        String mota      = desc.getText() == null ? "" : desc.getText().trim();
        String imageData = (selectedFile != null)
                ? ImageUtils.fileToBase64(selectedFile)
                : (user.getAvatarURL() != null && !user.getAvatarURL().isEmpty() ? user.getAvatarURL() : "null");

        AlertUtils.showStatus(err, "Đang xử lý, vui lòng đợi...", STYLE_PROCESSING);

        JsonObject payload = new JsonObject();
        payload.addProperty("description", mota);
        payload.addProperty("avatarURL", imageData);
        SocketHelper.sendRequest("UPDATE_PROFILE", payload);
    }

    public void loadUserAvatar(String pathOrBase64) {
        try {
            if (pathOrBase64 == null || pathOrBase64.isEmpty() || pathOrBase64.equals("null")) {
                loadDefaultAvatar();
                return;
            }
            Image image;
            if (!pathOrBase64.startsWith("file:/") && !pathOrBase64.startsWith("http")) {
                image = ImageUtils.base64ToImage(pathOrBase64);
            } else {
                image = new Image(pathOrBase64, true);
            }
            if (image != null) setCircularImage(image);
            else loadDefaultAvatar();
        } catch (Exception e) {
            log.warning("[Avatar Error]: " + e.getMessage());
            loadDefaultAvatar();
        }
    }

    private void loadDefaultAvatar() {
        String defaultPath = getClass().getResource("/images/avtDefault.jpg").toExternalForm();
        setCircularImage(new Image(defaultPath));
    }

    public void uploadImage(MouseEvent event) {
        selectedFile = ImageUtils.chooseImageFile(myImageView.getScene().getWindow(), "Chọn ảnh đại diện");
        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            setCircularImage(image);
        }
    }

    public void logout(ActionEvent e) {
        if (AlertUtils.showConfirmation("Xác nhận đăng xuất", "Bạn có chắc chắn muốn đăng xuất không?")) {
            SocketHelper.sendRequest("LOGOUT");
            UserSession.getInstance().cleanUserSession();
            changeScene((Node) e.getSource(), "start.fxml");
        }
    }

    public void canclePost(ActionEvent e) {
        if (AlertUtils.showConfirmation("Xác nhận hủy thay đổi", "Toàn bộ thông tin bạn vừa nhập sẽ không được lưu lại.\nBạn có chắc chắn muốn hủy thay đổi không?")) {
            changeScene((Node) e.getSource(), "profile.fxml");
        }
    }

    private void setCircularImage(Image image) {
        myImageView.setImage(image);
        myImageView.setFitWidth(CIRCLE_RADIUS * 2);
        myImageView.setFitHeight(CIRCLE_RADIUS * 2);
        myImageView.setPreserveRatio(false);
        myImageView.setSmooth(true);
        Circle clipCircle = new Circle(CIRCLE_RADIUS, CIRCLE_RADIUS, CIRCLE_RADIUS);
        myImageView.setClip(clipCircle);
    }

    @Override
    public void handleServerResponse(String response) {
        Platform.runLater(() -> {
            try {
                String type = SocketHelper.getType(response);
                if (!"GET_PROFILE".equals(type) && !"UPDATE_PROFILE".equals(type) && !"LOGOUT".equals(type)) return;

                String status  = SocketHelper.getStatus(response);
                String message = SocketHelper.getMessage(response);

                if ("SUCCESS".equals(status)) {
                    if ("GET_PROFILE".equals(type)) {
                        JsonObject data = SocketHelper.getPayloadObject(response);
                        if (data != null) {
                            String nick        = data.has("nickname")    ? data.get("nickname").getAsString()    : "";
                            double bal         = data.has("balance")     ? data.get("balance").getAsDouble()     : 0.0;
                            String avt         = data.has("avatarURL")   ? data.get("avatarURL").getAsString()   : "";
                            String description = data.has("description") ? data.get("description").getAsString() : "";

                            updateUI(nick, bal, avt, description);
                            user.setNickname(nick);
                            user.setBalance(bal);
                            user.setAvatar(avt);
                            user.setDescription(description);
                        }
                    } else if ("UPDATE_PROFILE".equals(type)) {
                        user.setDescription(desc.getText());
                        if (selectedFile != null) user.setAvatar(ImageUtils.fileToBase64(selectedFile));
                        AlertUtils.showStatus(err, "Cập nhật hồ sơ thành công!!", STYLE_SUCCESS);
                    } else if ("LOGOUT".equals(type)) {
                        log.info("Đăng xuất hoàn tất.");
                    }
                } else if ("ERROR".equals(status) || "FAILED".equals(status)) {
                    AlertUtils.showStatus(err, "Lỗi: " + message, STYLE_ERROR);
                }
            } catch (Exception e) {
                AlertUtils.showStatus(err, "Lỗi kết nối với server.", STYLE_ERROR);
                log.severe("KHÔNG THỂ ĐỌC JSON TỪ SERVER: " + response);
            }
        });
    }
}