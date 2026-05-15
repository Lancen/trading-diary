package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class BackfillRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dataType;

    private String exchange;

    private LocalDate startDate;

    private LocalDate endDate;
}
