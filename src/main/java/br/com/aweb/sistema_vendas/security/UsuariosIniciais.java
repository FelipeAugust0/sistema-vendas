package br.com.aweb.sistema_vendas.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import br.com.aweb.sistema_vendas.service.UsuarioService;

@Component
public class UsuariosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${PADRAO_EMAIL:}")
    private String padraoEmail;

    @Value("${PADRAO_PASSWORD:}")
    private String padraoPassword;

    public UsuariosIniciais(
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(String... args) {
        criarSeConfigurado("Administrador", adminEmail, adminPassword, UsuarioRole.ADMIN);
        criarSeConfigurado("Usuário Padrão", padraoEmail, padraoPassword, UsuarioRole.PADRAO);
    }

    private void criarSeConfigurado(
            String nome,
            String email,
            String senha,
            UsuarioRole role) {
        email = email.trim();
        if (email.isBlank() || senha.isBlank() || usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(senha);
        usuario.setRole(role);
        usuarioService.salvar(usuario);
    }
}
