# Spring Boot 2 OAuth2 JWT Authorization Server

Simple project on how to setup **OAuth2** authorization server with **JWT** tokens using **Spring Boot 2**, **JPA**, **Hibernate** and **MySQL**.

## In Short

All [Users](src/main/java/com/kristijangeorgiev/auth/entity/User.java) and Clients are stored in the database. [Users](src/main/java/com/kristijangeorgiev/auth/entity/User.java) can have many [Roles](src/main/java/com/kristijangeorgiev/auth/entity/Role.java) associated with them and [Roles](src/main/java/com/kristijangeorgiev/auth/entity/Role.java) can have many [Permissions](src/main/java/com/kristijangeorgiev/auth/entity/Permission.java) associated with them which in the end are added as a list of **authorities** in the **JWT** token.

First we must generate a **KeyStore** file. To do that execute the following command:
```
keytool -genkeypair -alias jwt -keyalg RSA -keypass password -keystore jwt.jks -storepass password
```
(if you're under Windows go your Java install dir and there you'll find a jar named ***keytool***)

The command will generate a file called ***jwt.jks*** which contains the **Public** and **Private** keys.

It is recommended to migrate to **PKCS12**. To do that execute the following command:
```
keytool -importkeystore -srckeystore jwt.jks -destkeystore jwt.jks -deststoretype pkcs12
```
Now let's export the public key:
```
keytool -list -rfc --keystore jwt.jks | openssl x509 -inform pem -pubkey
```
Copy the ***jwt.jks*** file to the [Resources](src/main/resources) folder.

Copy from (including) ***-----BEGIN PUBLIC KEY-----*** to (including) ***-----END PUBLIC KEY-----*** and save it in a file. You'll need this later in your resource servers.

There's a custom [User](src/main/java/com/kristijangeorgiev/auth/entity/User.java) class which implements the **UserDetails** interface and has all the required methods and an additional **email** field;

There's the [UserRepository](src/main/java/com/kristijangeorgiev/auth/repository/) in which there are 2 methods, one for finding a [User](src/main/java/com/kristijangeorgiev/auth/entity/User.java) entity by **username** and the other by **email**. This means we can authenticate a [User](src/main/java/com/kristijangeorgiev/auth/entity/User.java) based on the **username** or the **email**.

In order to use our custom [User](src/main/java/com/kristijangeorgiev/auth/entity/User.java) object we must provide with a [CustomUserDetailsService](src/main/java/com/kristijangeorgiev/auth/service/CustomUserDetailsService.java) which implements the **UserDetailsService**. The **loadUserByUsername** method is overriden and set up to work with our logic.

## Configure [WebSecurity](src/main/java/com/kristijangeorgiev/auth/configuration/WebSecurityConfiguration.java)

In **Spring Boot 2** you must use the **DelegatingPasswordEncoder**.
```
@Bean
public PasswordEncoder passwordEncoder() {
	return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```
AuthenticationManagerBean
```
@Bean
@Override
public AuthenticationManager authenticationManagerBean() throws Exception {
	return super.authenticationManagerBean();
}
```
Configure AuthenticationManagerBuilder
```
@Autowired
private CustomUserDetailsService userDetailsService;

@Override
public void configure(AuthenticationManagerBuilder auth) throws Exception {
	auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
}
```

HTTP Security configuration
```
@Override
public void configure(HttpSecurity http) throws Exception {
	http.csrf().disable().exceptionHandling()
			.authenticationEntryPoint(
					(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
			.and().authorizeRequests().antMatchers("/**").authenticated().and().httpBasic();
}
```

## Configure [OAuth2](src/main/java/com/kristijangeorgiev/auth/configuration/OAuth2Configuration.java)

```
@Configuration
@EnableAuthorizationServer
public class OAuth2Configuration extends AuthorizationServerConfigurerAdapter {...
```

There must be an **AuthenticationManager** provided
```
@Autowired
@Qualifier("authenticationManagerBean")
private AuthenticationManager authenticationManager;
```

Autowire the **DataSource** and set **OAuth2** clients to use the database and the **PasswordEncoder**.
```
@Autowired
private DataSource dataSource;

@Autowired
private PasswordEncoder passwordEncoder;

@Override
public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
	clients.jdbc(dataSource).passwordEncoder(passwordEncoder);
}
```

Configure the endpoints to use the custom beans.
```
@Autowired
private CustomUserDetailsService userDetailsService;

@Bean
public TokenStore tokenStore() {
	return new JwtTokenStore(jwtAccessTokenConverter());
}

@Override
public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		endpoints.tokenStore(tokenStore()).tokenEnhancer(jwtAccessTokenConverter())
			.authenticationManager(authenticationManager).userDetailsService(userDetailsService);
}
```

Configure who has acces to the **OAuth2** server
```
@Override
public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
	oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated()");
}
```

In order to add additional data in the **JWT** token we must implement a **CustomTokenEnchancer**.
```
protected static class CustomTokenEnhancer extends JwtAccessTokenConverter {
	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		User user = (User) authentication.getPrincipal();

		Map<String, Object> info = new LinkedHashMap<String, Object>(accessToken.getAdditionalInformation());

		info.put("email", user.getEmail());

		DefaultOAuth2AccessToken customAccessToken = new DefaultOAuth2AccessToken(accessToken);
		customAccessToken.setAdditionalInformation(info);

		return super.enhance(customAccessToken, authentication);
	}
}
```

Configure the token converter.
```
@Bean
public JwtAccessTokenConverter jwtAccessTokenConverter() {
	JwtAccessTokenConverter converter = new CustomTokenEnhancer();
	converter.setKeyPair(
			new KeyStoreKeyFactory(new ClassPathResource("jwt.jks"), "password".toCharArray()).getKeyPair("jwt"));
	return converter;
}
```


## Installing

Just clone or download the repo and import it as an existing maven project.

You'll also need to set up [Project Lombok](https://projectlombok.org/) or if you don't want to use this library you can remove the associated annotations from the code and write the getters, setters, constructors, etc. by yourself.

## Use
To test it I used [HTTPie](https://httpie.org/). It's similar to CURL.

To get a **JWT** token execute the following command:
```
http --form POST adminapp:password@localhost:9999/oauth/token grant_type=password username=user password=password
```

```
ACCESS_TOKEN={the access token}
REFRESH_TOKEN={the refresh token}
```

To access a resource use (you'll need a different application which has configured **ResourceServer**):
```
http localhost:8080/users 'Authorization: Bearer '$ACCESS_TOKEN
```

To use the refresh token functionality:
```
http --form POST adminapp:password@localhost:9999/oauth/token grant_type=refresh_token refresh_token=$REFRESH_TOKEN
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
