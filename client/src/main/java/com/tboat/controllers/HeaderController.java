package com.tboat.controllers;

import com.tboat.utilsclient.HeaderUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

public class HeaderController extends BaseController implements Initializable {

    @FXML private Label lblGreeting;
    @FXML private ImageView userAvatar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
    }
    @Override
    public void onReload() {
        HeaderUtils.setupHeader(lblGreeting, userAvatar, this);
    }
}