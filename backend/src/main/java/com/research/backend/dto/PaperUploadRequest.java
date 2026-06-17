package com.research.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperUploadRequest {

    @NotBlank(message = "论文标题不能为空")
    private String title;

    private String paperAbstract;

    private List<String> keywords;

    private LocalDate publishDate;

    private List<String> authors;

    private List<String> innovationPoints;

    private List<String> methods;

    private List<String> datasets;
}
