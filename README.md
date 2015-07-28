# shiro-redis
A Apache Shiro session cache and realm for Redis, it will allow your application to save your users session in Redis!

How to configure ?
===========
You can choose 2 ways : shiro.ini or spring-shiro.xml

shiro.ini:
```properties
#============redisManager=============
redisManager = wjw.shiro.redis.RedisManager

#redis server list
redisManager.serverlist = ${ip1}:${port1},${ip2}:${port2}
#or password for redis server
redisManager.serverlist = ${ip1}:${port1}:${password1},${ip2}:${port2}:${password2}

#optional, default value:5 .redis min connection
redisManager.minConn = 5

#optional, default value:100 .redis max connection
redisManager.maxConn = 100

#optional, default value:0 .The expire time is in second
redisManager.expire = 1800

#optional, timeout for jedis try to connect to redis server(In milliseconds), not equals to expire time! 
redisManager.socketTO = 6000

#============redisSessionDAO=============
redisSessionDAO = wjw.shiro.redis.RedisSessionDAO
redisSessionDAO.redisManager = $redisManager

sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager
sessionManager.sessionDAO = $redisSessionDAO

securityManager.sessionManager = $sessionManager

#============redisCacheManager===========
cacheManager = wjw.shiro.redis.RedisCacheManager
cacheManager.redisManager = $redisManager

#custom your redis key prefix, if you doesn't define this parameter shiro-redis will use 'shiro_cache:' as default prefix
cacheManager.keyPrefix = shiro_cache:

securityManager.cacheManager = $cacheManager
```

spring-shiro.xml:
```xml
<!-- shiro filter -->
<bean id="ShiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
	<property name="securityManager" ref="securityManager"/>
	
	<!--
	<property name="loginUrl" value="/login.jsp"/>
	<property name="successUrl" value="/home.jsp"/>  
	<property name="unauthorizedUrl" value="/unauthorized.jsp"/>
	-->
	<!-- The 'filters' property is not necessary since any declared javax.servlet.Filter bean  -->
	<!-- defined will be automatically acquired and available via its beanName in chain        -->
	<!-- definitions, but you can perform instance overrides or name aliases here if you like: -->
	<!-- <property name="filters">
		<util:map>
			<entry key="anAlias" value-ref="someFilter"/>
		</util:map>
	</property> -->
	<property name="filterChainDefinitions">
		<value>
			/login.jsp = anon
			/user/** = anon
			/register/** = anon
			/unauthorized.jsp = anon
			/css/** = anon
			/js/** = anon
			
			/** = authc
		</value>
	</property>
</bean>

<!-- shiro securityManager -->
<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">

	<!-- Single realm app.  If you have multiple realms, use the 'realms' property instead. -->
	
	<!-- sessionManager -->
	<property name="sessionManager" ref="sessionManager" />
	
	<!-- cacheManager -->
	<property name="cacheManager" ref="cacheManager" />
	
	<!-- By default the servlet container sessions will be used.  Uncomment this line
		 to use shiro's native sessions (see the JavaDoc for more): -->
	<!-- <property name="sessionMode" value="native"/> -->
</bean>
<bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>	

<!-- shiro redisManager -->
<bean id="redisManager" class="wjw.shiro.redis.RedisManager">
	<property name="serverlist" value="${ip1}:${port1},${ip2}:${port2}"/>
	<property name="minConn" value="5"/>
	<property name="maxConn" value="5"/>
	<property name="expire" value="1800"/>
	<property name="socketTO" value="6000"/>
</bean>

<!-- redisSessionDAO -->
<bean id="redisSessionDAO" class="wjw.shiro.redis.RedisSessionDAO">
	<property name="redisManager" ref="redisManager" />
</bean>

<!-- sessionManager -->
<bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
	<property name="sessionDAO" ref="redisSessionDAO" />
</bean>

<!-- cacheManager -->
<bean id="cacheManager" class="wjw.shiro.redis.RedisCacheManager">
	<property name="redisManager" ref="redisManager" />
</bean>
```

> Note:  
> Shiro-redis don't support SimpleAuthenticationInfo created by this constructor `org.apache.shiro.authc.SimpleAuthenticationInfo.SimpleAuthenticationInfo(Object principal, Object hashedCredentials, ByteSource credentialsSalt, String realmName)`.  
> Please use `org.apache.shiro.authc.SimpleAuthenticationInfo.SimpleAuthenticationInfo(Object principal, Object hashedCredentials, String realmName)` instead.  

If you found any bugs
===========

Please send email to wjw465150@gmail.com 
