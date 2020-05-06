package com.util;

import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

public class RequestFilter {

	ConcurrentHashMap<String, Calendar> ipAddressTimeMap = new ConcurrentHashMap<String, Calendar>();
	
	public boolean doFilter(HttpServletRequest request)  {
		String clientIP=request.getRemoteAddr();
		
					if(!(ipAddressTimeMap.containsKey(clientIP)))
					{
							ipAddressTimeMap.put(clientIP,Calendar.getInstance());
							return true;
					}
					else
					{
						Calendar clientTime=ipAddressTimeMap.get(clientIP);
						Calendar currentTime = Calendar.getInstance();
						long diff=CalcTimeDiff(clientTime,currentTime);
					
						if(diff <= 1 )
						{
							//System.out.println(Calendar.getInstance());
							//System.out.println("Too many requests. Please try again!");
							return false;
						}
					
						else
						{
							ipAddressTimeMap.put(clientIP, Calendar.getInstance());
							return true;
						}
			
					}
	}
	
	private long CalcTimeDiff(Calendar clientTime, Calendar currentTime)
	{
		long diff=(currentTime.getTimeInMillis() - clientTime.getTimeInMillis())/1000;
		return diff;
	}

}
