package br.com.aweb.sistema_vendas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.service.ClienteService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // LISTAR
    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "clientes",
                clienteService.listarTodos());

        return "cliente/list";
    }

    // ABRIR FORMULÁRIO
    @GetMapping("/novo")
    public String novo(Model model) {

        model.addAttribute(
                "cliente",
                new Cliente());

        return "cliente/form";
    }

    // SALVAR
    @PostMapping
    public String salvar(
            @Valid @ModelAttribute Cliente cliente,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            return "cliente/form";
        }

        try {

            clienteService.salvar(cliente);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "erro",
                    e.getMessage());

            return "cliente/form";
        }

        return "redirect:/clientes";
    }

    // EDITAR
    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            Model model) {

        Cliente cliente = clienteService.buscarPorId(id);

        model.addAttribute(
                "cliente",
                cliente);

        return "cliente/form";
    }

    // CONFIRMAÇÃO DE EXCLUSÃO
    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(
            @PathVariable Long id,
            Model model) {

        Cliente cliente = clienteService.buscarPorId(id);

        model.addAttribute(
                "cliente",
                cliente);

        return "cliente/delete";
    }

    // EXCLUIR
    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable Long id) {

        clienteService.excluir(id);

        return "redirect:/clientes";
    }
}