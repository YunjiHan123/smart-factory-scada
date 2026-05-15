package com.smartfactory.scada.energy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartfactory.scada.energy.domain.SummaryType;
import com.smartfactory.scada.energy.mapper.EnergyMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnergySummaryBatchService {

	private final EnergyMapper energyMapper;

	@Scheduled(cron = "${energy.summary.daily-cron:0 10 0 * * *}")
	public void createYesterdayDailySummaries() {
		createDailySummaries(LocalDate.now().minusDays(1));
	}

	@Scheduled(cron = "${energy.summary.monthly-cron:0 30 0 1 * *}")
	public void createPreviousMonthSummaries() {
		createMonthlySummaries(YearMonth.now().minusMonths(1));
	}

	@Transactional
	public void createDailySummaries(LocalDate date) {
		LocalDateTime from = date.atStartOfDay();
		LocalDateTime to = date.plusDays(1).atStartOfDay();

		energyMapper.deleteSummaries(SummaryType.DAILY, from, to);
		int facilitySummaryCount = energyMapper.insertDailyFacilitySummaries(from, to);
		int plantSummaryCount = energyMapper.insertDailyPlantSummaries(from, to);

		log.info(
			"Created daily energy summaries. date={}, facilitySummaryCount={}, plantSummaryCount={}",
			date,
			facilitySummaryCount,
			plantSummaryCount
		);
	}

	@Transactional
	public void createMonthlySummaries(YearMonth month) {
		LocalDateTime from = month.atDay(1).atStartOfDay();
		LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

		energyMapper.deleteSummaries(SummaryType.MONTHLY, from, to);
		int facilitySummaryCount = energyMapper.insertMonthlyFacilitySummaries(from, to);
		int plantSummaryCount = energyMapper.insertMonthlyPlantSummaries(from, to);

		log.info(
			"Created monthly energy summaries. month={}, facilitySummaryCount={}, plantSummaryCount={}",
			month,
			facilitySummaryCount,
			plantSummaryCount
		);
	}
}
