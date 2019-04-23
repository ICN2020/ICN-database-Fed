package com.ogb.time;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;


public class TimeNode {
    public enum TimeContained {
        kTimeContained_Before,
        kTimeContained_Between,
        kTimeContained_After
    }
    
    
    private long     startTime;
    private long     stopTime;
    private boolean  isFull;
    private TimeNode father;
    
    private HashMap<String, TimeNode> childs;
    
    
    
    public TimeNode(long start, long stop) {
        this.startTime = start;
        this.stopTime  = stop;
        this.father    = null;
        this.childs    = new HashMap<String, TimeNode>();
        
        checkIsFull();
    }
    
    public TimeNode appendChild(TimeNode node) {
        return appendChild(node.startTime, node.stopTime);
    }
    public TimeNode appendChild(long start, long stop) {
        String key = start+":"+stop;
        
        TimeNode node = new TimeNode(start, stop);
        node.setFather(this);
        if (node.isValid() == true) {
            childs.put(key, node);
            //System.out.println(node);
            return node;
        }
        
        return null;
    }
    
    private void checkIsFull()
    {
        isFull = false;
        
        if ((stopTime-startTime) == 1 && father != null) 
            isFull = true;
        if ((stopTime-startTime) == 10 && father != null)
            isFull = true;
        if ((stopTime-startTime) == 100 && father != null) 
            isFull = true;
        if ((stopTime-startTime) == 1000 && father != null) 
            isFull = true;
        if ((stopTime-startTime) == 10000 && father != null) 
            isFull = true;
    }
    
    public int evaluateCurrentDepthLevel() {
        if (father == null)
            return 0;
        return 1+father.evaluateCurrentDepthLevel();
    }

    
    public static TimeContained nodeContains(TimeNode node, long timeMinutes) {
        if (timeMinutes < node.startTime)
            return TimeContained.kTimeContained_Before;
        
        if (timeMinutes > node.stopTime)
            return TimeContained.kTimeContained_After;
        
        return TimeContained.kTimeContained_Between;
    }
    public TimeContained contains(TimeNode node, long timeMinutes) {
        return TimeNode.nodeContains(this, timeMinutes);
    }
    
    
    public boolean isValid() {
        return !((stopTime-startTime) == 0);
    }
    
    public boolean isFull() {
        return isFull;
    }
    
    public void setIsFull(boolean full) {
        isFull = full;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }
    
    public void setFather(TimeNode father) {
        this.father = father;
        
        checkIsFull();
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getStopTime() {
        return stopTime;
    }
    
    public TimeNode getFather() {
        return father;
    }
    
    
    public ArrayList<TimeNode> getChilds() {
        return new ArrayList<TimeNode>(childs.values());
    }
    public ArrayList<TimeNode> getFullCoveredChilds() {
        ArrayList<TimeNode> list = new ArrayList<TimeNode>();
        for (TimeNode node : childs.values())
            if (node.isFull == true)
                list.add(node);
        return list;
    }
    public ArrayList<TimeNode> getPartialCoveredChilds() {
        ArrayList<TimeNode> list = new ArrayList<TimeNode>();
        for (TimeNode node : childs.values())
            if (node.isFull == false)
                list.add(node);
        return list;
    }
    public ArrayList<TimeNode> getLeafList() {
        ArrayList<TimeNode> list = new ArrayList<TimeNode>();
        list.addAll(getFullCoveredChilds());
        for (TimeNode node : getPartialCoveredChilds()) {
            list.addAll(node.getLeafList());
        }
        return list;
    }
    
    public static long getChildsCountAtLevel(TimeNode head, int level) {
        long count = 0;
        
        if (level < 0)
            return 0;
        
        if (level == 0)
            return head.getChilds().size();
        
        if (level > 0) {
            for (TimeNode node : head.getChilds()) {
                count += getChildsCountAtLevel(node, level-1);
            }
        }
        
        return count;
    }
    
    public static ArrayList<TimeNode> getPartialCoveredNodesAtLevel(TimeNode head, int level) {
        
        ArrayList<TimeNode> result = new ArrayList<>();
        
        if (level < 0)
            return result;
        
        if (level == 0)
            return head.getPartialCoveredChilds();
        
        if (level > 0) {
            for (TimeNode node : head.getChilds()) {
                result.addAll(getPartialCoveredNodesAtLevel(node, level-1));
            }
        }
        
        return result;
    }
    
    public static ArrayList<TimeNode> getNodesAtLevel(TimeNode head, int level) {
        
        ArrayList<TimeNode> result = new ArrayList<>();
        
        if (level < 0)
            return result;
        
        if (level == 0)
            return head.getChilds();
        
        if (level > 0) {
            for (TimeNode node : head.getChilds()) {
                result.addAll(getNodesAtLevel(node, level-1));
            }
        }
        
        return result;
    }
    
    public boolean aggregateNodeBestCoveredAtLevel(TimeNode head, int level) {
        ArrayList<TimeNode> nodes = TimeNode.getPartialCoveredNodesAtLevel(head, level);
        nodes.sort(childsCountComparator());
        
        if (nodes.size() > 0) {
            //System.out.println("Level " + level+" Aggregable node: " + nodes);
            nodes.get(0).aggregate();
            return true;
        }
        
        return false;
    }
    
    private Comparator<TimeNode> childsCountComparator(){
        return new Comparator<TimeNode>() {
            @Override
            public int compare(TimeNode o1, TimeNode o2) {
                if ( o1.getChilds().size() < o2.getChilds().size() )
                    return 1;
                else if ( o1.getChilds().size() > o2.getChilds().size() )
                    return -1;
                else
                    return 0;
            }
        };
    }
    
    
    public void aggregate() {
        childs = new HashMap<String, TimeNode>();
        isFull = true;
        
        long base = stopTime-startTime;
        
        if ( (base > 1 && base < 10) )
            base = 10;
        if ( (base > 10 && base < 100) )
            base = 100;
        if ( (base > 100 && base < 1000) )
            base = 1000;
        if ( (base > 1000 && base < 10000) )
            base = 10000;
        
        if (startTime % base != 0)
            startTime = startTime - (startTime % base);
        
        if (stopTime % base != 0)
            stopTime  = stopTime - (stopTime % base)+base;
    }
    
    
    @Override
    public String toString() {
        String treeString = "["+startTime + ":" + stopTime+"]";
    
        return treeString;
    }
    
    public void printTree() {
        System.out.println(this);   
        for (TimeNode node : childs.values()) {
            System.out.println("\t"+node);
            for (TimeNode node1 : node.getChilds()) {
                System.out.println("\t\t"+node1);
                for (TimeNode node2 : node1.getChilds()) {
                    System.out.println("\t\t\t"+node2);
                    for (TimeNode node3 : node2.getChilds()) {
                        System.out.println("\t\t\t\t"+node3);
                        for (TimeNode node4 : node3.getChilds()) {
                            System.out.println("\t\t\t\t\t"+node4);
                        }
                    }
                }
            }
        }   
    }

}
