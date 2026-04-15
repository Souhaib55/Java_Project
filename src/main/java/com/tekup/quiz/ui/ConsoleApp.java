package com.tekup.quiz.ui;

import com.tekup.quiz.dao.UserDao;
import com.tekup.quiz.dao.jdbc.JdbcUserDao;
import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import com.tekup.quiz.service.AuthService;

import java.util.Scanner;

public class ConsoleApp {
    private final AuthService authService;

    public ConsoleApp(AuthService authService) {
        this.authService = authService;
    }

    public static void main(String[] args) {
        UserDao userDao = new JdbcUserDao();
        AuthService authService = new AuthService(userDao);
        ConsoleApp app = new ConsoleApp(authService);
        app.run();
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("=== Interactive Quiz Console (Bootstrap) ===");
            System.out.println("1) Register");
            System.out.println("2) Login");
            System.out.print("Select: ");
            String choice = scanner.nextLine().trim();

            if ("1".equals(choice)) {
                registerFlow(scanner);
            } else if ("2".equals(choice)) {
                loginFlow(scanner);
            } else {
                System.out.println("Unknown option");
            }
        }
    }

    private void registerFlow(Scanner scanner) {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        User created = authService.register(username, password, Role.PLAYER);
        System.out.println("Registered with id: " + created.getId());
    }

    private void loginFlow(Scanner scanner) {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        User user = authService.login(username, password);
        System.out.println("Welcome " + user.getUsername() + " (" + user.getRole() + ")");
    }
}
