package com.canadainc.intelligence.model;

/**
 * Any type of search that is done in an app. For example, looking up a hadith text. Or looking up a stop number.
 */
public class InAppSearch
{
	public String name;
	public String query;

	public InAppSearch(String name, String query) {
		super();
		this.name = name;
		this.query = query;
	}
}