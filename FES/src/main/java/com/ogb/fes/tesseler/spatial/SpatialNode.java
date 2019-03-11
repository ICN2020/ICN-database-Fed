package com.ogb.fes.tesseler.spatial;


import java.util.ArrayList;
import java.util.Comparator;

import com.ogb.fes.utils.Utils;


public class SpatialNode {
	private SpatialPoint           southWest;
	private SpatialPoint           northEst;
	private boolean                isFull;
	private ArrayList<SpatialNode> children;
	private SpatialNode            father;
	private int                    level;
	
	
	public SpatialNode(SpatialPoint sw, SpatialPoint ne, int lvl) {
		super ();
		
		
		southWest = new SpatialPoint(Utils.floor10(sw.latitude, lvl), Utils.floor10(sw.longitude, lvl));
		northEst  = new SpatialPoint(Utils.ceil10(ne.latitude, lvl),  Utils.ceil10(ne.longitude, lvl));
		
		if (level == 2)
			isFull = true;
		else
			isFull = false;
		
		children = new ArrayList<SpatialNode>();
		father   = null;
		level    = lvl;
	}
	
	public SpatialNode(SpatialPoint sw, int level) {
		super();
		

		southWest = sw;
		northEst  = new SpatialPoint(sw.latitude+Math.pow(10, 2-level)/100.0, sw.longitude+Math.pow(10, 2-level)/100.0);
		
		southWest.adjustCoordToDecimalNumber(level);
		northEst.adjustCoordToDecimalNumber(level);
	
		if (level == 2)
			isFull = true;
		else
			isFull = false;
		
		children = new ArrayList<SpatialNode>();
		father   = null;
	}
	
	
	public void aggregate() {
		if (father == null)
			return;
		
		children = new ArrayList<SpatialNode>();
		isFull   = true;
	}
	
		
	//Getter Methods
	public SpatialPoint getSouthWest() {
		return southWest;
	}
	public SpatialPoint getNorthEst() {
		return northEst;
	}
	public int getLevel() {
		return level;
	}
	public boolean isFull() {
		return isFull;
	}
	public ArrayList<SpatialNode> getChildren() {
		children.sort(new Comparator<SpatialNode>() {
			@Override
			public int compare(SpatialNode o1, SpatialNode o2) {
				return o1.children.size()-o2.children.size();
			}
		});
		
		return children;
	}
	public SpatialNode getFather() {
		return father;
	}


	
	//Setter Methods
	public void setSouthWest(SpatialPoint southWest) {
		this.southWest = southWest;
	}
	public void setNorthEst(SpatialPoint northEst) {
		this.northEst = northEst;
	}
	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}
	public void setChilds(ArrayList<SpatialNode> childs) {
		this.children = childs;
	}
	public void setFather(SpatialNode father) {
		this.father = father;
	}
	public void setLevel(int level) {
		this.level = level;
	}

	
	public void appendChild(SpatialNode node) {
		node.father = this;
		children.add(node);
	}

	public boolean computeIntersection(SpatialPoint sw, SpatialPoint ne) {
		isFull = false;
		if (intersects(sw, ne) == false) 
			return false;
		
		//Intersection checked. Checking is Contained
		isFull = true;

		if (sw.latitude > southWest.latitude)
			isFull = false;
		if (sw.longitude > southWest.longitude)
			isFull = false;
		if (ne.latitude < northEst.latitude)
			isFull = false;
		if (ne.longitude < northEst.longitude)
			isFull = false;
		
		return true;
	}
	
	
	 //Rectangle utilities functions
	public boolean valueInRange(double value, double min, double max) {
		return (value > min) && (value < max);
	}
	
    public boolean intersects(SpatialPoint sw, SpatialPoint ne) {
		boolean xOverlap  = valueInRange(southWest.longitude, sw.longitude, ne.longitude) || valueInRange(sw.longitude, southWest.longitude, northEst.longitude);
		boolean x2Overlap = valueInRange(northEst.longitude, sw.longitude, ne.longitude)  || valueInRange(ne.longitude, southWest.longitude, northEst.longitude);
		
		boolean yOverlap  = valueInRange(southWest.latitude, sw.latitude, ne.latitude) || valueInRange(sw.latitude, southWest.latitude, northEst.latitude);
		boolean y2Overlap = valueInRange(northEst.latitude, sw.latitude, ne.latitude)  || valueInRange(ne.latitude, southWest.latitude, northEst.latitude);
		
		return (xOverlap || x2Overlap) && (yOverlap || y2Overlap);
    }
    
	
	public ArrayList<SpatialNode> getLeaves() {
		ArrayList<SpatialNode> res = new ArrayList<SpatialNode>();
		
		if (children.size() <= 0) {
			res.add(this);
		}
		else {
			for (SpatialNode node : children) {
				res.addAll(node.getLeaves());
			}
		}
		
		return res;
	}
	
	@Override
	public String toString() {
		return "sw: " + southWest + " ne: " + northEst + " isFull: " + isFull + "\n";
	}
}
