package com.ogb.fes.tesseler.temporal;


import java.util.ArrayList;

import com.ogb.fes.tesseler.temporal.TemporalNode.TimeContained;


public class TemporalTesseler {

    public static ArrayList<TemporalNode> tesselate(long startMinutes, long stopMinutes, int k) {
        
        if (startMinutes == stopMinutes)
        	stopMinutes++;
        if (startMinutes > stopMinutes)
            return new ArrayList<TemporalNode>();
        
        TemporalNode head = new TemporalNode(startMinutes, stopMinutes);
       
        tesselate(head, 4);
       
        return aggregate(head, k);
    }
    
    public static ArrayList<String> computeTemporalTilesSuffix(ArrayList<TemporalNode> nodes) {
        
    	ArrayList<String> names = new ArrayList<String>();
        for (TemporalNode node : nodes) {
            names.add(TemporalUtils.computeTimeTileSuffix(node));
        }
        
        return names;
    }
    
    private static void tesselate(TemporalNode head, long level) {
        
        long step     = (long)((head.getStartTime() / (long)Math.pow(10, level)) * (long)Math.pow(10, level));
        long baseTime = head.getStartTime();
            
        while (true) {
            if (TemporalNode.nodeContains(head, step) == TimeContained.kTimeContained_Between) {
                TemporalNode node = head.appendChild(baseTime, step);
                baseTime = step;
                
                if (node != null && node.isFull() == false && level-1 >= 0) {
                    tesselate(node, level-1);
                }
            }
            else if (TemporalNode.nodeContains(head, step) == TimeContained.kTimeContained_After) {
                long prevStep = (long)(step-Math.pow(10, level));
                
                if (TemporalNode.nodeContains(head, prevStep) == TimeContained.kTimeContained_Between) {    
                    TemporalNode node = head.appendChild(prevStep, head.getStopTime());
                    baseTime = step;
                    
                    if (node != null && node.isFull() == false && level-1 >= 0) {
                        tesselate(node, level-1);
                    }
                }

                //Ho raggiunto la fine dell'asse temporale quindi esco dal while
                break;
            }
            
            step += Math.pow(10, level);
        }
    
        //System.out.println("\nFine:\n" + head.getChilds());
        if (head.getChilds().size() == 0)
            tesselate(head, level-1);
    }
    
    private static ArrayList<TemporalNode> aggregate(TemporalNode head, long k) {
        //Handle max resolution node k-overflow
        if (head.getFullCoveredChilds().size() > k) {
            for (TemporalNode node : head.getChilds()) {
                node.aggregate();
            }
            
            if (head.getChilds().size() > k) {
                if (head.getStopTime()-head.getStartTime() < 10000) {
                    head.aggregate();
                
                    ArrayList<TemporalNode> result = new ArrayList<TemporalNode>();
                    result.add(head);
                    return result;
                }
            }
            
            return head.getChilds();
        }
        
        ArrayList<TemporalNode> leafs = head.getLeafList();
        //The current tree size is in the k-range
        if (leafs.size() <= k) {
            return leafs;
        }
        
        for (int level = 4; level > -1; level--) {
            if (head.getLeafList().size() <= k) {
                ArrayList<TemporalNode> result = head.getLeafList();
                return result;
            }
            
            boolean aggregate = head.aggregateNodeBestCoveredAtLevel(head, level);
            if (aggregate) {
                level++;
            }
        }
        if (head.getLeafList().size() < k)
            return head.getLeafList();
        
        //Recheck threshold for max resolution node k-overflow
        if (head.getFullCoveredChilds().size() > k) {
            for (TemporalNode node : head.getChilds()) {
                node.aggregate();
        	}
            
            if ( head.getChilds().size() > k) {
                if (head.getStopTime()-head.getStartTime() < 10000) {
                    head.aggregate();
                
                    ArrayList<TemporalNode> result = new ArrayList<TemporalNode>();
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
