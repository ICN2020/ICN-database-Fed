package com.ogb.fes.tesseler.temporal;


import java.util.ArrayList;
import java.util.Date;


public class TemporalUtils 
{
	public static int MAX_LEVEL = 5;
	

    public static long dateToMinutes(Date date) {
        return millisecondsToMinutes(date.getTime());
    }
    
    public static long millisecondsToMinutes(long dateMilliseconds) {
        return (long) (dateMilliseconds / Math.floor(1000 * 60));
    }
  
    public static String dateToTemporalSuffixFromDate(Date date) {
        return dateToTemporalSuffixFromMinutes(millisecondsToMinutes(date.getTime()));
    }
    public static String dateToTemporalSuffixFromMillisecons(long milliseconds){
        
        return dateToTemporalSuffixFromMinutes(millisecondsToMinutes(milliseconds));
    }
    public static String dateToTemporalSuffixFromMinutes(long minutes) {
        return String.join("/", dateToTemporalComponent(minutes));
    }
    
    private static ArrayList<String> dateToTemporalComponent(long minutes) {
        String            minutesString = minutes + "";
        ArrayList<String> component     = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (minutesString.length()-i-1 >= 0)
                component.add(0, ""+minutesString.charAt(minutesString.length()-i-1));
        }
        
        for (int i = component.size(); i < 4; i++)
            component.add(0, ""+0);
        
        if (minutesString.length() > 4)
            component.add(0, ""+minutesString.substring(0, minutesString.length()-4));  
        
        return component;
    }
    private static ArrayList<String> dateToTemporalComponentAtLevel(long minutes, int level)  {
        ArrayList<String> components = dateToTemporalComponent(minutes);
        
        if (components.size() <= level)
            return components;
        
        do {
            components.remove(components.size()-1);
            
            if (components.size() <= level)
                break;
        }while (true);

        return components;
    }
    
    public static String computeTimeTileSuffix(TemporalNode node){
        long start = node.getStartTime();
        long stop  = node.getStopTime();
        
        return String.join("/",dateToTemporalComponentAtLevel(start, (MAX_LEVEL-(int)Math.log10((double)(stop-start)))));
    }
}
