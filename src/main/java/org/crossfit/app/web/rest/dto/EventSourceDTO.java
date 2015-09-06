package org.crossfit.app.web.rest.dto;

import java.util.List;

import org.crossfit.app.domain.util.CustomDateTimeSerializer;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class EventSourceDTO {

	private List<TimeSlotEventDTO> events;

	private String color;
	boolean editable = true;

	public List<TimeSlotEventDTO> getEvents() {
		return events;
	}

	public void setEvents(List<TimeSlotEventDTO> events) {
		this.events = events;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

}
