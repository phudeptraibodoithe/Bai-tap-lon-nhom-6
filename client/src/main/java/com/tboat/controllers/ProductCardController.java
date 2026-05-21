package com.tboat.controllers;

import com.tboat.models.auction.AuctionSession;
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

    private AuctionSession session;
    private static final Logger logger = Logger.getLogger(ProductCardController.class.getName());

    // Nạp dữ liệu vào thẻ
    public void setData(AuctionSession session) {
        this.session = session;
        lblName.setText(session.getName());
        lblCurrentBid.setText(CurrencyFormatter.formatDisplay(session.getCurrentPrice()));
        lblStatus.setText(session.getStatusOfAuction() != null ? session.getStatusOfAuction().name() : "ONGOING");

        // Xử lý ảnh và bo tròn góc
        String base64Data = session.getImageURL();
        if (base64Data != null && !base64Data.isEmpty()) {
            Image img = ImageUtils.base64ToImage(base64Data);
            if (img != null) {
                itemImage.setImage(img);
                Rectangle clip = new Rectangle(300, 240);
                clip.setArcWidth(40);
                clip.setArcHeight(40);
                itemImage.setClip(clip);
            }
        }
    }

    // Xử lý khi ấn nút PLACE BID
    @FXML
    public void handlePlaceBid(ActionEvent event) {
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