package spark.agent;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.weisong.soa.agent.impl.MainModule;
import com.weisong.soa.core.zk.service.ZkServiceHandlerWriteOnly;
import com.weisong.soa.service.ServiceDescriptor;

public class MainServiceModule extends MainModule {

	@Value("${app.port:-1}") private int port;
	
	@Autowired private ZkServiceHandlerWriteOnly serviceHandler;
	
	@PostConstruct
	protected void register() throws Exception {
		super.register();
		ServiceDescriptor desc = new ServiceDescriptor(appDomain, appName, appVersion);
		serviceHandler.register(desc, port);
	}
	
}
