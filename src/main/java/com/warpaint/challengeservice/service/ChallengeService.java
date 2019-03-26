package com.warpaint.challengeservice.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.warpaint.challengeservice.calculator.ScenarioComposer;
import com.warpaint.challengeservice.dataprovider.YahooFinanceClient;
import com.warpaint.challengeservice.model.Asset;
import com.warpaint.challengeservice.model.Pricing;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import lombok.Getter;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.channels.Pipe;
import java.text.Bidi;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.warpaint.challengeservice.service.ChallengeService.DEFAULT_TIME_RANGE;


@Service
@Slf4j
@AllArgsConstructor
public class ChallengeService {

    static final long DEFAULT_TIME_RANGE = 5;

    private final YahooFinanceClient dataProvider;

    public List<Pricing> getHistoricalAssetData(Asset asset, Optional<String> start,  Optional<String> end) {
        DateRange range = new DateRange(start, end);

        log.info("Fetching historical price data: " + asset + " startDate: " + range.getStartDate() + " endDate:" + range.getEndDate());
        return dataProvider.fetchPriceData(asset.getSymbol(), range.getStartDate(), range.getEndDate());
    }

    public List<Pricing> getProjectedAssetData(Asset asset,  Optional<String> start,  Optional<String> end) {
        DateRange range = new DateRange(start, end);
        log.info("Generating projected price data: " + asset + " startDate: " + range.getStartDate() + " endDate:" + range.getEndDate());

        //also possible to use the REST API but that call is much slower
        List<Pricing> prices = dataProvider.fetchPriceData(asset.getSymbol(), range.getStartDate(), range.getEndDate());
        if(prices.isEmpty())
            return Collections.emptyList();

        //very likely the date already sorted if true then we can skipp this step
        prices.sort(Comparator.comparing(Pricing::getTradeDate));

        //TODO get the current price. now I assume the last element is the current price
        BigDecimal lastPrice = Iterables.getLast(prices).getClosePrice();
        //ArrayList<BigDecimal> bumps = bumps(prices);

        ScenarioComposer scnearios = ScenarioComposer.create(lastPrice, prices);

        log.info("lowest scenario: " + scnearios.lowestScenario());
        log.info("median scenario: " + scnearios.medianScenario());
        return scnearios.highestScenario();
    }
}

@Slf4j
class DateRange {
    @Getter
    private LocalDate endDate;
    @Getter
    private LocalDate startDate;

    DateRange(Optional<String> start,  Optional<String> end)
    {
        try
        {
            endDate = end.map(LocalDate::parse).orElseThrow(() -> new DateTimeException("Missing end date"));
            startDate = start.map(LocalDate::parse).orElseThrow(() -> new DateTimeException("Missing start date"));
        }
        catch(DateTimeException e)
        {
            log.error("Unknown date " + e );
            endDate = LocalDate.now();
            startDate = endDate.minusYears(DEFAULT_TIME_RANGE);
        }

    }
}
