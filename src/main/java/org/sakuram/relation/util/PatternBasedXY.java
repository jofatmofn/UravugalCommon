package org.sakuram.relation.util;

import org.springframework.stereotype.Component;

@Component
public class PatternBasedXY {
	private int x, y, ind, incr;
	private String pattern;
	
	/*
	 * Origin at top left; x increases when moved towards right; y increases when moved downwards
	 * Pattern is made-up of characters R, L, D, U 
	 */
	
	public void init(String pattern, int initX, int initY, int incr) {
		this.pattern = pattern;
		x = initX;
		y = initY;
		this.incr = incr;
		ind = -2;
	}
	
	public XY getNextXY() {
		ind++;
		if (ind >= 0) {
			if (ind == pattern.length()) {
				ind = 0;
			}
			switch(pattern.charAt(ind)) {
			case 'R':
				x = x + incr;
				break;
			case 'L':
				x = x - incr;
				break;
			case 'U':
				y = y + incr;
				break;
			case 'D':
				y = y - incr;
				break;
			}
		}
		return new XY(x, y);
	}
	
    public class XY {
    	public int x;
    	public int y;
    	
    	public XY(int x, int y) {
    		this.x = x;
    		this.y = y;
    	}
    }

}
