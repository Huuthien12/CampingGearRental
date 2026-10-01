package com.campinggearrental.controller;

import com.campinggearrental.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class LoginController {
	private final AuthService authService;
	private final Runnable onAuthenticated;
	private final TextField usernameField = new TextField();
	private final PasswordField passwordField = new PasswordField();

	public LoginController(AuthService authService, Runnable onAuthenticated) {
		this.authService = authService;
		this.onAuthenticated = onAuthenticated;
	}

	public Parent createView() {
		Label title = new Label("Camping Gear Rental");
		title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
		usernameField.setPromptText("Tên đăng nhập");
		passwordField.setPromptText("Mật khẩu");

		Button loginButton = new Button("Đăng nhập");
		loginButton.setDefaultButton(true);
		loginButton.setMaxWidth(Double.MAX_VALUE);
		loginButton.setOnAction(event -> login());
		passwordField.setOnAction(event -> login());

		VBox content = new VBox(14, title, usernameField, passwordField, loginButton);
		content.setAlignment(Pos.CENTER);
		content.setPadding(new Insets(36));
		content.setMaxWidth(360);
		VBox page = new VBox(content);
		page.setAlignment(Pos.CENTER);
		return page;
	}

	private void login() {
		try {
			if (authService.login(usernameField.getText(), passwordField.getText())) {
				onAuthenticated.run();
			} else {
				showAlert(Alert.AlertType.WARNING, "Đăng nhập thất bại", "Tên đăng nhập hoặc mật khẩu không đúng.");
			}
		} catch (SQLException exception) {
			showAlert(Alert.AlertType.ERROR, "Không thể kết nối cơ sở dữ liệu",
					"Kiểm tra MySQL và cấu hình CAMPING_DB_URL, CAMPING_DB_USERNAME, CAMPING_DB_PASSWORD.");
		}
	}

	private void showAlert(Alert.AlertType type, String title, String message) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}