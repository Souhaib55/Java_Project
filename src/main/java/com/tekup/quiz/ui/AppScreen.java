package com.tekup.quiz.ui;

import javafx.scene.Parent;

public interface AppScreen {
    String title();

    Parent root();

    default void onHide() {
    }

    default void onShow() {
    }
}
