package com.tboat.controllers;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

public abstract class BaseController {

    private Scene scene;
    private Parent root;

    public void changeScene(Node node, String fxmlFileName) {
        try {
            root = FXMLLoader.load(getClass().getResource("/views/" + fxmlFileName));
            scene = node.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/styles/Button.css").toExternalForm());
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void switchToMenu(Event event) {
        changeScene((Node) event.getSource(), "TrangChu.fxml");
    }

    @FXML public void switchToHistory(ActionEvent event) {
        changeScene((Node) event.getSource(), "history.fxml");
    }

    @FXML public void switchToPostItem(ActionEvent event) {
        changeScene((Node) event.getSource(), "postItem.fxml");
    }

    @FXML public void switchToWallet(ActionEvent event) {
        changeScene((Node) event.getSource(), "NapRut.fxml");
    }

    @FXML public void switchToProfile(ActionEvent event) {
        changeScene((Node) event.getSource(), "profile.fxml");
    }

    @FXML public void switchToLogin(ActionEvent event) throws IOException {
        changeScene((Node) event.getSource(),"login.fxml");
    }

    @FXML public void switchToRegister(ActionEvent event) throws IOException {
        changeScene((Node) event.getSource(),"register.fxml");
    }

    @FXML public void switchToStart(MouseEvent event) throws IOException {
        changeScene((Node) event.getSource(),"start.fxml");
    }
}