package com.tekup.quiz.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ScreenManager {
    private static final double DEFAULT_WIDTH = 920;
    private static final double DEFAULT_HEIGHT = 620;

    private final Stage stage;
    private final SessionContext sessionContext;
    private final Map<String, AppScreen> screens = new HashMap<>();
    private Scene scene;
    private AppScreen currentScreen;

    public ScreenManager(Stage stage) {
        this(stage, null);
    }

    public ScreenManager(Stage stage, SessionContext sessionContext) {
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
        this.sessionContext = sessionContext;
    }

    public void register(String route, AppScreen screen) {
        screens.put(route, screen);
    }

    public void show(String route) {
        String resolvedRoute = resolveRoute(route);
        AppScreen screen = screens.get(resolvedRoute);
        if (screen == null) {
            throw new IllegalArgumentException("No screen registered for route: " + resolvedRoute);
        }

        if (currentScreen != null) {
            currentScreen.onHide();
        }

        if (scene == null) {
            scene = new Scene(screen.root(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
            URL stylesheetUrl = ScreenManager.class.getResource("/ui/modern-theme.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            }
            stage.setScene(scene);
        } else {
            scene.setRoot(screen.root());
        }

        stage.setTitle(screen.title());
        if (sessionContext != null && sessionContext.getCurrentUser() != null && !AppRoute.AUTH.equals(resolvedRoute)) {
            sessionContext.markActivity();
        }
        screen.onShow();
        currentScreen = screen;
        stage.show();
    }

    private String resolveRoute(String route) {
        if (sessionContext == null || AppRoute.AUTH.equals(route)) {
            return route;
        }

        if (sessionContext.getCurrentUser() == null) {
            return AppRoute.AUTH;
        }

        if (sessionContext.isSessionExpired()) {
            sessionContext.expireSession();
            return AppRoute.AUTH;
        }

        return route;
    }
}
