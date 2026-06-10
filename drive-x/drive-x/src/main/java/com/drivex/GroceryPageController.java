package com.drivex;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class GroceryPageController {

    @GetMapping("/grocery")
    public RedirectView groceryPage() {
        return new RedirectView("/grocery.html");
    }
}
