package com.canadainc.intelligence.model;


public class Location
{
	@Override
	public int hashCode()
	{
		return city.hashCode() + country.hashCode() + region.hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Location) {
			Location os = (Location)obj;
			return city.equals(os.city) && country.equals(os.country) && region.equals(os.region);
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return city+","+region+","+country;
	}

	public String city = new String();
	public String country = new String();
	public double latitude;
	public String location = new String();
	public double longitude;
	public String name = new String();
	public String region = new String();
}