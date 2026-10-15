package br.com.aweb.sistema_vendas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import br.com.aweb.sistema_vendas.security.UsuariosIniciais;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuarios_iniciais_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "ADMIN_EMAIL= admin.inicial@example.com ",
        "ADMIN_PASSWORD=senha-admin-teste",
        "PADRAO_EMAIL=padrao.inicial@example.com",
        "PADRAO_PASSWORD=senha-padrao-teste"
})
class UsuariosIniciaisTests {

    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UsuariosIniciais usuariosIniciais;

    @Test
    void criaOsDoisPerfisComHashEPreservaCredenciaisAoReiniciar() {
        Usuario admin = usuarioRepository.findByEmailIgnoreCase("ADMIN.INICIAL@EXAMPLE.COM").orElseThrow();
        Usuario padrao = usuarioRepository.findByEmailIgnoreCase("padrao.inicial@example.com").orElseThrow();
        assertEquals(UsuarioRole.ADMIN, admin.getRole());
        assertEquals(UsuarioRole.PADRAO, padrao.getRole());
        assertTrue(passwordEncoder.matches("senha-admin-teste", admin.getPassword()));
        assertTrue(passwordEncoder.matches("senha-padrao-teste", padrao.getPassword()));

        String hashOriginal = admin.getPassword();
        String hashAlterado = passwordEncoder.encode("senha-alterada");
        padrao.setSenha(hashAlterado);
        usuarioRepository.save(padrao);
        usuariosIniciais.run();

        assertEquals(2, usuarioRepository.count());
        assertEquals(hashOriginal, usuarioRepository.findById(admin.getId()).orElseThrow().getPassword());
        assertEquals(hashAlterado, usuarioRepository.findById(padrao.getId()).orElseThrow().getPassword());
    }
}
