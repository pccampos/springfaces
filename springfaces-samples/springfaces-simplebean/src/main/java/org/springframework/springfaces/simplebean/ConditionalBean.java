package org.springframework.springfaces.simplebean;

import java.io.Serializable;

public class ConditionalBean implements Serializable {
	private static final long serialVersionUID = 8873177301577325126L;
	
	private String firstMessage;
	private String secondMessage;
	private String thirdMessage;

	public String getFirstMessage() {
		return firstMessage;
	}

	public void setFirstMessage(String firstMessage) {
		this.firstMessage = firstMessage;
	}

	public String getSecondMessage() {
		return secondMessage;
	}

	public void setSecondMessage(String secondMessage) {
		this.secondMessage = secondMessage;
	}

	public String getThirdMessage() {
		return thirdMessage;
	}

	public void setThirdMessage(String thirdMessage) {
		this.thirdMessage = thirdMessage;
	}

}
