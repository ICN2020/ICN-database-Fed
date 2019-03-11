package com.ogb.fes.utils;

public class Stats {
	double tilesComputedTime;
	double ndnRequestTime;
	int    objCount;
	int    tilesWithDataCount;
	double requestArea;
	double responseArea;
	double requestTime;
	double postProcessingTime;
		

	//Constructor
	public Stats() {
		tilesComputedTime  = 0;
		ndnRequestTime     = 0;
		
		objCount         = 0;
		tilesWithDataCount = 0;
		
		requestArea  = 0;
		responseArea = 0;
	}


	public double getTilesComputedTime() {
		return tilesComputedTime;
	}
	public double getNdnRequestTime() {
		return ndnRequestTime;
	}
	public int getObjCount() {
		return objCount;
	}
	public int getTilesWithDataCount() {
		return tilesWithDataCount;
	}
	public double getRequestArea() {
		return requestArea;
	}
	public double getResponseArea() {
		return responseArea;
	}
	public double getRequestTime() {
		return requestTime;
	}
	public double getPostProcessingTime() {
		return postProcessingTime;
	}


	public void setTilesComputedTime(double tilesComputedTime) {
		this.tilesComputedTime = Utils.floor10(tilesComputedTime, 0);
	}
	public void setNdnRequestTime(double ndnRequestTime) {
		this.ndnRequestTime = Utils.floor10(ndnRequestTime, 0);
	}
	public void setObjCount(int tilesCount) {
		this.objCount = tilesCount;
	}
	public void setTilesWithDataCount(int tilesWithDataCount) {
		this.tilesWithDataCount = tilesWithDataCount;
	}
	public void setRequestTime(double requestTimeMillis) {
		this.requestTime = requestTimeMillis;
	}
	public void setRequestArea(double requestArea) {
		this.requestArea = Utils.floor10(requestArea, 2);
	}
	public void setResponseArea(double responseArea) {
		this.responseArea = Utils.floor10(responseArea, 2);
	}
	public void setPostProcessingTime(double postProcessingTime) {
		this.postProcessingTime = postProcessingTime;
	}
	
	

	@Override
	public String toString() {
		return "ServiceStats [requestTime=" + requestTime + ", postProcessingTime=" + postProcessingTime +
							 ", tilesComputedTime=" + tilesComputedTime + ", ndnRequestTime=" + ndnRequestTime+
							 "]";
	}

}
