package com.andreyferraz.gestao.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

	@GetMapping("/login")
	public String login(
			@RequestParam(required = false) String error,
			@RequestParam(required = false) String logout,
			Model model) {
		if (error != null) {
			model.addAttribute("errorMessage", "Usuario ou senha invalidos.");
		}
		if (logout != null) {
			model.addAttribute("successMessage", "Logout realizado com sucesso.");
		}
		return "auth/login";
	}

}
