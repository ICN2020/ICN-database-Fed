package com.ogb.fes.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DateTime 
{
	public static String currentTime() 
	{	
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return "[" + dateFormat.format(new Date()) + "] ";
	}
}
