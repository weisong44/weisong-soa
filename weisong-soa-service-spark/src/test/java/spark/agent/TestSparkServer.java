package spark.agent;

import java.util.Properties;
import java.util.Random;

import spark.Request;
import spark.Response;
import spark.Spark;

import com.weisong.soa.core.zk.ZkConst;
import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry;
import com.weisong.soa.core.zk.config.ZkPropertyChangeRegistry.Listener;
import com.weisong.soa.service.ServiceConst;

public class TestSparkServer {
	
	static Random random = new Random();
	static private int value = 5;
	
	static public class ValueProvider extends Thread {
		public void run() {
			while(true) {
				try {
					value = random.nextInt(10);
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
    public static void main(String[] args) {
    	
    	System.setProperty(ZkConst.ZK_CONN_STR, "localhost:2181");
		
    	new ValueProvider().start();

    	int[] delays = new int[] {
    		0, 10, 20, 30, 40	
    	};
    	for(int i = 0; i < delays.length; i++) {
    		final int index = i;
    		String uri = "/hello" + i;
            Spark.get(uri, (req, res)  -> { return delayAndServer(req, res, delays[index]); });
            Spark.post(uri, (req, res) -> { return delayAndServer(req, res, delays[index]); });
            Spark.put(uri, (req, res)  -> { return delayAndServer(req, res, delays[index]); });
    	}
        
    	ZkPropertyChangeRegistry reg = ModuleFactory.getBean(ZkPropertyChangeRegistry.class);
    	reg.addPropertyChangeListener(new Listener() {
			@Override
			public void propertyChanged(Properties props) {
				System.out.println("Delay updated:");
				for(int i = 0; i < delays.length; i++) {
					setDelay(props, i);
				}
			}
			
			private void setDelay(Properties props, int index) {
				String value = props.getProperty("hello" + index);
				if(value != null) {
					try {
						int intValue = Integer.valueOf(value);
						System.out.println(String.format("  hello%d %d -> %d", 
								index, delays[index], intValue));
						delays[index] = intValue;
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} 
				}
			}
    	});
    	
    }

	static int[] codes = new int[] {
		200, 400, 200, 401, 200, 403, 200, 404, 200, 500, 200, 501	
	};
    
    static private String delayAndServer(Request req, Response res, long t) throws Exception {
    	long sleepValue = t + value;
    	Thread.sleep(sleepValue);

    	transferHeader(req, res, ServiceConst.HEADER_REQUEST_ID);
    	transferHeader(req, res, ServiceConst.HEADER_DOMAIN);
    	transferHeader(req, res, ServiceConst.HEADER_SERVICE_NAME);
    	transferHeader(req, res, ServiceConst.HEADER_SERVICE_VERSION);
    	transferHeader(req, res, ServiceConst.HEADER_USER_DATA);
    	
    	int code = codes[random.nextInt(codes.length)];
    	res.status(code);
    	
    	return String.format("Hello world! [%d ms delay, status %d]\n", t, code);
    }
    
    static private void transferHeader(Request req, Response res, String header) {
    	String value = req.headers(header);
    	if(value != null) {
    		res.header(header, value);
    	}
    }
}
