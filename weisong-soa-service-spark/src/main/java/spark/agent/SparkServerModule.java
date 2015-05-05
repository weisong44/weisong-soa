package spark.agent;

import com.weisong.soa.agent.Module;
import com.weisong.soa.agent.impl.BaseCallModule;

public class SparkServerModule extends BaseCallModule {

	public SparkServerModule(Module parent) {
		super(parent);
	}

	@Override
	public String getName() {
		return "server-call-stats";
	}

	@Override
	public String getType() {
		return "spark-server-call-stats";
	}

	@Override
	public String getDescription() {
		return "The Spark server";
	}
}
