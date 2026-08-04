package com.cabinettraduction.togo.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration Spring Security stateless pour l'API REST.
 *
 * CORS : activé ici car le front-office et le back-office admin sont des applications
 * séparées (React, Vue, etc.) servis sur des origines différentes. Si frontend et API
 * sont servis depuis le même domaine (même port), CORS n'est pas nécessaire et tu peux
 * supprimer {@code .cors(…)} ci-dessous.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
		this.jwtFilter = jwtFilter;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// ── CSRF désactivé : API REST stateless, pas de session ──
			.csrf(csrf -> csrf.ignoringRequestMatchers("/api/paiement/webhook") // webhook
																				// FedaPay
																				// signé
				.disable())
			// ── CORS ──
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			// ── Session stateless (JWT) ──
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// ── Règles d'autorisation ──
			.authorizeHttpRequests(auth -> auth
				// ── Pages publiques du site ──
				.requestMatchers(HttpMethod.GET,
						"/", "/devis", "/devis/**",
						"/services", "/a-propos", "/contact", "/mentions-legales")
				.permitAll()
				// ── Page paiement publique ──
				.requestMatchers("/paiement/**")
				.permitAll()
				// ── Téléchargement sécurisé client (token valide l'accès, pas JWT) ──
				.requestMatchers(HttpMethod.GET, "/client/demandes/*/telechargement")
				.permitAll()
				// ── API publique ──
				.requestMatchers("/api/auth/login")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/devis")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/services")
				.permitAll()
				// Webhook et notification CinetPay (appels depuis CinetPay, pas de JWT)
				.requestMatchers(HttpMethod.POST, "/api/paiement/webhook")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/paiement/notify")
				.permitAll()
				// CinetPay endpoints dédiés
				.requestMatchers(HttpMethod.POST, "/api/paiement/cinetpay/notify")
				.permitAll()
				// ── Ressources statiques (CSS, JS, images, webjars) ──
				.requestMatchers("/resources/**", "/static/**", "/css/**", "/js/**", "/images/**", "/webjars/**",
						"/favicon.ico")
				.permitAll()
				// ── Actuator health (supervision) ──
				.requestMatchers("/actuator/health", "/actuator/info")
				.permitAll()
				// ── Pages héritées petclinic (à supprimer plus tard) ──
				.requestMatchers("/owners/**", "/vets/**", "/pets/**", "/oups")
				.permitAll()
				// ── Back-office : utilisateurs → ADMIN uniquement ──
				.requestMatchers("/admin/utilisateurs/**")
				.hasRole("ADMIN")
				// ── Back-office : devis → ADMIN ou EDITEUR ──
				.requestMatchers("/admin/devis/**")
				.hasAnyRole("ADMIN", "EDITEUR")
				// ── Tout le reste sous /admin/** → authentifié ──
				.requestMatchers("/admin/**")
				.authenticated()
				// ── API protégée (paiement init, etc.) ──
				.requestMatchers("/api/paiement/init")
				.authenticated()
				// ── Tout le reste est public (site vitrine) ──
				.anyRequest()
				.permitAll())
			// ── Filtre JWT ──
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	/**
	 * Configuration CORS — autorise les origines déclarées dans les variables d'env. En
	 * production, remplace "*" par l'URL exacte de ton frontend admin.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

}
