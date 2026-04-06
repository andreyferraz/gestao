package com.andreyferraz.gestao.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping({"/", "/home", "/dashboard"})
	public String home(Authentication authentication, Model model) {
		String username = authentication != null ? authentication.getName() : "Visitante";
		model.addAttribute("username", username);
		return "home/dashboard";
	}

}
