package com.canadainc.intelligence.client;

public class Bookmark
{
	public int chapter;
	public int verse;

	public Bookmark(int chapter, int verse)
	{
		this.chapter = chapter;
		this.verse = verse;
	}

	@Override
	public int hashCode() {
		return chapter*31+verse;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Bookmark) {
			Bookmark b = (Bookmark)obj;
			return b.chapter == chapter && b.verse == verse;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return chapter+":"+verse;
	}
}