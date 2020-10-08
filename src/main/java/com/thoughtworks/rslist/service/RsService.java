package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RsService {
  final RsEventRepository rsEventRepository;
  final UserRepository userRepository;
  final VoteRepository voteRepository;
  final TradeRepository tradeRepository;

  public RsService(RsEventRepository rsEventRepository, UserRepository userRepository, VoteRepository voteRepository, TradeRepository tradeRepository) {
    this.rsEventRepository = rsEventRepository;
    this.userRepository = userRepository;
    this.voteRepository = voteRepository;
    this.tradeRepository = tradeRepository;
  }

  public void vote(Vote vote, int rsEventId) {
    Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
    Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
    if (!rsEventDto.isPresent()
        || !userDto.isPresent()
        || vote.getVoteNum() > userDto.get().getVoteNum()) {
      throw new RuntimeException();
    }
    VoteDto voteDto =
        VoteDto.builder()
            .localDateTime(vote.getTime())
            .num(vote.getVoteNum())
            .rsEvent(rsEventDto.get())
            .user(userDto.get())
            .build();
    voteRepository.save(voteDto);
    UserDto user = userDto.get();
    user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
    userRepository.save(user);
    RsEventDto rsEvent = rsEventDto.get();
    rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
    rsEventRepository.save(rsEvent);
  }

  @Transactional //https://www.cnblogs.com/alice-cj/p/10417097.html
  public void buy(Trade trade) {
    RsEventDto rsEventDto = rsEventRepository.findById(trade.getRsEventId()).orElse(null);
    if (rsEventDto == null) {
      throw new RuntimeException();
    }
    TradeDto tradeDtoFound = tradeRepository.findTradeDtoByRanking(trade.getRanking()).orElse(null);
    TradeDto tradeDtoInput = TradeDto.builder().amount(trade.getAmount()).ranking(trade.getRanking())
        .rs_event_tdo(rsEventDto).build();
    if (tradeDtoFound == null) {
      tradeRepository.save(tradeDtoInput);
    } else if (trade.getAmount() <= tradeDtoFound.getAmount()) {
      throw new RuntimeException();
    } else {
      rsEventRepository.delete(tradeDtoFound.getRs_event_tdo());
      rsEventRepository.save(rsEventDto);
      tradeRepository.save(tradeDtoInput);
    }
  }

  public List<RsEvent> sort(List<RsEvent> list) {
    RsEvent[] rsEvents = new RsEvent[list.size()];
    List<RsEvent> noRankingList = list.stream().filter(e -> e.getRanking() == 0)
        .collect(Collectors.toList());
    List<RsEvent> rankingList = list.stream().filter(e -> e.getRanking() != 0)
        .collect(Collectors.toList());
    noRankingList.sort((o1, o2) -> o1.getVoteNum() > o2.getVoteNum() ? -1 : 1);
    rankingList.sort((o1, o2) -> o1.getRanking() > o2.getRanking() ? 1 : -1);

    rankingList.stream().forEach(e -> rsEvents[e.getRanking() - 1] = e);
    int j = 0;
    for (int i = 0; i < list.size(); i++) {
      if (rsEvents[i] == null) {
        rsEvents[i] = noRankingList.get(j);
        j++;
      }
    }
    return Stream.of(rsEvents).collect(Collectors.toList());
  }
}
