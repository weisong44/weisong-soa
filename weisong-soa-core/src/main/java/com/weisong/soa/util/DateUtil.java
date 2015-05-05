package com.weisong.soa.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

	final static private SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
	
	static public Date parse(String s) throws ParseException {
		return df.parse(s);
	}
	
	static public String format(Date date) {
		return df.format(date);
	}
	
}
