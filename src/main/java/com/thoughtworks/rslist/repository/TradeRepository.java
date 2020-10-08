package com.thoughtworks.rslist.repository;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.dto.TradeDto;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TradeRepository extends CrudRepository<TradeDto, Integer> {
    Optional<TradeDto> findTradeDtoByRanking(int ranking);
}
