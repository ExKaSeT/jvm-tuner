package last.project.jvmtuner.controller;

import last.project.jvmtuner.dto.tuning_task.CreateTaskResponseDto;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.service.tuning_task.TuningTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    @ResponseBody
    public void deleteTask(@PathVariable Long taskId) {
        taskService.delete(taskId);
    }

    @GetMapping("/{taskId}")
    public String getTask(@PathVariable Long taskId, Model model) {
        var taskDetails = taskService.getDetails(taskId);
        model.addAttribute("task", taskDetails);
        return "task-details";
    }

    @PostMapping
    @ResponseBody
    public CreateTaskResponseDto createTask(@RequestParam Long propId, @RequestParam TuningMode mode) {
        var task = taskService.startTask(propId, mode);
        return new CreateTaskResponseDto().setId(task.getId());
    }
}
