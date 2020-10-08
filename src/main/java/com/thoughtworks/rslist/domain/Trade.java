package com.thoughtworks.rslist.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Trade {
    private double amount;
    private int ranking;
    private int rsEventId;
}