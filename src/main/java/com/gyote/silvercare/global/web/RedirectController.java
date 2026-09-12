package com.gyote.silvercare.global.web;

import com.gyote.silvercare.global.config.FrontendOrigin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class RedirectController {

    private final FrontendOrigin frontend;

    public RedirectController(FrontendOrigin frontend) {
        this.frontend = frontend;
    }

    @GetMapping({"/", "/login", "/home", "/role", "/terms", "/privacy"})
    public void toFrontend(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI();
        if (path == null || "/".equals(path)) {
            path = "/";
        }
        String query = request.getQueryString();
        response.sendRedirect(frontend.path(path) + (query == null || query.isBlank() ? "" : "?" + query));
    }
}
