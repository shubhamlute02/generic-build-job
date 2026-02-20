package com.accrevent.radius.service;

import com.accrevent.radius.model.EmailOutreachCampaignMaturityRecordRegister;
import com.accrevent.radius.repository.ConstantLifecycleRepository;
import com.accrevent.radius.repository.EmailOutreachCampaignMaturityRecordRegisterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailOutreachCampaignMaturityRecordRegisterService {

    private final EmailOutreachCampaignMaturityRecordRegisterRepository maturityRepo;
    private final ConstantLifecycleRepository constantLifecycleRepo;

    private static final Long EMAIL_OUTREACH_CYCLE_ID = 5L;

    //Helper conversions
    private long toEpochMillis(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private LocalDate toLocalDate(Long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    // Update method
    @Transactional
    public void updateMaturityReport(Long campaignId, String status) {
        LocalDate today = LocalDate.now();
        long todayMillis = toEpochMillis(today);

        Optional<EmailOutreachCampaignMaturityRecordRegister> recordOpt =
                maturityRepo.findByCampaignIdAndDateAndStatus(campaignId, todayMillis, status);

        EmailOutreachCampaignMaturityRecordRegister record = recordOpt.orElseGet(() -> {
            EmailOutreachCampaignMaturityRecordRegister r = new EmailOutreachCampaignMaturityRecordRegister();
            r.setCampaignId(campaignId);
            r.setDate(todayMillis);
            r.setStatus(status);
            r.setCount(0L);
            return r;
        });

        record.setCount(record.getCount() + 1);
        maturityRepo.save(record);
    }

    //  Report method
    public Map<String, Object> getEmailOutreachMaturityReport(Long campaignId) {
        List<EmailOutreachCampaignMaturityRecordRegister> records =
                maturityRepo.findByCampaignIdOrderByDateAsc(campaignId);

        // fetch statuses dynamically
        List<String> statuses = constantLifecycleRepo
                .findByCycleId(EMAIL_OUTREACH_CYCLE_ID)
                .stream()
                .map(c -> c.getCycleName())
                .toList();

        // group by LocalDate (convert from millis)
        Map<LocalDate, List<EmailOutreachCampaignMaturityRecordRegister>> grouped =
                records.stream().collect(Collectors.groupingBy(r -> toLocalDate(r.getDate())));

        List<Map<String, Object>> rows = new ArrayList<>();


        Map<String, Long> individualLifecycleCount = new LinkedHashMap<>();
        for (String status : statuses) {
            individualLifecycleCount.put(status, 0L);
        }

        LocalDate prevDate = null;

        for (Map.Entry<LocalDate, List<EmailOutreachCampaignMaturityRecordRegister>> entry :
                grouped.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .toList()) {

            Map<String, Object> row = new HashMap<>();
            // send back millis to frontend
            row.put("date", toEpochMillis(entry.getKey()));

            Map<String, Long> statusCount = new LinkedHashMap<>();

            // preload all statuses with 0
            for (String s : statuses) {
                long count = entry.getValue().stream()
                        .filter(r -> r.getStatus().equalsIgnoreCase(s))
                        .mapToLong(EmailOutreachCampaignMaturityRecordRegister::getCount)
                        .sum();
                statusCount.put(s,count);

                //count of individual lifecycle throughout column.
                individualLifecycleCount.put(
                        s,
                        individualLifecycleCount.get(s) + count
                );
            }

            // calculate working day gap
            long gapCount = 0;
            if (prevDate != null) {
                gapCount = workingDayGap(prevDate, entry.getKey());
            }
            statusCount.put("gapCount", gapCount);

            row.put("statusCount", statusCount);
            rows.add(row);

            prevDate = entry.getKey();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("campaignId", campaignId);
        response.put("rows", rows);
        response.put("individualLifecycleCount", individualLifecycleCount);
        return response;
    }


    private long workingDayGap(LocalDate a, LocalDate b) {
        long gap = 0;
        LocalDate date = a;
        while (!date.isEqual(b)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow != DayOfWeek.FRIDAY &&dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                gap++;
            }
            date = date.plusDays(1);
        }
        return gap;
    }

}


