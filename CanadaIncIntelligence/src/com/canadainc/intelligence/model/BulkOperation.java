package com.canadainc.intelligence.model;

/**
 * A bulk operation is an operation when the user does a bunch of elements in bulk. For example, in Auto Block, when the choose to
 * block a bunch of addresses at once. Or block a bunch of keywords at once.
 */
public class BulkOperation
{
	public String type;
	public int count;
}