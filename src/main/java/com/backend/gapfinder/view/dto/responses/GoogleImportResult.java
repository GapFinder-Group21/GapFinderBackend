package com.backend.gapfinder.dto.responses;

import java.util.List;

import com.backend.gapfinder.dto.ClassBlockBasicDTO;

public record GoogleImportResult(List<ClassBlockBasicDTO> created, int skippedDuplicates) {
}
