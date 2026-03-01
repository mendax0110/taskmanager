package com.taskmanager.ui;

import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.*;
import com.taskmanager.pattern.AuditLogger;
import com.taskmanager.service.TaskService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JAVAFX controler class
 */
public class MainController
{
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;

    @FXML private RadioButton filterAll;
    @FXML private RadioButton filterPending;
    @FXML private RadioButton filterOverdue;
    @FXML private RadioButton filterDone;
    @FXML private ComboBox<String> categoryFilter;

    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> colStatus;
    @FXML private TableColumn<Task, Integer> colId;
    @FXML private TableColumn<Task, String> colTitle;
    @FXML private TableColumn<Task, String> colCategory;
    @FXML private TableColumn<Task, String> colPriority;
    @FXML private TableColumn<Task, String> colDueDate;
    @FXML private TableColumn<Task, String> colProject;
    
    @FXML private Label detailTitle;
    @FXML private Label detailDesc;
    @FXML private Label detailMeta;
    
    @FXML private Button btnComplete;
    @FXML private Button btnDelete;
    
    @FXML private TextField inputTitle;
    @FXML private TextField inputDescription;
    @FXML private TextField inputDueDate;
    @FXML private ComboBox<String> inputType;
    @FXML private TextField inputProject;
    @FXML private ComboBox<String> inputUrgency;
    @FXML private ComboBox<Integer> inputEffort;

    private TaskService service;
    private AuditLogger auditLogger;

    private final ObservableList<Task> tableData = FXCollections.observableArrayList();

    public void initialize(TaskService service, AuditLogger auditLogger)
    {
        this.service = service;
        this.auditLogger = auditLogger;

        setupTable();
        setupFilters();
        setupForm();
        setupSelectionListener();
        seedIfEmpty();
        refresh();
    }

