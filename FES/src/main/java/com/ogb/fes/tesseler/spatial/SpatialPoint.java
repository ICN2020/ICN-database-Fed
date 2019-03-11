package com.ogb.fes.tesseler.spatial;


import com.ogb.fes.utils.Utils;


public class SpatialPoint {
	public double latitude;
	public double longitude;
	
	
	public SpatialPoint() {
		super();
		
		this.latitude  = -Double.MAX_VALUE;
		this.longitude = -Double.MAX_VALUE;
	}
	
	public SpatialPoint(double latitude, double longitude) {
		super();
		
		this.latitude  = latitude;
		this.longitude = longitude;
	}



	//Getter Method
	public double getLatitude() {
		return latitude;
	}
	public double getLongitude() {
		return longitude;
	}

	//Setter Method
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public void setLatitudeAndCeilToDecimal(double latitude, int decimal) {
		this.latitude = Utils.ceil10(latitude, decimal);
	}
	public void setLatitudeAndFloorToDecimal(double latitude, int decimal) {
		this.latitude = Utils.floor10(latitude, decimal);
	}
	
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public void setLongitudeAndCeilToDecimal(double longitude, int decimal) {
		this.longitude = Utils.ceil10(longitude, decimal);
	}
	public void setLongitudeAndFloorToDecimal(double longitude, int decimal) {
		this.longitude = Utils.floor10(longitude, decimal);
	}
	
	
	public void adjustCoordToDecimalNumber(int decimalDigit) {
		setLatitudeAndCeilToDecimal(this.latitude, decimalDigit);
		setLongitudeAndFloorToDecimal(this.longitude, decimalDigit);
	}
	
	
	@Override
	public SpatialPoint clone() {
		return new SpatialPoint(latitude, longitude);
	}
	
	@Override
	public String toString() {
		return "{latitude=" + latitude + "; longitude="+ longitude + "}";
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpatialPoint other = (SpatialPoint) obj;
		if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
			return false;
		if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
			return false;
		
		return true;
	}
}
