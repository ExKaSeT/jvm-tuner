package last.project.jvmtuner.controller;

import last.project.jvmtuner.service.tuning_task.TuningTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TuningTaskService taskService;

    @GetMapping
    public String showTasks(Model model) {
        var tasks = taskService.getAllTasks();
        model.addAttribute("tasks", tasks);
        return "task-list";
    }

    @DeleteMapping("/{taskId}")
    public String deleteTask(Model model, @PathVariable Long taskId) {
        taskService.delete(taskId);
        return showTasks(model);
    }
}
