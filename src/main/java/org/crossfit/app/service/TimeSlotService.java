package org.crossfit.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.crossfit.app.domain.ClosedDay;
import org.crossfit.app.domain.TimeSlot;
import org.crossfit.app.domain.enumeration.Level;
import org.crossfit.app.repository.ClosedDayRepository;
import org.crossfit.app.repository.TimeSlotRepository;
import org.crossfit.app.web.rest.dto.TimeSlotInstanceDTO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import scala.reflect.internal.util.Statistics.TimerStack;

@Service
@Transactional
public class TimeSlotService {

	private final Logger log = LoggerFactory.getLogger(TimeSlotService.class);

    @Inject
    private CrossFitBoxSerivce boxService;
    
    @Inject
    private ClosedDayRepository closedDayRepository;

    @Inject
    private TimeSlotRepository timeSlotRepository;
    
	/**
	 * Renvoie toutes les créneau horaire entre start et end, en tenant compte des jours de fermeture
	 * @param start
	 * @param end
	 * @return
	 */
	public List<TimeSlotInstanceDTO> findAllTimeSlotInstance(DateTime start, DateTime end){

		List<TimeSlotInstanceDTO> timeSlotInstances = new ArrayList<>();
		
		if (end.isBefore(start)){
			return timeSlotInstances;
		}
		
		List<TimeSlot> allSlots = timeSlotRepository.findAll();
		List<ClosedDay> closedDays = closedDayRepository.findAllByBoxAndBetween(boxService.findCurrentCrossFitBox().get(), start, end);
		
		while(!start.isAfter(end)){
			final DateTime startF = start;
			List<TimeSlotInstanceDTO> slotInstanceOfDay = allSlots.stream()
				.filter( slot -> { return slot.getDayOfWeek() == startF.getDayOfWeek(); })
				.map(slot -> {return new TimeSlotInstanceDTO(startF, slot);})
				.collect(Collectors.toList());
			
			timeSlotInstances.addAll(slotInstanceOfDay);
			
			start = start.plusDays(1);
		}
		start = null;
		//Attention la variable start a changé ici !!! elle n'est plus réutilisable
    	
    	List<TimeSlotInstanceDTO> timeSlotInstanceWithoutClosedDay = timeSlotInstances.stream()
    			.filter(slotInstance -> slotNotInAnCloseDay(slotInstance, closedDays))
				.collect(Collectors.toList());
    	
    	return timeSlotInstanceWithoutClosedDay;
	}
	

    
	private boolean slotNotInAnCloseDay(TimeSlotInstanceDTO slot, List<ClosedDay> closedDays) {
		Optional<ClosedDay> closedDayContainingSlot = closedDays.stream()
				.filter( closeDay -> closeDay.contain(slot.getStart()) || 
						( closeDay.contain(slot.getEnd()) && !closeDay.getStartAt().isEqual(slot.getEnd())) ).findFirst();
		return ! closedDayContainingSlot.isPresent();
	}
}