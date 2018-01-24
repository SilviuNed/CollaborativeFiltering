package com.silviuned.model;

public class UserRating {
	
	private int userId;
	private short rating;
	private String date;
	
	public UserRating(int userId, short rating, String date) {
		this.userId = userId;
		this.rating = rating;
		this.date = date;
	}
	
	public int getUserId() {
		return userId;
	}
	public short getRating() {
		return rating;
	}
	public String getDate() {
		return date;
	}
}
