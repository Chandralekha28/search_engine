package com.entity;

public class AdData {

	String url;
	String imageUrl;
	float budget;
	float billAfterClicks;
	String nGrams;
	String text;
	int score;
	
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	public float getBudget() {
		return budget;
	}
	public void setBudget(float budget) {
		this.budget = budget;
	}
	public float getBillAfterClicks() {
		return billAfterClicks;
	}
	public void setBillAfterClicks(float billAfterClicks) {
		this.billAfterClicks = billAfterClicks;
	}
	public String getnGrams() {
		return nGrams;
	}
	public void setnGrams(String nGrams) {
		this.nGrams = nGrams;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	
}
