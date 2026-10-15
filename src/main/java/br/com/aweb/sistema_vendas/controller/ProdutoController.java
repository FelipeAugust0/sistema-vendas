package br.com.aweb.sistema_vendas.controller;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.service.ProdutoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    // Listar produtos
    @GetMapping
    public ModelAndView list() {
        return new ModelAndView("produto/list", Map.of("produtos", produtoService.listarTodos()));
    }

    // Formulário de cadastro
    @GetMapping("/novo")
    public ModelAndView create() {
        return new ModelAndView("produto/form", Map.of("produto", new Produto()));
    }

    // Salvar produto
    @PostMapping("/novo")
    public String create(@Valid Produto produto, BindingResult result) {
        if (result.hasErrors()) {
            return "produto/form";
        }
        produtoService.salvar(produto);
        return "redirect:/produtos";
    }

    // Formulário de edição
    @GetMapping("/edit/{id}")
    public ModelAndView edit(@PathVariable Long id) {
        var optionalProduto = produtoService.buscarPorId(id);
        if (optionalProduto.isPresent()) {
            return new ModelAndView("produto/form", Map.of("produto", optionalProduto.get()));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    // Atualizar produto
    @PostMapping("/edit/{id}")
    public String edit(
            @PathVariable Long id,
            @Valid Produto produto,
            BindingResult result) {

        if (produtoService.buscarPorId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // O ID da URL determina qual produto será alterado.
        produto.setId(id);

        if (result.hasErrors()) {
            return "produto/form";
        }

        produtoService.atualizar(id, produto);
        return "redirect:/produtos";
    }

    // Excluir produto
    @GetMapping("/delete/{id}")
    public ModelAndView delete(@PathVariable Long id) {
        var optionalProduto = produtoService.buscarPorId(id);
        if (optionalProduto.isPresent()) {
            return new ModelAndView("produto/delete", Map.of("produto", optionalProduto.get()));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        if (produtoService.buscarPorId(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try {
            produtoService.excluir(id);
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute(
                    "erro",
                    "Não é possível excluir este produto porque ele está vinculado a pedidos.");
        }

        return "redirect:/produtos";
    }

}
