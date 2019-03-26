package com.warpaint.challengeservice.calculator;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.warpaint.challengeservice.model.Pricing;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ScenarioComposer {
    public static final int TIME_PROJECTION_IN_MONTH = 20 * 12;
    private static final int MAX_SCENARIOS = 1000;

    private final TreeSet<ArrayList<Pricing>> scenarios;
    private final int numberOfMonths;

    public static ScenarioComposer create(final BigDecimal currentPrice, final List<Pricing> prices, final int numberOfMonths)
    {
      return new ScenarioComposer(currentPrice, prices, numberOfMonths);
    }

    private ScenarioComposer(final BigDecimal currentPrice, final List<Pricing> prices, final int numberOfMonths)
    {
        this.numberOfMonths = numberOfMonths;
        scenarios = createScenarios(currentPrice, calcBumps(prices));
    }

    public List<Pricing> highestScenario()
    {
        return scenarios.last();
    }

    public List<Pricing> lowestScenario()
    {
        return scenarios.first();
    }

    public List<Pricing> medianScenario()
    {
        Iterator<ArrayList<Pricing>> iter = scenarios.iterator();
        for(int i = 0; i < scenarios.size()/2; ++i) {
             iter.next();
        }
        return iter.next();
    }

    protected TreeSet<ArrayList<Pricing>> createScenarios(final BigDecimal currentPrice, final ArrayList<BigDecimal> bumps) {

        try {
            return IntStream.rangeClosed(1, MAX_SCENARIOS).mapToObj(i -> scenario(currentPrice, bumps))
                    .collect(Collectors.toCollection(
                            () -> new TreeSet<>(Comparator.comparing(s -> Iterables.getLast(s).getClosePrice()))
                    ));
        } catch (NoSuchElementException e)
        {
            log.error("ooops " +  e);
            return new TreeSet<>();
        }

    }


    protected ArrayList<Pricing> scenario(final BigDecimal price, final ArrayList<BigDecimal> bumps)
    {
        Random rand = new Random();
        ArrayList<Pricing> list = Lists.newArrayList();
        if(bumps.isEmpty())
            return  list;
        LocalDate date = LocalDate.now();
        BigDecimal currentPrice = price;
        for(int i=0; i< numberOfMonths; ++i)
        {
            currentPrice = currentPrice.add(bumps.get(rand.nextInt(bumps.size())));
            date = date.plusMonths(1);
            list.add(Pricing.builder().closePrice(currentPrice).tradeDate(date).build());
        }
        return list;
    }

    protected ArrayList<BigDecimal> calcBumps(List<Pricing> prices)
    {
        ArrayList<BigDecimal> priceBumps = Lists.newArrayList();
        Pricing lastMonthPrice = null;
        for(Pricing p : prices)
        {
            if(lastMonthPrice == null) {
                lastMonthPrice = p;
                continue;
            }

            if(p.getTradeDate().getMonthValue() != lastMonthPrice.getTradeDate().getMonthValue())
            {
                priceBumps.add(p.getClosePrice().subtract(lastMonthPrice.getClosePrice()));
                lastMonthPrice = p;
            }
        }

        return priceBumps;
    }
}
