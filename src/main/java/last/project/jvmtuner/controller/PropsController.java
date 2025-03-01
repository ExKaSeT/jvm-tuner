package last.project.jvmtuner.controller;

import jakarta.validation.Valid;
import last.project.jvmtuner.dto.tuning_test.CreateTestPropsRequestDto;
import last.project.jvmtuner.exception.DeploymentParsingException;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.service.tuning_test.TuningTestPropsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

import static java.util.Objects.isNull;

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

    @GetMapping("/{propId}")
    public String getProp(@PathVariable Long propId, Model model) {
        var propDetails = propsService.getPropsWithModes(propId);
        model.addAttribute("prop", propDetails);
        return "prop-details";
    }

    @GetMapping("/create")
    public String getCreateProp(Model model) {
        model.addAttribute("testProps", new CreateTestPropsRequestDto());
        return "prop-create";
    }

    @PostMapping("/create")
    public String createTestProps(@Valid @ModelAttribute("testProps") CreateTestPropsRequestDto propsDto,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "prop-create";
        }

        TuningTestProps props;
        try {
            props = propsService.saveTuningTestProps(
                    propsDto.getDeployment(),
                    propsDto.getAppContainerName(),
                    propsDto.getAppMetricPortWithPath(),
                    propsDto.getGatlingImage(),
                    propsDto.getGatlingExecCommand(),
                    propsDto.getStartTestTimeoutSec(),
                    propsDto.getTestDurationSec(),
                    isNull(propsDto.getMetricMaxValues()) ?
                            Collections.emptyList() : propsDto.getMetricMaxValues(),
                    propsDto.getDescription()
            );
        } catch (DeploymentParsingException ex) {
            bindingResult.rejectValue("deployment", "error.deployment", ex.getMessage());
            return "prop-create";
        }

        return "redirect:/properties/" + props.getId();
    }
}
