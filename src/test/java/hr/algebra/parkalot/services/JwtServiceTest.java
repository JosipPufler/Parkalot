package hr.algebra.parkalot.services;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.services.db.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest extends AbstractIntegrationTest {
    @Autowired private JwtService jwtService;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private UserService userService;
    @Autowired private TestData data;
    @Value("${jwt.secret}") private String secret;

    @Test
    void generatedToken_isValidAndCarriesUsername() {
        User user = data.user("jwt@test.com", Role.USER);
        String token = jwtService.generateJwtToken(user);

        assertThat(jwtService.validateJwtToken(token)).isTrue();
        assertThat(jwtService.getUsernameFromJwtToken(token)).isEqualTo("jwt@test.com");
    }

    @Test
    void invalidTokens_areRejected() {
        assertThat(jwtService.validateJwtToken("garbage")).isFalse();
        assertThat(jwtService.validateJwtToken("")).isFalse();

        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        String expired = Jwts.builder().subject("x").expiration(new Date(System.currentTimeMillis() - 10_000)).signWith(key).compact();
        assertThat(jwtService.validateJwtToken(expired)).isFalse();

        String unsigned = Jwts.builder().subject("x").compact();
        assertThat(jwtService.validateJwtToken(unsigned)).isFalse();
    }

    @Test
    void refreshTokensAreRandom_andHashingIsDeterministic() {
        assertThat(jwtService.generateRefreshToken()).isNotEqualTo(jwtService.generateRefreshToken());
        assertThat(jwtService.hashToken("abc")).isEqualTo(jwtService.hashToken("abc")).isNotEqualTo("abc");
    }

    @Test
    void accessTokenCookie_isHttpOnlyAndStrict() {
        ResponseCookie cookie = jwtService.generateAccessToken(data.user("cookie@test.com", Role.USER));

        assertThat(cookie.getName()).isEqualTo("JWT_TOKEN");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    @Test
    void extractRefreshToken_readsCookieOrReturnsNull() {
        MockHttpServletRequest none = new MockHttpServletRequest();
        assertThat(jwtService.extractRefreshToken(none)).isNull();

        MockHttpServletRequest unrelated = new MockHttpServletRequest();
        unrelated.setCookies(new Cookie("OTHER", "x"));
        assertThat(jwtService.extractRefreshToken(unrelated)).isNull();

        MockHttpServletRequest withRefresh = new MockHttpServletRequest();
        withRefresh.setCookies(new Cookie("OTHER", "x"), new Cookie("REFRESH_TOKEN", "abc"));
        assertThat(jwtService.extractRefreshToken(withRefresh)).isEqualTo("abc");
    }

    @Test
    void userDetailsService_loadsByEmail_orThrows() {
        data.user("details@test.com", Role.OFFICER);

        UserDetails details = userDetailsService.loadUserByUsername("details@test.com");
        assertThat(details.getUsername()).isEqualTo("details@test.com");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_OFFICER");
        assertThat(details.isEnabled() && details.isAccountNonExpired() && details.isAccountNonLocked()
                && details.isCredentialsNonExpired()).isTrue();

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void userService_existsAndFindByEmail() {
        data.user("exists@test.com", Role.USER);

        assertThat(userService.existsByEmail("exists@test.com")).isTrue();
        assertThat(userService.existsByEmail("nope@test.com")).isFalse();
        assertThat(userService.findByEmail("exists@test.com")).isPresent();
    }
}
