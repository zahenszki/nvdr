package com.warpaint.challengeservice.service;

import com.warpaint.challengeservice.dataprovider.YahooFinanceClient;
import com.warpaint.challengeservice.model.Asset;
import com.warpaint.challengeservice.model.Pricing;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import lombok.Getter;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    public List<Pricing> getProjectedAssetData(Asset asset) {
        log.info("Generating projected price data");
        // TODO Implement getProjectedAssetData()
        return null;
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
