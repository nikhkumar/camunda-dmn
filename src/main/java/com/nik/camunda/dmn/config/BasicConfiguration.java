package com.nik.camunda.dmn.config;

import javax.ws.rs.HttpMethod;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class BasicConfiguration extends WebSecurityConfigurerAdapter {

	/** Authentication : User --> Roles */
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication()
			.withUser("apiuser").password("{noop}useruser").roles("USER")
			.and()
			.withUser("apiadmin").password("{noop}adminadmin").roles("ADMIN");
	}

	// Secure the end points with HTTP Basic authentication
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                //HTTP Basic authentication
                .httpBasic()
                .and()
                .authorizeRequests()
                //.antMatchers(HttpMethod.GET, 	"/rule").permitAll()	//hasRole("USER")
                .antMatchers(HttpMethod.POST, 	"/rule").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, 	"/rule").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/rule").hasRole("ADMIN")
                .anyRequest().permitAll()				//Any URL that has not already been matched on, permitall
                .and()
                .csrf().disable()
                .formLogin().disable();
         
    }


}