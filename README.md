# Spring Boot 2 OAuth2 JWT Authorization Server

### Link to [Spring Boot 2 OAuth2 JWT Resource Server](https://github.com/dzinot/spring-boot-2-oauth2-resource-jwt) project
---

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

## Database [oauth2.sql](src/main/resources/oauth2.sql) and [application.yml](src/main/resources/application.yml)

The database with all the tables and a test client and users. Check the configuration in the [application.yml](src/main/resources/application.yml) file.

### Users

**username**: ***admin*** or ***user***

**password**: ***password***

### Clients

**client**: ***adminapp***

**secret**: ***password***

The ***admin*** is associated with a ***role_admin*** and that role is associated with several permissions.
The ***user*** is associated with a ***role_user*** and read permissions.

### checkUserScopes

If **checkUserScopes** is set to **false** (default **Spring Boot 2** functionality), no checks will be done between the client **scope** and the user **authorities**.

If **checkUserScopes** is set to **true** (see documentation below), then when a user tries to authenticate with a client, we check whether at least one of the user **authorities** is contained in the client **scope**. (I don't know why the default implementation is not done like this)

**checkUserScopes** is set as a property inside the [application.yml](src/main/resources/application.yml) file.
```
check-user-scopes: true
```
And we get the value in the [OAuth2Configuration](src/main/java/com/kristijangeorgiev/auth/configuration/OAuth2Configuration.java) class.
```
@Value("${check-user-scopes}")
private Boolean checkUserScopes;
```

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
private UserDetailsService userDetailsService;

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
private UserDetailsService userDetailsService;

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

In order to make **checkUserScopes** to work, you must set that field in the **RequestFactory** and configure Spring to use that factory in the **endpoints** configuration. This should've worked just like this but for some reason when the **checkUserScopes** is enabled the authentication of a user works fine but the refresh token is not working. When you hit the token endpoint with the refresh token, Spring sets the **Authentication** in the Security Context to be the one of the client, not the user from the refresh token and it doesn't update it later. This means when checks are done on the **scope** and **authorities** you always get the **authorities** from the client, not the user. 

I've created a **CustomOAuth2RequestFactory** that extends the **DefaultOAuth2RequestFactory** and override the **createTokenRequest** method where I get the **Authentication** from the refresh token, autowire the **userDetailsService**, get the [User](src/main/java/com/kristijangeorgiev/auth/entity/User.java) from the database and manually update the Security Context. This means if there are any changes to the [User](src/main/java/com/kristijangeorgiev/auth/entity/User.java) we always check for details from the database and not the refresh token itself.

```
class CustomOauth2RequestFactory extends DefaultOAuth2RequestFactory {
	@Autowired
	private TokenStore tokenStore;

	public CustomOauth2RequestFactory(ClientDetailsService clientDetailsService) {
		super(clientDetailsService);
	}

	@Override
	public TokenRequest createTokenRequest(Map<String, String> requestParameters,
			ClientDetails authenticatedClient) {
		if (requestParameters.get("grant_type").equals("refresh_token")) {
			OAuth2Authentication authentication = tokenStore.readAuthenticationForRefreshToken(
					tokenStore.readRefreshToken(requestParameters.get("refresh_token")));
			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(authentication.getName(), null,
							userDetailsService.loadUserByUsername(authentication.getName()).getAuthorities()));
		}
		return super.createTokenRequest(requestParameters, authenticatedClient);
	}
}
```

Define a **requestFactory** bean. You'll also need the **clientDetailsService** here.
```
@Autowired
private ClientDetailsService clientDetailsService;

@Bean
public OAuth2RequestFactory requestFactory() {
	CustomOauth2RequestFactory requestFactory = new CustomOauth2RequestFactory(clientDetailsService);
	requestFactory.setCheckUserScopes(true);
	return requestFactory;
}
```
Last step is to configure the endpoints to use this **requestFactory**. Because we are doing check on the **checkUserScopes** we have this in the endpoints configuration method.
```
if (checkUserScopes)
	endpoints.requestFactory(requestFactory());
```

## Installing

Just clone or download the repo and import it as an existing maven project.

You'll also need to set up [Project Lombok](https://projectlombok.org/) or if you don't want to use this library you can remove the associated annotations from the code and write the getters, setters, constructors, etc. by yourself.

## Use

To test it I used [HTTPie](https://httpie.org/). It's similar to CURL.

To get a **JWT** token execute the following command:
```
http --form POST adminapp:password@localhost:9999/oauth/token grant_type=password username=admin password=password
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
