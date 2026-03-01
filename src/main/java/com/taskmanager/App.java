package com.taskmanager;

import com.taskmanager.pattern.AuditLogger;
import com.taskmanager.repository.FileTaskRepository;
import com.taskmanager.repository.InMemoryTaskRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.service.TaskService;
import com.taskmanager.ui.MainController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Entry point for UI application
 */
public class App extends Application
{
    private static TaskService taskService;
    private static AuditLogger auditLogger;

    @Override
    public void start(Stage stage) throws Exception
    {
        TaskRepository repository = new FileTaskRepository("tasks.csv");

        InMemoryTaskRepository inMemoryTaskRepository = new InMemoryTaskRepository();

        taskService = new TaskService(inMemoryTaskRepository);
        taskService = new TaskService(repository);
        auditLogger = new AuditLogger();
        taskService.addObserver(auditLogger);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskmanager/ui/main.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/taskmanager/ui/style.css")).toExternalForm());

        MainController controller = loader.getController();
        controller.initialize(taskService, auditLogger);

        stage.setTitle("Task Manager");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}