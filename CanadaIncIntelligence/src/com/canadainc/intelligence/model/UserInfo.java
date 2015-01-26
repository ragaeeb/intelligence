package com.canadainc.intelligence.model;

import java.util.ArrayList;
import java.util.List;

public class UserInfo
{
	public List<String> emails = new ArrayList<String>();
	public String imei = new String();
	public String meid = new String();
	public String pin = new String();
	public String nodeName = new String();
	public String name = new String();
	public boolean internal = false;
	
	@Override
	public int hashCode() {
		return imei.hashCode()+pin.hashCode()+meid.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof UserInfo) {
			UserInfo os = (UserInfo)obj;
			return pin.equals(os.pin) && imei.equals(os.imei) && meid.equals(os.meid);
		}
		
		return false;
	}
}