package spark.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.weisong.soa.agent.impl.DefaultMgmtAgentJavaConfigZk;
import com.weisong.soa.agent.impl.JvmModuleJavaConfig;
import com.weisong.soa.agent.impl.MainModule;
import com.weisong.soa.core.zk.service.ZkServiceHandlerImpl;

public class ModuleFactory {
	
	static private ApplicationContext ctx;
	
	static public void init() {
		if(ctx == null) {
			ctx = new AnnotationConfigApplicationContext(
			    JavaConfig.class
			  ,	JvmModuleJavaConfig.class
			  , DefaultMgmtAgentJavaConfigZk.class
			  , ZkServiceHandlerImpl.JavaConfig.class);
		}
	}
	
	static public <T> T getBean(Class<T> clazz) {
		return ctx.getBean(clazz);
	}

	@Configuration
	static public class JavaConfig {
		
		@Autowired private MainModule mainModule;

		@Bean public MainServiceModule sparkMainModule() {
			return new MainServiceModule();
		}
		
		@Bean public SparkServerModule sparkServerModule() {
			return  new SparkServerModule(mainModule);
		}
	}
}
