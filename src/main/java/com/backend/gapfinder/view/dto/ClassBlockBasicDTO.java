package com.backend.gapfinder.dto;

import com.backend.gapfinder.enums.DayOfWeekEnum;
import com.backend.gapfinder.models.ClassBlockModel;

import lombok.Data;

import java.time.LocalTime;

@Data
public class ClassBlockBasicDTO {
	private Long id;
	private String subject;
	private String location;
	private DayOfWeekEnum dayOfWeek;
	private LocalTime startTime;
	private LocalTime endTime;

	public static ClassBlockBasicDTO fromModel(ClassBlockModel entity) {
		ClassBlockBasicDTO dto = new ClassBlockBasicDTO();
		dto.setId(entity.getId());
		dto.setSubject(entity.getSubject());
		dto.setLocation(entity.getLocation());
		dto.setDayOfWeek(entity.getDayOfWeek());
		dto.setStartTime(entity.getStartTime());
		dto.setEndTime(entity.getEndTime());
		return dto;
	}
}
