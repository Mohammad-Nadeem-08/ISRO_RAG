package com.spacedoc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

@Controller
public class PageController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "error", required = false) String error,
                            Model model) {
        if (logout != null) {
            model.addAttribute("loggedOut", true);
        }
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("contentPage", "dashboard-content.jsp");
        return "layout";
    }

    @GetMapping("/research-assistant")
    public String researchAssistantPage(Model model) {
        model.addAttribute("activeTab", "assistant");
        model.addAttribute("contentPage", "research-assistant-content.jsp");
        return "layout";
    }

    @GetMapping("/document-library")
    public String documentLibraryPage(Model model) {
        model.addAttribute("activeTab", "library");
        model.addAttribute("contentPage", "document-library-content.jsp");
        return "layout";
    }

    @GetMapping("/ingestion-centre")
    public String ingestionCentrePage(Model model) {
        model.addAttribute("activeTab", "ingestion");
        model.addAttribute("contentPage", "ingestion-centre-content.jsp");
        return "layout";
    }

    @GetMapping("/document-detail")
    public String documentDetailPage(Model model) {
        model.addAttribute("activeTab", "library");
        model.addAttribute("contentPage", "document-detail-content.jsp");
        return "layout";
    }
}
