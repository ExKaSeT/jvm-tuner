package last.project.jvmtuner.controller;

import last.project.jvmtuner.service.tuning_test.TuningTestPropsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropsController {

    private final TuningTestPropsService propsService;

    @GetMapping
    public String showProps(Model model) {
        var props = propsService.getAllProps();
        model.addAttribute("props", props);
        return "props-list";
    }

    @DeleteMapping("/{propId}")
    @ResponseBody
    public void deleteTask(@PathVariable Long propId) {
        propsService.delete(propId);
    }
}
