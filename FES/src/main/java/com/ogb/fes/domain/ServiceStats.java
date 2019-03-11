package com.ogb.fes.domain;

import com.ogb.fes.utils.Utils;


public class ServiceStats {
	long   queryNameCount;
	double tessellingTime;
	double ndnRequestTime;
	int    tilesCount;
	int    tilesWithDataCount;
	double requestArea;
	double responseArea;
	double requestTime;
	double postProcessingTime;
	double parsingTime;
	double intersectTime;
	

	//Constructor
	public ServiceStats() {
		queryNameCount = 0;
		
		tessellingTime = 0;
		ndnRequestTime = 0;
		
		tilesCount         = 0;
		tilesWithDataCount = 0;
		
		requestArea  = 0;
		responseArea = 0;
		
		parsingTime = 0;
		intersectTime=0;
	}


	public long getQueryNameCount() {
		return queryNameCount;
	}
	public double getTessellingTime() {
		return tessellingTime;
	}
	public double getNdnRequestTime() {
		return ndnRequestTime;
	}
	public int    getTilesCount() {
		return tilesCount;
	}
	public int    getTilesWithDataCount() {
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
	public double getParsingTime() {
		return parsingTime;
	}
	public double getIntersectTime() {
		return intersectTime;
	}


	public void setQueryNameCount(long queryNameCount) {
		this.queryNameCount = queryNameCount;
	}
	public void setTessellingTime(double tilesComputedTime) {
		this.tessellingTime = Utils.floor10(tilesComputedTime, 0);
	}
	public void setNdnRequestTime(double ndnRequestTime) {
		this.ndnRequestTime = Utils.floor10(ndnRequestTime, 0);
	}
	public void setTilesCount(int tilesCount) {
		this.tilesCount = tilesCount;
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
	public void setParsingTime(double parsingTime) {
		this.parsingTime = parsingTime;
	}
	public void setIntersectTime(double intersectTime) {
		this.intersectTime = intersectTime;
	}
	
	

	@Override
	public String toString() {
		return "ServiceStats [parsingTime=" + parsingTime + ", requestTime=" + requestTime + ", intersectTime=" + intersectTime + ", tilesComputedTime=" + tessellingTime + ", ndnRequestTime=" + ndnRequestTime
				+ ", queryNameCount=" +queryNameCount+ ", tilesCount=" + tilesCount + ", requestArea=" + requestArea + ", responseArea="+ responseArea + "]";
	}

}
