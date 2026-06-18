package com.research.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperDesensitizedDto {

    private Long paperId;

    private String desensitizedTitle;

    private String desensitizedAbstract;

    private String desensitizedAuthors;

    private String desensitizedAffiliation;

    private boolean hasOriginalAccess;
}
