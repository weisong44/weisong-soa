package com.weisong.soa.agent.impl;

import java.rmi.registry.Registry;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.ConnectorServerFactoryBean;
import org.springframework.remoting.rmi.RmiRegistryFactoryBean;

import com.weisong.soa.agent.MgmtAgent;
import com.weisong.soa.agent.Module;
import com.weisong.soa.util.HostUtil;

abstract public class BaseDefaultMgmtAgentJavaConfig {
    
    final static String JMX_RMI_URL = "service:jmx:rmi://${HOST}/jndi/rmi://localhost:%d/jmxrmi";
    
    @Value("${app.domain}") private String appDomain;
    @Value("${app.name}") private String appName;
    @Value("${rmi.port:1099}") private int rmiPort;
    
    @Autowired private RmiRegistryFactoryBean rmiRegistryFactory;
    @Autowired private ConnectorServerFactoryBean connectorServerFactory;
    
    @Bean
    public RmiRegistryFactoryBean rmiRegistryFactoryBean() {
        RmiRegistryFactoryBean factoryBean = new RmiRegistryFactoryBean();
        factoryBean.setAlwaysCreate(true);
        factoryBean.setPort(rmiPort);
        return factoryBean;
    }
    
    @Bean 
    public Registry rmiRegistry() throws Exception {
        return rmiRegistryFactory.getObject();
    }
    
    @Bean
    public ConnectorServerFactoryBean connectorServerFactoryBean() throws Exception {
        ConnectorServerFactoryBean factory = new ConnectorServerFactoryBean();
        factory.setObjectName(createMBeanName(factory));
        String url = JMX_RMI_URL.replace("${HOST}", HostUtil.getHostIpAddress());
        factory.setServiceUrl(String.format(url, rmiPort));
        return factory;
    }

    private ObjectName createMBeanName(Object bean) throws MalformedObjectNameException {
		String objName = null;
		if(bean instanceof Module) {
			objName = ((Module) bean).getMBeanPath();
		}
		else {
			objName = String.format("%s:service=%s,name=%s", appDomain, appName, 
					bean.getClass().getSimpleName());
		}
		return new ObjectName(objName);
    }
    
    @Bean
    public ObjectNamingStrategy objectNamingStrategy() {
    	return new ObjectNamingStrategy() {
			@Override
			public ObjectName getObjectName(Object managedBean, String beanKey)
					throws MalformedObjectNameException {
				return createMBeanName(managedBean);
			}
		};
    }
    
    @Bean
    public JMXConnectorServer jmxConnectorServer() throws Exception {
        return connectorServerFactory.getObject();
    }
    
    @Bean
    public MgmtAgent mgmtAgent() {
    	return new DefaultMgmtAgent();
    }
}
