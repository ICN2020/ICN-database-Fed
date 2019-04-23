package com.ogb.time;


import java.util.ArrayList;

import com.ogb.time.TimeNode.TimeContained;


public class TimeTesseler {

    public static ArrayList<TimeNode> tesselate(long startMinutes, long stopMinutes, int k) {
        
        if (startMinutes == stopMinutes)
        	stopMinutes++;
        if (startMinutes > stopMinutes)
            return new ArrayList<TimeNode>();
        
        TimeNode head = new TimeNode(startMinutes, stopMinutes);
        
        //long startTime = System.currentTimeMillis();
        tesselate(head, 4);
        //System.out.println("Tesseling Time: " + (System.currentTimeMillis()-startTime));
        
        //startTime = System.currentTimeMillis();
        ArrayList<TimeNode> list = aggregate(head, k);
        //System.out.println("Aggregation Time: " + (System.currentTimeMillis()-startTime));
        
        return list;
    }
    
    public static ArrayList<String> computeTemporalTilesSuffix(ArrayList<TimeNode> head) {
        ArrayList<String> names = new ArrayList<String>();
        for (TimeNode node : head)
            names.add(TimeUtils.computeTimeTileSuffix(node));
        return names;
    }
    
    private static void tesselate(TimeNode head, long level) {
        //System.out.println("\nTasselate time: " + head);
        if (level < 0)
            return;
        
        long step     = (long)((head.getStartTime() / (long)Math.pow(10, level)) * (long)Math.pow(10, level));
        long baseTime = head.getStartTime();
            
        while (true) {
            if (TimeNode.nodeContains(head, step) == TimeContained.kTimeContained_Between) {
                TimeNode node = head.appendChild(baseTime, step);
                baseTime = step;
                
                if (node != null && node.isFull() == false)
                    tesselate(node, level-1);
            }
            else if (TimeNode.nodeContains(head, step) == TimeContained.kTimeContained_After) {
                long prevStep = (long)(step-Math.pow(10, level));
                
                if (TimeNode.nodeContains(head, prevStep) == TimeContained.kTimeContained_Between) {    
                    TimeNode node = head.appendChild(prevStep, head.getStopTime());
                    baseTime = step;
                    
                    if (node != null && node.isFull() == false)
                        tesselate(node, level-1);
                }
//              else if (TimeNode.nodeContains(head, prevStep) == TimeContained.kTimeContained_Before) {
//                  TimeNode node = head.appendChild(prevStep, step);
//              
//                  if (node != null && node.isFull() == false)
//                      tesselate(head, level-1);   
//              }

                //Ho raggiunto la fine dell'asse temporale quindi esco dal while
                break;
            }
            
            step += Math.pow(10, level);
        }
    
        //System.out.println("\nFine:\n" + head.getChilds());
        if (head.getChilds().size() == 0)
            tesselate(head, level-1);
    }
    
    private static ArrayList<TimeNode> aggregate(TimeNode head, long k) {
        //Handle max resolution node k-overflow
        if (head.getFullCoveredChilds().size() > k) {
            for (TimeNode node : head.getChilds())
                node.aggregate();

            if ( head.getChilds().size() > k) {
                if (head.getStopTime()-head.getStartTime()<10000) {
                    head.aggregate();
                
                    ArrayList<TimeNode> result = new ArrayList<TimeNode>();
                    result.add(head);
                    return result;
                }
            }
            return head.getChilds();
        }
        ArrayList<TimeNode> leafs = head.getLeafList();
        //The current three size is in the k-range
        if (leafs.size() <= k) {
            return leafs;
        }
        
        for (int level = 4; level > -1; level--) {
            if (head.getLeafList().size() <= k) {
                ArrayList<TimeNode> result = head.getLeafList();
                return result;
            }
            
            boolean aggregate = head.aggregateNodeBestCoveredAtLevel(head, level);
            if (aggregate) {
                level++;
                //System.out.println(head.getLeafList());
            }
        }
        if (head.getLeafList().size() < k )
            return head.getLeafList();
        
        //Recheck threshold for max resolution node k-overflow
        if (head.getFullCoveredChilds().size() > k) {
            for (TimeNode node : head.getChilds())
                node.aggregate();
            
            if ( head.getChilds().size() > k) {
                if (head.getStopTime()-head.getStartTime()<10000) {
                    head.aggregate();
                
                    ArrayList<TimeNode> result = new ArrayList<TimeNode>();
                    result.add(head);
                
                    return result;
                }
            }
            
            //Return root child list because the number of child exceeded k threshold 
            return head.getChilds();
        }
        
        return head.getLeafList();
    }
}
