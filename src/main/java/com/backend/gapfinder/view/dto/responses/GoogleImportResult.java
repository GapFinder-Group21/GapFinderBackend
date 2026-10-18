package com.backend.gapfinder.view.dto.responses;

import java.util.List;

import com.backend.gapfinder.view.dto.ClassBlockBasicDTO;

public record GoogleImportResult(List<ClassBlockBasicDTO> created, int skippedDuplicates) {
}
