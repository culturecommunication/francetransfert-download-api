package fr.gouv.culture.francetransfert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * The type Demo application.
 */
@EnableGlobalMethodSecurity(prePostEnabled = true)
@SpringBootApplication
public class FranceTransfertDownloadStarter extends WebSecurityConfigurerAdapter {

	@Autowired
	private Environment env;

	/**
	 * The entry point of application.
	 *
	 * @param args the input arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(FranceTransfertDownloadStarter.class, args);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().exceptionHandling()
				.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)).and().csrf().disable()
				.headers().frameOptions().disable();
	}

	@Bean
	public OpenAPI springShopOpenAPI() {
		return new OpenAPI().info(new Info().title(env.getProperty("tool.swagger.api.title"))
				.description(env.getProperty("tool.swagger.api.description"))
				.version(env.getProperty("tool.swagger.api.version"))
				.license(new License().name(env.getProperty("tool.swagger.api.licence"))
						.url(env.getProperty("tool.swagger.api.licence.url")))
				.termsOfService(env.getProperty("tool.swagger.api.terms-of-services-use")));
	}

}