    private void setupTable()
    {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));

        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isCompleted() ? "Y" : data.getValue().isOverdue() ? "!" : "o"));
        colDueDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDueDate() != null ? data.getValue().getDueDate().toString() : "-"));
        colProject.setCellValueFactory(data ->{
            Task t = data.getValue();
            return new SimpleStringProperty(t instanceof WorkTask wt ? wt.getProject() : "-");
        });

        taskTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Task task, boolean empty)
            {
                super.updateItem(task, empty);
                getStyleClass().removeAll("row-done", "row-overdue", "row-critical");
                if (task != null && !empty)
                {
                    if (task.isCompleted())
                    {
                        getStyleClass().add("row-done");
                    }
                    else if (task.isOverdue())
                    {
                        getStyleClass().add("row-overdue");
                    }
                    else if ("CRITICAL".equals(task.getPriority()))
                    {
                        getStyleClass().add("row-critical");
                    }
                }
            }
        });

        taskTable.setItems(tableData);
    }

    private void setupFilters()
    {
        filterAll.setOnAction(e -> refresh());
        filterPending.setOnAction(e -> refresh());
        filterOverdue.setOnAction(e -> refresh());
        filterDone.setOnAction(e -> refresh());
        categoryFilter.setOnAction(e -> refresh());
    }

    private void setupForm()
    {
        inputType.setItems(FXCollections.observableArrayList("Work", "Personal"));
        inputUrgency.setItems(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        inputEffort.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));

        inputType.setOnAction(e -> {
            boolean isWork = "Work".equals(inputType.getValue());
            inputProject.setVisible(isWork);
            inputProject.setManaged(isWork);
            inputUrgency.setVisible(isWork);
            inputUrgency.setManaged(isWork);
            inputEffort.setVisible(!isWork);
            inputEffort.setManaged(!isWork);
        });

        inputType.setValue("Work");
        inputEffort.setVisible(false);
        inputEffort.setManaged(false);
    }

    private void setupSelectionListener()
    {
        taskTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateDetailPanel(newVal));
    }

    private void seedIfEmpty()
    {
        if (!service.getAllTasks().isEmpty()) return;
        service.addTask(new WorkTask(service.nextId(), "Design database schema",
                "ERD for the new project", LocalDate.now().plusDays(3), "Backend", WorkTask.Urgency.HIGH));
        service.addTask(new WorkTask(service.nextId(), "Fix login bug",
                "Users report session timeout", LocalDate.now().minusDays(1), "Frontend", WorkTask.Urgency.CRITICAL));
        service.addTask(new PersonalTask(service.nextId(), "Buy groceries",
                "Milk, eggs, bread", LocalDate.now().plusDays(1), 1));
        service.addTask(new PersonalTask(service.nextId(), "Read Java book",
                "Chapter 8: Generics", LocalDate.now().plusDays(7), 3));
        service.addTask(new WorkTask(service.nextId(), "Write unit tests",
                "Cover service layer", LocalDate.now().plusDays(5), "Backend", WorkTask.Urgency.MEDIUM));
    }

    private void refresh()
    {
        List<Task> tasks;

        if (filterPending.isSelected())
        {
            tasks = service.getPendingTasks();
        }
        else if (filterOverdue.isSelected())
        {
            tasks = service.getOverdueTasks();
        }
        else if (filterDone.isSelected())
        {
            tasks = service.getAllTasks().stream().filter(Task::isCompleted).collect(Collectors.toList());
        }
        else
        {
            tasks = service.getAllTasks();
        }

        String cat = categoryFilter.getValue();
        if (cat != null && !cat.isEmpty())
        {
            tasks = tasks.stream().filter(t -> t.getCategory().equals(cat)).collect(Collectors.toList());
        }

        tableData.setAll(tasks);

        List<String> categories = service.getAllTasks().stream().map(Task::getCategory).distinct().sorted().toList();
        String currentCat = categoryFilter.getValue();

        if (!new java.util.HashSet<>(categoryFilter.getItems()).equals(new java.util.HashSet<>(categories)))
        {
            categoryFilter.getItems().setAll(categories);
        }
        if (currentCat != null && !currentCat.isEmpty() && categories.contains(currentCat))
        {
            categoryFilter.setValue(currentCat);
        }

        summaryLabel.setText(service.getSummary());
        status("Showing " + tasks.size() + " task(s)");
    }

    private void updateDetailPanel(Task task)
    {
        if (task == null)
        {
            detailTitle.setText("-");
            detailDesc.setText("");
            detailMeta.setText("");
            btnComplete.setDisable(true);
            btnDelete.setDisable(true);
            return;
        }

        detailTitle.setText(task.getTitle());
        detailDesc.setText(task.getDescription());

        String meta = "Priority: " + task.getPriority() + "\n" +
                "Due: " + (task.getDueDate() != null ? task.getDueDate() : "No date") + "\n" +
                "Status: " + (task.isCompleted() ? "Done" : task.isOverdue() ? "OVERDUE" : "Pending");

        if (task instanceof WorkTask wt)
        {
            meta += "\nProject: " + wt.getProject();
        }

        if (task instanceof PersonalTask pt)
        {
            meta += "\nEffort: " + pt.getEffortLevel() +  "/5";
        }

        detailMeta.setText(meta);
        btnComplete.setDisable(task.isCompleted());
        btnDelete.setDisable(false);
    }

    @FXML
    private void onComplete()
    {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null)
        {
            status("Select a task first.");
            return;
        }

        try
        {
            service.completeTask(selected.getId());
            refresh();
            status("Task #" + selected.getId() + " marked complete.");
        }
        catch (TaskNotFoundException ex)
        {
            showAlert("Error", ex.getMessage());
        }
    }

    @FXML
    private void onDelete()
    {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null)
        {
            status("Select a task first.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete \"" + selected.getTitle() + "\"?",
                            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES)
            {
                try
                {
                    service.deleteTask(selected.getId());
                    updateDetailPanel(null);
                    refresh();
                    status("Task #" + selected.getId() + " deleted.");
                }
                catch (TaskNotFoundException ex)
                {
                    showAlert("Error", ex.getMessage());
                }
            }
        });
    }

    @FXML
    private void onAddTask()
    {
        String title = inputTitle.getText().trim();
        if (title.isEmpty())
        {
            status("Title is required.");
            return;
        }

        String desc = inputDescription.getText().trim();
        String type = inputType.getValue();
        LocalDate due = parseDate(inputDueDate.getText());

        try
        {
            Task task = null;
            if ("Work".equals(type))
            {
                String project = inputProject.getText().trim();
                String urgencyStr = inputUrgency.getValue();
                if (urgencyStr == null)
                {
                    status("Select urgency.");
                    return;
                }
                WorkTask.Urgency urgency = WorkTask.Urgency.valueOf(urgencyStr);
                task = new WorkTask(service.nextId(), title, desc, due, project.isEmpty() ? "General" : project, urgency);
            }
            else if ("Personal".equals(type))
            {
                Integer effort = inputEffort.getValue();
                if (effort == null)
                {
                    status("Select effort level.");
                    return;
                }
                task = new PersonalTask(service.nextId(), title, desc, due, effort);
            }
            else
            {
                status("Select a task type.");
                return;
            }

            service.addTask(task);
            clearForm();
            refresh();
            taskTable.getSelectionModel().select(task);
            status("Added: " + title);
        }
        catch (IllegalArgumentException ex)
        {
            showAlert("Invalid Input", ex.getMessage());
        }
    }

    @FXML
    private void onShowAuditLog()
    {
        StringBuilder sb = new StringBuilder();
        auditLogger.getLog().forEach(entry -> sb.append(entry).append("\n"));

        TextArea area = new TextArea(sb.isEmpty() ? "No events logged yet." : sb.toString());
        area.setEditable(false);
        area.setPrefSize(500, 300);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Audit Log");
        dialog.setHeaderText("All task events this session");
        dialog.getDialogPane().setContent(area);
        dialog.showAndWait();
    }

    private void clearForm()
    {
        inputTitle.clear();
        inputDescription.clear();
        inputDueDate.clear();
        inputProject.clear();
        inputUrgency.setValue(null);
        inputEffort.setValue(null);
    }

    private void status(String msg)
    {
        statusLabel.setText(msg);
    }

    private LocalDate parseDate(String s)
    {
        if (s == null || s.isBlank())
        {
            return null;
        }

        try
        {
            return LocalDate.parse(s.trim());
        }
        catch (DateTimeParseException ex)
        {
            return null;
        }
    }

    private void showAlert(String title, String msg)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}