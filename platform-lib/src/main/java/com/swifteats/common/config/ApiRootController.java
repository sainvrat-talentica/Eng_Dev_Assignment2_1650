package com.swifteats.common.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@ServiceScope(ServiceName.BACKEND)
public class ApiRootController {

    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView("/swagger-ui.html");
    }
}
