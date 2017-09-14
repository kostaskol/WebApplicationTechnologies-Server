package com.WAT.airbnb.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

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
