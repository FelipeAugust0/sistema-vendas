package br.com.aweb.sistema_vendas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/pedidos";
    }

    @GetMapping("/login")
    public String exibirLogin() {
        return "login";
    }

    @GetMapping("/logout")
    public String exibirLogout() {
        return "logout";
    }
}
