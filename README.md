# shiro-redis
A Apache Shiro `session`,`cache`,`realm` for Redis, it will allow your application to save your users `cache`,`session`,`realm` in Redis!

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

securityManager.cacheManager = $cacheManager

#============redisRealm===========
redisRealm = wjw.shiro.redis.RedisRealm
redisRealm.redisManager = $redisManager
redisRealm.permissionsLookupEnabled = true
securityManager.realms=$redisRealm
```

spring-shiro.xml:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans" xmlns:util="http://www.springframework.org/schema/util" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="
       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd">

  <!-- 基于Form表单的身份验证过滤器 -->
  <bean id="formAuthenticationFilter"		class="org.apache.shiro.web.filter.authc.FormAuthenticationFilter">
  	<property name="usernameParam" value="userText" />
  	<property name="passwordParam" value="passText" />
  	<property name="loginUrl" value="/login.do" />      <!-- 表单要提交到的URL地址  -->
  	<property name="successUrl" value="/success.jsp" />
  </bean>
  
  <!-- shiro的Web主过滤器,beanId 和web.xml中配置的filter name需要保持一致 -->
  <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
  	<property name="securityManager" ref="securityManager" />
  	
  	<property name="loginUrl" value="/login.jsp" />     <!-- 要填写登录信息的URL地址  -->
  	<property name="unauthorizedUrl" value="/unauthorized.jsp" />
  	<property name="filters">
  		<util:map>
  			<entry key="authc" value-ref="formAuthenticationFilter" />
  		</util:map>
  	</property>
  	<property name="filterChainDefinitions">
  		<value>
  			/js/** = anon
  			/index.htm*= anon
  			/unauthorized.jsp*= anon
  			/login.jsp* = anon
  			/login.do = authc
  			/logout = logout
  			/** = user
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
    
    <!-- realm -->
    <property name="realm" ref="redisRealm" />
    
    <!-- By default the servlet container sessions will be used.  Uncomment this line
       to use shiro's native sessions (see the JavaDoc for more): -->
    <!-- <property name="sessionMode" value="native"/> -->
  </bean>
  
  <!-- 相当于调用SecurityUtils.setSecurityManager(securityManager) -->
  <bean class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
    <property name="staticMethod" value="org.apache.shiro.SecurityUtils.setSecurityManager" />
    <property name="arguments" ref="securityManager" />
  </bean>

  <!-- Shiro生命周期处理器 -->
  <bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>  
  
  <!-- shiro redisManager -->
  <bean id="redisManager" class="wjw.shiro.redis.RedisManager">
    <property name="serverlist" value="${ip1}:${port1},${ip2}:${port2}"/>
    <property name="minConn" value="5"/>
    <property name="maxConn" value="100"/>
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
  
  <!-- redisRealm -->
  <bean id="redisRealm" class="wjw.shiro.redis.RedisRealm">
    <property name="redisManager" ref="redisManager" />
    <property name="permissionsLookupEnabled" value="true" />
  </bean>
</beans>
```

> Note:  
> Shiro-redis don't support SimpleAuthenticationInfo created by this constructor `org.apache.shiro.authc.SimpleAuthenticationInfo.SimpleAuthenticationInfo(Object principal, Object hashedCredentials, ByteSource credentialsSalt, String realmName)`.  
> Please use `org.apache.shiro.authc.SimpleAuthenticationInfo.SimpleAuthenticationInfo(Object principal, Object hashedCredentials, String realmName)` instead.  

If you found any bugs
===========

Please send email to wjw465150@gmail.com 
