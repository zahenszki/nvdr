package com.warpaint.challengeservice.calculator;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.warpaint.challengeservice.model.Pricing;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScenarioComposerUnitTest {

    @Test
    public void equalScenario()
    {
        Pricing one = Pricing.builder().closePrice(new BigDecimal(50))
                                       .tradeDate(LocalDate.parse("2018-10-10"))
                                       .build();
        Pricing two = Pricing.builder().closePrice(new BigDecimal(60))
                .tradeDate(LocalDate.parse("2018-12-10"))
                .build();

        BigDecimal currentPrice = new BigDecimal(100);
        ScenarioComposer scen = ScenarioComposer.create(currentPrice, Arrays.asList(one,two), 2);

        assertEquals(scen.highestScenario(),scen.lowestScenario());
        assertEquals(scen.medianScenario(),scen.lowestScenario());
    }

    @Test
    public void complexScenario()
    {
        Pricing a = Pricing.builder().closePrice(new BigDecimal(50))
                .tradeDate(LocalDate.parse("2018-10-10"))
                .build();
        Pricing b = Pricing.builder().closePrice(new BigDecimal(60))
                .tradeDate(LocalDate.parse("2018-12-10"))
                .build();

        Pricing c = Pricing.builder().closePrice(new BigDecimal(45))
                .tradeDate(LocalDate.parse("2019-01-10"))
                .build();

        Pricing d = Pricing.builder().closePrice(new BigDecimal(30))
                .tradeDate(LocalDate.parse("2019-02-10"))
                .build();

        BigDecimal currentPrice = new BigDecimal(100);
        ScenarioComposer scen = ScenarioComposer.create(currentPrice, Arrays.asList(a,b,c,d), 2);

        assertTrue( Iterables.getLast(scen.highestScenario()).getClosePrice()
                    .compareTo(Iterables.getLast(scen.medianScenario()).getClosePrice())
                    >= 0);

        assertTrue( Iterables.getLast(scen.medianScenario()).getClosePrice()
                .compareTo(Iterables.getLast(scen.lowestScenario()).getClosePrice())
                >= 0);
    }

    @Test
    public void bumpsTest()
    {
        Pricing a = Pricing.builder().closePrice(new BigDecimal(50))
                .tradeDate(LocalDate.parse("2018-10-10"))
                .build();
        Pricing b = Pricing.builder().closePrice(new BigDecimal(60))
                .tradeDate(LocalDate.parse("2018-12-10"))
                .build();

        Pricing c = Pricing.builder().closePrice(new BigDecimal(45))
                .tradeDate(LocalDate.parse("2019-01-10"))
                .build();

        Pricing d = Pricing.builder().closePrice(new BigDecimal(30))
                .tradeDate(LocalDate.parse("2019-02-10"))
                .build();

        ScenarioComposer scen = ScenarioComposer.create(new BigDecimal(1), Arrays.asList(), 0);
        ArrayList<BigDecimal> bumps = scen.calcBumps(Arrays.asList(a, b, c, d));

        assertEquals(Arrays.asList( b.getClosePrice().subtract(a.getClosePrice()),
                                    c.getClosePrice().subtract(b.getClosePrice()),
                                    d.getClosePrice().subtract(c.getClosePrice())
                                   ),
                     bumps);
    }

    @Test
    public void scenarioTest()
    {
        BigDecimal curr = new BigDecimal(100);
        ArrayList<BigDecimal>  bumps = Lists.newArrayList();
        bumps.add(new BigDecimal(1));

        ScenarioComposer scen = ScenarioComposer.create(new BigDecimal(1), Arrays.asList(), 3);
        ArrayList<Pricing> s = scen.scenario(curr, bumps);

        Pricing a = Pricing.builder().closePrice(curr.add(new BigDecimal(1)))
                .tradeDate(LocalDate.now().plusMonths(1))
                .build();
        Pricing b = Pricing.builder().closePrice(curr.add(new BigDecimal(2)))
                .tradeDate(LocalDate.now().plusMonths(2))
                .build();

        Pricing c = Pricing.builder().closePrice(curr.add(new BigDecimal(3)))
                .tradeDate(LocalDate.now().plusMonths(3))
                .build();

        assertEquals(Arrays.asList(a,b,c), s);
    }
}
