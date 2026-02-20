package com.accrevent.radius.service;

import com.accrevent.radius.model.LinkedInOutreachCampaignMaturityRecordRegister;
import com.accrevent.radius.repository.ConstantLifecycleRepository;
import com.accrevent.radius.repository.LinkedInOutreachCampaignMaturityRecordRegisterRepository;
import com.accrevent.radius.util.LifecycleName;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LinkedInOutreachCampaignMaturityRecordRegisterService {

    private final LinkedInOutreachCampaignMaturityRecordRegisterRepository maturityRepo;
    private final ConstantLifecycleRepository constantLifecycleRepo;

    private static final Long LINKEDIN_OUTREACH_CYCLE_ID = 10L;

    public LinkedInOutreachCampaignMaturityRecordRegisterService(LinkedInOutreachCampaignMaturityRecordRegisterRepository maturityRepo, ConstantLifecycleRepository constantLifecycleRepo) {
        this.maturityRepo = maturityRepo;
        this.constantLifecycleRepo = constantLifecycleRepo;
    }


    // ---------------- Helper Conversions ----------------
    private long toEpochMillis(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private LocalDate toLocalDate(Long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    // ---------------- Update Method ----------------
    @Transactional
    public void updateMaturityReport(Long campaignId, String status) {
        LocalDate today = LocalDate.now();
        long todayMillis = toEpochMillis(today);

        Optional<LinkedInOutreachCampaignMaturityRecordRegister> recordOpt =
                maturityRepo.findByCampaignIdAndDateAndStatus(campaignId, todayMillis, status);

        LinkedInOutreachCampaignMaturityRecordRegister record = recordOpt.orElseGet(() -> {
            LinkedInOutreachCampaignMaturityRecordRegister r = new LinkedInOutreachCampaignMaturityRecordRegister();
            r.setCampaignId(campaignId);
            r.setDate(todayMillis);
            r.setStatus(status);
            r.setCount(0L);
            return r;
        });

        record.setCount(record.getCount() + 1);
        maturityRepo.save(record);
    }

    //  Report Method
    public Map<String, Object> getLinkedInMaturityReport(Long campaignId) {
        List<LinkedInOutreachCampaignMaturityRecordRegister> records =
                maturityRepo.findByCampaignIdOrderByDateAsc(campaignId);
        
        List<String> statuses = new ArrayList<>(
                constantLifecycleRepo
                        .findByCycleId(LINKEDIN_OUTREACH_CYCLE_ID)
                        .stream()
                        .map(c -> c.getCycleName())
                        .toList()
        );

        statuses.add(LifecycleName.ACCEPTED);



        // Group by LocalDate (converted from millis)
        Map<LocalDate, List<LinkedInOutreachCampaignMaturityRecordRegister>> grouped =
                records.stream().collect(Collectors.groupingBy(r -> toLocalDate(r.getDate())));

        List<Map<String, Object>> rows = new ArrayList<>();


        Map<String, Long> individualLifecycleCount = new LinkedHashMap<>();
        for (String status : statuses) {
            individualLifecycleCount.put(status, 0L);
        }

        LocalDate prevDate = null;

        for (Map.Entry<LocalDate, List<LinkedInOutreachCampaignMaturityRecordRegister>> entry :
                grouped.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .toList()) {

            Map<String, Object> row = new HashMap<>();
            row.put("date", toEpochMillis(entry.getKey())); // return date as millis

            Map<String, Long> statusCount = new LinkedHashMap<>();


            // preload all statuses with counts (0 if missing)
            for (String s : statuses) {
                long count = entry.getValue().stream()
                        .filter(r -> r.getStatus().equalsIgnoreCase(s))
                        .mapToLong(LinkedInOutreachCampaignMaturityRecordRegister::getCount)
                        .sum();
                statusCount.put(s, count);

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
