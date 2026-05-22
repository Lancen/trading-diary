package com.tradingdiary.collection.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CollectionStatusVO {

    private String dataType;

    private String dataTypeLabel;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private JobStatus lastFetch;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private JobStatus lastCleanse;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private java.time.LocalDateTime lastDataDate;

    @Getter
    @Setter
    public static class JobStatus {

        private String status;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime startedAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime completedAt;

        private Integer recordCount;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String errorMsg;
    }
}
