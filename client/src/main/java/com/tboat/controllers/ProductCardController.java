package com.tboat.controllers;

import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.utilsclient.CurrencyFormatter;
import com.tboat.utilsclient.ImageUtils;
import com.tboat.utilsclient.TimeUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.util.logging.Logger;

public class ProductCardController extends BaseController {

    @FXML private ImageView itemImage;
    @FXML private Label lblName;
    @FXML private Label lblCurrentBid;
    @FXML private Label lblStatus;
    @FXML private Button btnBid;
    @FXML private Button btnFavorite;

    private AuctionSession session;
    private static final Logger logger = Logger.getLogger(ProductCardController.class.getName());
    private static final double IMAGE_CLIP_WIDTH = 300;
    private static final double IMAGE_CLIP_HEIGHT = 240;
    private static final double IMAGE_CORNER_ARC = 40;

    // Nạp dữ liệu vào thẻ
    public void setData(AuctionSession session) {
        this.session = session;
        lblName.setText(session.getName());
        lblCurrentBid.setText(CurrencyFormatter.formatDisplay(session.getCurrentPrice()));
        lblStatus.setText(session.getStatusOfAuction() != null
                ? translateStatus(session.getStatusOfAuction().name()) : "ĐANG DIỄN RA");
        updateActionState(session.getStatusOfAuction());
        // Xử lý ảnh và bo tròn góc
        String base64Data = session.getImageURL();
        if (base64Data != null && !base64Data.isEmpty()) {
            Image img = ImageUtils.base64ToImage(base64Data);
            if (img != null) {
                itemImage.setImage(img);
                Rectangle clip = new Rectangle(IMAGE_CLIP_WIDTH, IMAGE_CLIP_HEIGHT);
                clip.setArcWidth(IMAGE_CORNER_ARC);
                clip.setArcHeight(IMAGE_CORNER_ARC);
                itemImage.setClip(clip);
            }
        }
    }
    private String translateStatus(String status) {
        return switch (status) {
            case "ONGOING"     -> "Đang diễn ra";
            case "ENDED"       -> "Đã kết thúc";
            case "CANCELED"    -> "Đã hủy";
            case "NOT_STARTED" -> "Chưa bắt đầu";
            default            -> status;
        };
    }

    private void updateActionState(StatusOfAuction status) {
        boolean canceled = status == StatusOfAuction.CANCELED;
        if (btnBid != null) {
            btnBid.setDisable(canceled);
            btnBid.setText(status == StatusOfAuction.ENDED ? "XEM CHI TIẾT"
                    : canceled ? "ĐÃ HỦY" : "ĐẶT GIÁ NGAY");
        }
        if (btnFavorite != null) {
            btnFavorite.setVisible(status != StatusOfAuction.ENDED && !canceled);
            btnFavorite.setManaged(status != StatusOfAuction.ENDED && !canceled);
        }
    }

    // Xử lý khi ấn nút PLACE BID
    @FXML
    public void handlePlaceBid(ActionEvent event) {
        if (session == null || session.getStatusOfAuction() == StatusOfAuction.CANCELED) {
            return;
        }
        try {
            AuctionController controller = changeSceneAndGetController(btnBid, "auction.fxml");
            if (controller != null) {
                controller.setItemData(session);
            }
        } catch (Exception e) {
            logger.severe("Lỗi xử lý khi lấy dữ liệu từ server: " + e.getMessage());
        }
    }
}
