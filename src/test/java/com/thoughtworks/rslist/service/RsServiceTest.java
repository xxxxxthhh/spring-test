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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class RsServiceTest {
    RsService rsService;

    @Mock
    RsEventRepository rsEventRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    VoteRepository voteRepository;
    @Mock
    TradeRepository tradeRepository;
    LocalDateTime localDateTime;
    Vote vote;

    @BeforeEach
    void setUp() {
        initMocks(this);
        rsService = new RsService(rsEventRepository, userRepository, voteRepository, tradeRepository);
        localDateTime = LocalDateTime.now();
        vote = Vote.builder().voteNum(2).rsEventId(1).time(localDateTime).userId(1).build();
    }

    @Test
    void shouldVoteSuccess() {
        // given

        UserDto userDto =
                UserDto.builder()
                        .voteNum(5)
                        .phone("18888888888")
                        .gender("female")
                        .email("a@b.com")
                        .age(19)
                        .userName("xiaoli")
                        .id(2)
                        .build();
        RsEventDto rsEventDto =
                RsEventDto.builder()
                        .eventName("event name")
                        .id(1)
                        .keyword("keyword")
                        .voteNum(2)
                        .user(userDto)
                        .build();

        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
        // when
        rsService.vote(vote, 1);
        // then
        verify(voteRepository)
                .save(
                        VoteDto.builder()
                                .num(2)
                                .localDateTime(localDateTime)
                                .user(userDto)
                                .rsEvent(rsEventDto)
                                .build());
        verify(userRepository).save(userDto);
        verify(rsEventRepository).save(rsEventDto);
    }

    @Test
    void shouldThrowExceptionWhenUserNotExist() {
        // given
        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
        //when&then
        assertThrows(
                RuntimeException.class,
                () -> {
                    rsService.vote(vote, 1);
                });
    }

    @Test
    void when_buy_rs_and_no_ranking_bought_then_add_trade() {
        UserDto userDto = UserDto.builder().voteNum(5).phone("18888888888").gender("female")
                .email("a@b.com").age(19).userName("xiaoli").id(2).build();
        RsEventDto rsEventDto = RsEventDto.builder().eventName("event name").id(1)
                .keyword("keyword").voteNum(2).user(userDto).build();
        TradeDto tradeDto = TradeDto.builder().amount(100).ranking(1)
                .rs_event_tdo(rsEventDto).build();
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
        when(tradeRepository.findById(anyInt())).thenReturn(Optional.of(tradeDto));

        Trade trade = Trade.builder().amount(tradeDto.getAmount()).rsEventId(1)
                .ranking(tradeDto.getRanking()).build();

        userRepository.save(userDto);
        rsEventRepository.save(rsEventDto);
        rsService.buy(trade);

        verify(tradeRepository).save(tradeDto);
    }

    @Test
    public void when_buy_rs_and_ranking_bought_and_amount_insufficient_then_runtime_exception() {
        UserDto userDto = UserDto.builder().voteNum(5).phone("18888888888").gender("female")
                .email("a@b.com").age(19).userName("xiaoli").id(2).build();
        RsEventDto rsEventDto = RsEventDto.builder().eventName("event name").id(1)
                .keyword("keyword").voteNum(2).user(userDto).build();
        TradeDto tradeDto = TradeDto.builder().amount(200).ranking(1)
                .rs_event_tdo(rsEventDto).build();
        TradeDto tradeDto2 = TradeDto.builder().amount(100).ranking(1)
                .rs_event_tdo(rsEventDto).build();
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
        when(tradeRepository.findTradeDtoByRanking(anyInt())).thenReturn(Optional.of(tradeDto));


        Trade trade = Trade.builder().amount(100).rsEventId(1)
                .ranking(1).build();
        assertThrows(
                RuntimeException.class,
                () -> {
                    rsService.buy(trade);
                });
    }

  @Test
  public void when_buy_rs_and_ranking_bought_and_amount_sufficient_then_update_old_trade() {
    UserDto userDto = UserDto.builder().voteNum(5).phone("18888888888").gender("female")
        .email("a@b.com").age(19).userName("xiaoli").id(2).build();
    RsEventDto rsEventDto = RsEventDto.builder().eventName("event name").id(1)
        .keyword("keyword").voteNum(2).user(userDto).build();
    TradeDto tradeDtoOld = TradeDto.builder().amount(200).ranking(1)
        .rs_event_tdo(rsEventDto).build();
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
    when(tradeRepository.findTradeDtoByRanking(anyInt())).thenReturn(Optional.of(tradeDtoOld));

    Trade tradeNew = Trade.builder().amount(300).ranking(1).rsEventId(1).build();
    TradeDto tradeDtoNew = TradeDto.builder()
        .amount(tradeNew.getAmount()).ranking(tradeNew.getRanking())
        .rs_event_tdo(rsEventDto).build();
    rsService.buy(tradeNew);

    verify(rsEventRepository).delete(tradeDtoOld.getRs_event_tdo());
    verify(rsEventRepository).save(tradeDtoNew.getRs_event_tdo());
    verify(tradeRepository).save(tradeDtoNew);
  }

  @Test
  public void should_sort_rs_list_by_vote_num_and_trade_ranking() {
    RsEvent rsEvent1 = RsEvent.builder().eventName("ttt").keyword("ttt").voteNum(5).build();
    RsEvent rsEvent2 = RsEvent.builder().eventName("qqq").keyword("qqq").voteNum(2).build();
    RsEvent rsEvent3 = RsEvent.builder().eventName("eee").keyword("eee").voteNum(3).ranking(1).build();
    RsEvent rsEvent4 = RsEvent.builder().eventName("rrr").keyword("rrr").voteNum(9).ranking(3).build();
    RsEvent rsEvent5 = RsEvent.builder().eventName("yyy").keyword("yyy").voteNum(8).build();
    List<RsEvent> list = new ArrayList<RsEvent>();
    list.add(rsEvent1);
    list.add(rsEvent2);
    list.add(rsEvent3);
    list.add(rsEvent4);
    list.add(rsEvent5);
    list = rsService.sort(list);
    Assertions.assertEquals("eee", list.get(0).getEventName());
    Assertions.assertEquals("yyy", list.get(1).getEventName());
    Assertions.assertEquals("rrr", list.get(2).getEventName());
    Assertions.assertEquals("ttt", list.get(3).getEventName());
    Assertions.assertEquals("qqq", list.get(4).getEventName());

  }
}
