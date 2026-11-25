package cl.induztek.ms_usuarios.security;

import cl.induztek.ms_usuarios.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.util.Date;

@Service
public class JwtService {

    private final SecretKey clave;

    private final long expiracion;

    public JwtService(
        @Value("${jwt.secret}") String secreto,
        @Value("${jwt.expiration}") long expiracion
    ) {

        this.clave = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(secreto)
        );

        this.expiracion = expiracion;
    }

    public String generarToken(
        Usuario usuario,
        boolean requiereCambio
    ) {

        Date ahora = new Date();

        Date vencimiento = new Date(
            ahora.getTime() + expiracion
        );

        return Jwts.builder()

            .subject(usuario.getId().toString())

            .issuer("induztek")

            .claim("correo", usuario.getCorreo())

            .claim("rolId", usuario.getRol().getId())

            .claim("rol", usuario.getRol().getCodigo())

            .claim(
                "requiereCambioContrasena",
                requiereCambio
            )

            .issuedAt(ahora)

            .expiration(vencimiento)

            .signWith(clave, Jwts.SIG.HS256)

            .compact();
    }

    public Claims obtenerClaims(String token) {

        return Jwts.parser()

            .verifyWith(clave)

            .requireIssuer("induztek")

            .build()

            .parseSignedClaims(token)

            .getPayload();
    }

    public Long obtenerIdUsuario(String token) {

        return Long.valueOf(
            obtenerClaims(token).getSubject()
        );
    }
}