package com.canadainc.intelligence.client;

public class QuranBookmark
{
	public int chapter;
	public int verse;
	public String name = new String();

	public QuranBookmark(int chapter, int verse)
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
		if (obj instanceof QuranBookmark) {
			QuranBookmark b = (QuranBookmark)obj;
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