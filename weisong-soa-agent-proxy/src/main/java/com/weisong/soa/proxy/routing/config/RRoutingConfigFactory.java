package com.weisong.soa.proxy.routing.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import com.weisong.soa.proxy.load.balancing.LoadBalancingType;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigBaseListener;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigLexer;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Circuit_breakerContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Circuit_breaker_nameContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.DropContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Err_rate_4xxContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Err_rate_5xxContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Err_rate_timeoutContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Err_rate_totalContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Forward_toContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Forward_to_destContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Forward_weight_valueContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Load_balancing_typeContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Match_valueContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.RouteContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Route_circuit_breaker_nameContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Route_nameContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Route_otherwiseContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.TargetContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Target_circuit_breaker_nameContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Target_groupContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Target_group_nameContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Target_valueContext;
import com.weisong.soa.proxy.routing.antlr4.RRoutingConfigParser.Target_weight_valueContext;


public class RRoutingConfigFactory {

    public RRoutingConfig parse(String config) throws IOException {
    	ByteArrayInputStream in = new ByteArrayInputStream(config.getBytes());
    	return parse(in);
    }
    
    public RRoutingConfig parse(InputStream in) throws IOException {
    	
        RRoutingConfigLexer l = new RRoutingConfigLexer(new ANTLRInputStream(in));
        RRoutingConfigParser p = new RRoutingConfigParser(new CommonTokenStream(l));
        
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
        		int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + 
            		" due to " + msg, e);
            }
        });
        
        final RRoutingConfig routing = new RRoutingConfig();

        p.addParseListener(new RRoutingConfigBaseListener() {
        	
        	private RCircuitBreaker cbDef;
        	private Map<String, RCircuitBreaker> cbDefMap = new HashMap<>();
        	private RTargetGroup targetGroup;
        	private RTarget target;
        	private RRoute route;
        	private RRoute.ForwardTo forwardTo;
        	
			@Override
			public void exitCircuit_breaker_name(Circuit_breaker_nameContext ctx) {
				String name = ctx.getText();
				if(cbDefMap.containsKey(name)) {
					throw new RuntimeException("Duplicated circuit breaker name: " + name);
				}
				cbDef = new RCircuitBreaker();
				cbDef.setName(name);
			}

			@Override
			public void exitErr_rate_total(Err_rate_totalContext ctx) {
				cbDef.setErrTotalThreshold(Float.valueOf(ctx.getText()));
			}

			@Override
			public void exitErr_rate_4xx(Err_rate_4xxContext ctx) {
				cbDef.setErr4xxThreshold(Float.valueOf(ctx.getText()));
			}

			@Override
			public void exitErr_rate_5xx(Err_rate_5xxContext ctx) {
				cbDef.setErr5xxThreshold(Float.valueOf(ctx.getText()));
			}

			@Override
			public void exitErr_rate_timeout(Err_rate_timeoutContext ctx) {
				cbDef.setTimedOutThreshold(Float.valueOf(ctx.getText()));
			}

			@Override
			public void exitCircuit_breaker(Circuit_breakerContext ctx) {
				cbDefMap.put(cbDef.getName(), cbDef);
				cbDef = null;
			}

			@Override
			public void enterTarget_group(Target_groupContext ctx) {
				targetGroup = new RTargetGroup();
			}

			@Override
			public void exitTarget_group(Target_groupContext ctx) {
				routing.getTargetGroups().add(targetGroup);
				targetGroup = null;
			}

			@Override
			public void exitTarget_group_name(Target_group_nameContext ctx) {
				targetGroup.setName(ctx.getText());
			}

			@Override
			public void exitLoad_balancing_type(Load_balancing_typeContext ctx) {
				LoadBalancingType lbType = LoadBalancingType.leastActive;
				if("round-robin".equals(ctx.getText())) {
					lbType = LoadBalancingType.roundRobin;
				}
				if("random".equals(ctx.getText())) {
					lbType = LoadBalancingType.random;
				}
				targetGroup.setLbType(lbType);
			}

			@Override
			public void enterTarget(TargetContext ctx) {
				target = new RTarget(targetGroup);
			}

			@Override
			public void exitTarget_value(Target_valueContext ctx) {
				target.setConnStr(ctx.getText());
			}

			@Override
			public void exitTarget_weight_value(Target_weight_valueContext ctx) {
				target.setWeight(Float.valueOf(ctx.getText()));
			}

			@Override
			public void exitTarget(TargetContext ctx) {
				targetGroup.getTargets().add(target);
			}

			@Override
			public void exitTarget_circuit_breaker_name(
					Target_circuit_breaker_nameContext ctx) {
				String name = ctx.getText();
				RCircuitBreaker def = cbDefMap.get(name);
				if(def == null) {
					throw new RuntimeException("Circuit breaker not found" + name);
				}
				target.setCbDef(def);
			}

			@Override
			public void enterRoute(RouteContext ctx) {
				route = new RRoute();
			}

			@Override
			public void exitRoute(RouteContext ctx) {
				routing.getRoutes().add(route);
				route = null;
			}

			@Override
			public void exitRoute_name(Route_nameContext ctx) {
				route.setName(ctx.getText());
			}

			@Override
			public void enterRoute_otherwise(Route_otherwiseContext ctx) {
				route = new RRoute();
				route.setName("otherwise");
			}

			@Override
			public void exitRoute_otherwise(Route_otherwiseContext ctx) {
				routing.getRoutes().add(route);
				route = null;
			}

			@Override
			public void enterForward_to(Forward_toContext ctx) {
				forwardTo = new RRoute.ForwardTo();
			}

			@Override
			public void exitForward_to(Forward_toContext ctx) {
				route.getForwardToList().add(forwardTo);
				RTargetGroup tg = forwardTo.getTargetGroup();
				if(tg != null) {
					route.getForwardToNames().add(forwardTo.getTargetGroup().getName());
				}
				forwardTo = null;
			}

			@Override
			public void exitForward_to_dest(Forward_to_destContext ctx) {
				RTargetGroup tg = routing.getTargetGroup(ctx.getText());
				if(tg == null) {
					throw new RuntimeException(String.format(
							"Target group %s undefined", ctx.getText()));
				}
				forwardTo.setTargetGroup(tg);
			}

			@Override
			public void exitForward_weight_value(Forward_weight_valueContext ctx) {
				forwardTo.setWeight(Float.valueOf(ctx.getText()));
			}

			@Override
			public void exitMatch_value(Match_valueContext ctx) {
				route.setMatch(ctx.getText().replace("\"", ""));
			}

			@Override
			public void exitRoute_circuit_breaker_name(
					Route_circuit_breaker_nameContext ctx) {
				String name = ctx.getText();
				RCircuitBreaker def = cbDefMap.get(name);
				if(def == null) {
					throw new RuntimeException("Circuit breaker not found" + name);
				}
				route.setCbDef(def);
			}

			@Override
			public void exitDrop(DropContext ctx) {
				route.getForwardToList().clear();
				route.getForwardToNames().clear();
			}
        });

        // Do the parsing
        p.routing();
        
        return routing;
    }
    
}
