package com.thoughtworks.rslist.api;

import com.thoughtworks.rslist.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradeController {
    @Autowired
    TradeRepository tradeRepository;
}
