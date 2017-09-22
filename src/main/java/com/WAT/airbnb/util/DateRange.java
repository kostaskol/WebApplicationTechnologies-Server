package com.WAT.airbnb.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 *  Creates a list of java.time.LocalDate objects that are between 
 *  the two given dates
 *  i.e.:
 *  <code>
 *  this.start = new java.time.LocalDate(2017, Month.MAY, 1);
 *  this.end = new java.time.LocalDate(2017, Month.MAY, 4);
 *  DateRange$ToList() -> ArrayList {1-5-2017, 2-5-2017, 3-5-2017, 4-5-20171}
 *  </code>
 *  @author Kostas Kolivas
 *  @version 1.0
 */
public class DateRange implements Iterable<LocalDate> {
    private final LocalDate start;
    private final LocalDate end;

    public DateRange(java.sql.Date start, java.sql.Date end) {
        this.start = start.toLocalDate();
        this.end = end.toLocalDate();
    }

    @Override
    public Iterator<LocalDate> iterator() {
        return stream().iterator();
    }

    private Stream<LocalDate> stream() {
        return Stream.iterate(start, d -> d.plusDays(1))
                .limit(ChronoUnit.DAYS.between(start, end) + 1);
    }

    public List<LocalDate> toList() {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dates.add(d);
        }
        return dates;
    }
}
